package bittorrentClient.pieceHandling;

import bittorrentClient.peer.Peer;
import bittorrentClient.peer.PeerHandler;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DownloadManager {
    public static final int BLOCK_SIZE = 16 * 1024;

    private final int pieceCount;
    private final int pieceLength;
    private final int lastPieceLength;
    private final byte[][] pieceHashes;
    private final PieceState[] pieces;
    private final boolean[] have;
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    public final ExecutorService ioPool = Executors.newCachedThreadPool();
    private final RandomAccessFile file;

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
        this.file = new RandomAccessFile(filename, "rw");
        long totalSize = (long)(pieceCount - 1) * pieceLength + lastPieceLength;
        this.file.setLength(totalSize);
    }

    public void addPeer(Peer p) {
        peers.put(p.getPeerId(), p);
    }
}
