package bittorrentClient.pieceHandling;

import bittorrentClient.peer.Peer;
import bittorrentClient.peer.PeerHandler;
import bittorrentClient.utils.myLogs;

import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadManager {
    public static final int BLOCK_SIZE = 16 * 1024;
    public static final long REQUEST_TIMEOUT_MS = 30_000L;
    public static final long DISPATCH_INTERVAL_MS = 50L;
    public static final int DEFAULT_PIPELINE = 6;

    private final int pieceCount;
    private final int pieceLength;
    private final int lastPieceLength;
    private final byte[][] pieceHashes;
    private final PieceState[] pieces;
    private final boolean[] have;
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    public final ExecutorService ioPool = Executors.newCachedThreadPool();
    private final RandomAccessFile file;
    private final Object nextPieceLock = new Object();
    private int nextPiecePointer = 0;
    private final InflightManager inflight = new InflightManager();
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    public boolean isStopping() { return stopping.get(); }

    public DownloadManager(int pieceCount, int pieceLength, int lastPieceLength, byte[][] pieceHashes,
                           String filename) throws IOException {
        this.pieceCount = pieceCount;
        this.pieceLength = pieceLength;
        this.lastPieceLength = lastPieceLength;
        this.pieceHashes = pieceHashes;
        this.pieces = new PieceState[pieceCount];
        this.have = new boolean[pieceCount];
        for (int i = 0; i < pieceCount; i++) {
            int len = (i == pieceCount - 1) ? lastPieceLength : pieceLength;
            pieces[i] = new PieceState(i, len);
        }
        // open file and preallocate size (optional but recommended)
        Path outputDir = Paths.get("../output");
        Files.createDirectories(outputDir); // make sure directory exists
        Path outputFile = outputDir.resolve(filename);
        this.file = new RandomAccessFile(outputFile.toFile(), "rw");
        long totalSize = (long)(pieceCount - 1) * pieceLength + lastPieceLength;
        this.file.setLength(totalSize);
    }

    public void addPeer(Peer p) {
        peers.put(p.getPeerId(), p);
    }

    public void removePeer(String peerId) {
        peers.remove(peerId);
        inflight.removePeerEntries(peerId);
    }

    public void start() {
        myLogs.warn("Schedulers started...");
        scheduler.scheduleAtFixedRate(this::dispatchPass, 0, DISPATCH_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::reassignTimedOut, 1, 1, TimeUnit.SECONDS);
    }

    // dispatcher: try to fill each peer pipeline
    private void dispatchPass() {
        myLogs.info("Dispatch called...");
        for (Peer peer : peers.values()) {
            if (peer.choked) continue; // respect choke state if known
            while (peer.inflight.size() < peer.pipelineLimit) {
                // find next block for this peer (naive sequential)
                BlockSelection sel = findNextBlockForPeer(peer);
                if (sel == null)
                {
                    myLogs.warn("findNextBlockForPeer returned null");
                    break;
                }

                boolean reserved = inflight.reserve(sel.index, sel.begin, peer.getPeerId());
                if (!reserved) {
                    myLogs.warn("someone else reserved it concurrently; try again");
                    continue;
                }
                peer.inflight.add(new BlockKey(sel.index, sel.begin));

                peer.sendRequestMessage(sel.index, sel.begin, sel.length);
            }
        }
    }

    // reassign timed-out blocks so dispatcher can re-request them
    private void reassignTimedOut() {
        List<BlockKey> removed = inflight.removeTimedOut(REQUEST_TIMEOUT_MS);
        if (!removed.isEmpty()) {
            for (BlockKey k : removed) {
                // also remove from any peer.inflight sets
                for (Peer p : peers.values()) p.inflight.remove(k);
                System.out.println("Reassigned timed-out " + k);
            }
        }
    }

    private BlockSelection findNextBlockForPeer(Peer peer) {
        myLogs.info("---> findNextBlockForPeer for peer: " + peer.getPeerId());
        int n = pieceCount;
        for (int offset = 0; offset < n; offset++) {

            int idx;
            synchronized (nextPieceLock) {
                idx = (nextPiecePointer + offset) % n;
            }

            if (have[idx] || peer.bitfield == null || idx >= peer.bitfield.length || !peer.bitfield[idx])
            {
                myLogs.warn(String.format("One of following is true: [%b, %b, %b, %b]",
                        have[idx],
                        peer.bitfield == null,
                        idx >= peer.bitfield.length,
                        !peer.bitfield[idx])
                );
                continue;
            }

            PieceState piece = pieces[idx];
            if (piece == null) {
                myLogs.warn("piece is null for idx=" + idx);
                continue;
            }

            myLogs.info(String.format("Inspecting piece %d: blocksTotal=%d", idx, piece.blocksTotal));
            for (int b = 0; b < piece.blocksTotal; b++) {
                if (piece.blocks[b] == null)
                {
                    int begin = piece.blockBegin(b);
                    int len = piece.blockLength(b);

//                    if (inflight.owner(idx, begin) != null)
//                    {
//                        myLogs.warn(String.format("[%d, %d] already reserved!", idx, begin));
//                        continue;
//                    }

                    InflightManager.InflightEntry owner = inflight.owner(idx, begin);
                    myLogs.info(String.format("inflight.owner(%d,%d) => %s", idx, begin, owner));

                    if (owner != null) {
                        myLogs.warn(String.format("[%d, %d] already reserved by %s!", idx, begin, owner));
                        continue;
                    }

                    // increment the nextpeicepointer only when its at the last block of this piece
                    if(b == piece.blocksTotal - 1){
                        synchronized (nextPieceLock) {
                            nextPiecePointer = (idx + 1) % n;
                        }
                    }

                    return new BlockSelection(idx, begin, len);
                }
                else
                {
                    myLogs.info(String.format("Block no. %d already present for piece %d", b, idx));
                }
            }
        }
        return null;
    }

    // simple sequential selection: scan from nextPiecePointer, wrap around
    private static final class BlockSelection {
        final int index;
        final int begin;
        final int length;
        BlockSelection(int index, int begin, int length) { this.index=index; this.begin=begin; this.length=length; }
    }

    public void parsePeerMessage(String peerid) throws Exception {

        Peer peer = peers.get(peerid);
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(peer.socket.getInputStream())))
        {
            while(true)
            {
                try {
                    int prefixLength = in.readInt();
                    if (prefixLength == 0) {
                        continue; // Keep-alive message, no payload
                    }

                    byte id = in.readByte();
                    int payloadLength = prefixLength - 1;
                    byte[] payload = new byte[payloadLength];
                    in.readFully(payload);


                    if(id == 5)     // bitfield message
                    {
                        peer.bitfield = parseBitfield(payload);

                        myLogs.info("Sending interested message to " + peerid);
                        byte[] tempPayload = new byte[0]; // length = 0
                        byte tempId = 2;
                        peer.sendPeerMessage(tempId, tempPayload);
                    }
                    if(id == 1)     // unchoke message
                    {
                        myLogs.info("Unchoke received from peer " + peerid);
                        peer.choked = false;

//                        // for first request message we default with index 0
//                        int nextIndex = pieceHandler.pickNextPieceIndex(0);
//                        if(nextIndex != -1)
//                        {
//                            // todo: maybe this can break if the first request is < 16348
//                            sendRequestMessage(peer.socket, nextIndex, 0, 16384);
//                        }
//                        else {
//                            System.out.println("ok done bye");
//                            break;
//                        }

                    }
                    if(id == 7)     // piece message
                    {
                        DataInputStream payloadStream = new DataInputStream(new ByteArrayInputStream(payload));

                        int index = payloadStream.readInt();
                        int offset = payloadStream.readInt();

                        byte[] payloadBytes = new byte[payloadLength - 8];
                        payloadStream.readFully(payloadBytes);

                        handleIncomingBlock(peer.getPeerId(), index, offset, payloadBytes);
                    }

                }
                catch (SocketException | EOFException e) {
                    if (!isStopping()) throw e; // rethrow only if unexpected
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // todo: why unreachable?
//        myLogs.info("<--- parsePeerMessage()");
    }

    public boolean[] parseBitfield(byte[] payload) {
        boolean[] bitset = new boolean[pieceCount];
        int expectedLen = (pieceCount + 7) / 8;
        int useLen = Math.min(payload.length, expectedLen);

        for (int byteIndex = 0; byteIndex < useLen; byteIndex++) {
            int b = payload[byteIndex] & 0xFF; // unsigned
            // within byte, MSB is piece (byteIndex*8)
            for (int bitInByte = 0; bitInByte < 8; bitInByte++) {
                int pieceIndex = byteIndex * 8 + bitInByte;
                if (pieceIndex >= pieceCount) break; // ignore padding bits
                int mask = 1 << (7 - bitInByte);
                if ((b & mask) != 0) {
                    bitset[pieceIndex] = true;
                }
            }
        }

        myLogs.info("The received bitfield is --- ");
        myLogs.info("Size: " + bitset.length);
        myLogs.info("Content: " + Arrays.toString(bitset));

        return bitset;
    }

    public void handleIncomingBlock(String peerId, int index, int begin, byte[] data) {
        // remove peer inflight record (best-effort)
        Peer p = peers.get(peerId);
        if (p != null) p.inflight.remove(new BlockKey(index, begin));

        int blockIdx = begin / BLOCK_SIZE;
        PieceState piece = pieces[index];
        boolean added = piece.putBlock(blockIdx, data);
        inflight.release(index, begin);

        if (!added) return; // duplicate block
        myLogs.warn("Piece message received from: " + peerId + " for index: " + index + " with offset: " + begin);
        if (piece.isComplete()) {
            ioPool.execute(() -> {
                try {
                    byte[] all = piece.assemble();
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    byte[] actual = md.digest(all);
                    if (Arrays.equals(actual, pieceHashes[index])) {
                        // write to file
                        try {
                            long offset = (long) index * pieceLength;
                            synchronized (file) {
                                file.seek(offset);
                                file.write(all);
                            }
                            have[index] = true;
                            // broadcast have to peers so they update availability (implement buildHaveMessage)
                            p.sendHaveMessage(index);
                            System.out.println("Piece " + index + " verified and written.");
                            myLogs.info("Piece " + index + " verified and written.");

                            if (isDownloadComplete()) {
                                System.out.println("***** All pieces downloaded! Stopping schedulers... *****");
                                myLogs.info(" ***** All pieces downloaded! ****** ");
                                stop();
                            }

                        } catch (IOException ioe) {
                            System.err.println("Failed writing piece " + index + ": " + ioe);
                            myLogs.error("Failed writing piece " + index + ": " + ioe);
                            // reset piece to re-download on failure
                            pieces[index] = new PieceState(index, piece.length);
                        }
                    } else {
                        System.err.println("Piece " + index + " failed verification. Requeueing.");
                        pieces[index] = new PieceState(index, piece.length);
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public void stop() {
        if (!stopping.compareAndSet(false, true)) return; // idempotent

        System.out.println("Stopping download manager...");
        scheduler.shutdownNow();

        for (Peer p : peers.values()) {
            try { p.socket.shutdownInput(); } catch (Exception ignored) {}
            try { p.socket.shutdownOutput(); } catch (Exception ignored) {}
        }

        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        for (Peer p : peers.values()) {
            try { p.socket.close(); } catch (Exception ignored) {}
        }

        ioPool.shutdownNow();

        try { file.close(); } catch (Exception ignored) {}

        System.out.println("ok done bye!");
    }

    private boolean isDownloadComplete() {
        for (boolean h : have) {
            if (!h) return false;
        }
        return true;
    }

}
