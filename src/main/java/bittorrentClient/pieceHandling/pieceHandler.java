package bittorrentClient.pieceHandling;

import bittorrentClient.pojo.Torrent;
import bittorrentClient.utils.myLogs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.locks.ReentrantLock;

import static bittorrentClient.bencoding.Utils.hex2ByteArray;

public class pieceHandler
{
    static byte [][] pieces;
    private long pieceSize;
    private int pieceBlockOffset;
    private int pieceIndex;
    private int totalPieces;
    private int totalBlocks;
    private int blockSize = 16384;
    private long fileSize;
    final ReentrantLock ioLock = new ReentrantLock();
    final FileChannel channel;
    private Torrent torrent;

    private final BitSet bitfield;
    private final BitSet peerbitfield;

    public pieceHandler(Torrent torrent) throws IOException {
        this.torrent = torrent;
        pieceSize = torrent.getPieceLength();

        totalPieces = (int) (torrent.getTotalSize() / torrent.getPieceLength());
        totalBlocks = (int) (torrent.getPieceLength() / blockSize);
        fileSize = torrent.getTotalSize();

        this.pieces = new byte[totalPieces][];
        for (int i = 0; i < totalPieces; i++) {
            long remainingBytes = fileSize - (i * pieceSize);
            int currentPieceSize = (int) Math.min(pieceSize, remainingBytes);
            pieces[i] = new byte[currentPieceSize];
        }

        pieceBlockOffset = 1;
        pieceIndex = 0;

        String fileName = torrent.getName() != null ? torrent.getName() : "output.data";
        Path outputDir = Paths.get("../output");
        Files.createDirectories(outputDir); // make sure directory exists
        Path outputFile = outputDir.resolve(fileName);

        channel = FileChannel.open(outputFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ);

        myLogs.info("total pieces: " + totalPieces);
        myLogs.info("total blocks: " + totalBlocks);

        // setting up bitfield
        this.bitfield = new BitSet(totalPieces);
        this.peerbitfield = new BitSet(totalPieces);
    }

    public ArrayList<Integer> oldhandleBlock(int index, int offset, byte[] payload)
    {
        System.arraycopy(payload, 0, pieces[index], offset, payload.length);
        myLogs.warn("Copied payload for index: " + index + " and offset: " + offset);

        ArrayList<Integer> res = new ArrayList<Integer>(3);
        pieceBlockOffset++;
        if(pieceBlockOffset <= totalBlocks)
        {
            res.add(pieceIndex);
            res.add(offset + 16384);
            res.add(blockSize);
        }
        else
        {
            // last block calculation
            int remaining = (int) (pieceSize - (long) blockSize * (pieceBlockOffset - 1));
            if(remaining > 0)
            {
                res.add(pieceIndex);
                res.add(offset + remaining);
                res.add(remaining);
            }
            else
            {
                pieceBlockOffset = 1;
                pieceIndex++;

                if(pieceIndex < totalPieces)
                {
                    res.add(pieceIndex);
                    res.add(0);
                    res.add(blockSize);
                }
                else {
                    res.add(-1);
                    res.add(-1);
                    res.add(-1);
                }
            }
        }

        return res;
    }

    private int actualPieceSize(int pieceIndex, long totalSize, long standardPieceSize) {
        long numPieces = (totalSize + standardPieceSize - 1L) / standardPieceSize; // ceil
        if (pieceIndex < numPieces - 1) {
            return (int) standardPieceSize;
        } else {
            long lastSize = totalSize - (numPieces - 1L) * standardPieceSize;
            return (int) lastSize;
        }
    }

    public ArrayList<Integer> handleBlock(int index, int offset, byte[] payload) {
        // copy incoming data
        System.arraycopy(payload, 0, pieces[index], offset, payload.length);
        myLogs.warn("Copied payload for index: " + index + " and offset: " + offset + " len: " + payload.length);

        ArrayList<Integer> res = new ArrayList<>(3);

        int pieceLen = actualPieceSize(index, fileSize, pieceSize);

        // compute next offset inside this piece
        int nextOffset = offset + payload.length;

        if (nextOffset < pieceLen) {
            int nextBlockSize = (int) Math.min(blockSize, (long) pieceLen - nextOffset);
            res.add(index);
            res.add(nextOffset);
            res.add(nextBlockSize);
        } else {
            // piece complete
            res.add(-1);
            res.add(index);

            if(verifyPieceHash(index))
            {
                bitfield.set(index);
                res.add(1);
            }
            else res.add(-1);
        }

        return res;
    }

    public synchronized void setBitfieldFromPayload(byte[] payload) {
        BitSet parsed = parseBitfieldToBitSet(payload, totalPieces);
        peerbitfield.clear();
        peerbitfield.or(parsed);

        myLogs.info("The received bitfield is --- ");
        myLogs.info("Size: " + peerbitfield.length());
        myLogs.info("Content: " + peerbitfield);
    }

    public static BitSet parseBitfieldToBitSet(byte[] payload, int numPieces) {
        BitSet bitset = new BitSet(numPieces);
        int expectedLen = (numPieces + 7) / 8;
        // It's okay if payload length != expectedLen; we'll use min length
        int useLen = Math.min(payload.length, expectedLen);

        for (int byteIndex = 0; byteIndex < useLen; byteIndex++) {
            int b = payload[byteIndex] & 0xFF; // unsigned
            // within byte, MSB is piece (byteIndex*8)
            for (int bitInByte = 0; bitInByte < 8; bitInByte++) {
                int pieceIndex = byteIndex * 8 + bitInByte;
                if (pieceIndex >= numPieces) break; // ignore padding bits
                int mask = 1 << (7 - bitInByte);
                if ((b & mask) != 0) {
                    bitset.set(pieceIndex);
                }
            }
        }
        return bitset;
    }

    public int pickNextPieceIndex(int index) {
        BitSet candidate = (BitSet) peerbitfield.clone();
        candidate.andNot(bitfield);
        return candidate.isEmpty() ? -1 : candidate.nextSetBit(index);
    }

    public synchronized void markPieceAsComplete(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= totalPieces) throw new IllegalArgumentException();
        bitfield.set(pieceIndex);
        // persist resume info to disk as needed
//        saveToDisk(pieceIndex);
    }



    private boolean verifyPieceHash(int index) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(pieces[index]);
            String expectedHashHex = torrent.getPieces().get(index);
            byte[] expectedHash = hex2ByteArray(expectedHashHex);
            return MessageDigest.isEqual(hash, expectedHash);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    public void writePiece(int idx, byte[] data) throws IOException {
        long pos = (long) idx * pieceSize;
        ByteBuffer buf = ByteBuffer.wrap(data);
        ioLock.lock();
        try {
            channel.write(buf, pos);
        } finally {
            ioLock.unlock();
        }

        myLogs.info("Successfully wrote at index: " + idx);
    }

    public void downloadPieces() throws IOException {

        myLogs.info("Inside downloadPieces");
        for(int i = 0; i < pieces.length; i++)
        {
            // todo: afterwards need to handle the logic wherein some piece hash will not match.
            boolean chk = verifyPieceHash(i);

            if(chk)
            {
                writePiece(i, pieces[i]);
            }
        }
    }
}
