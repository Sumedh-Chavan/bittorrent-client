package bittorrentClient.pieceHandling;

import java.util.concurrent.atomic.AtomicInteger;

import static bittorrentClient.pieceHandling.pieceHandler.BLOCK_SIZE;

public class PieceState {
    final int index;
    final int length;
    final int blocksTotal;
    final byte[][] blocks;
    final AtomicInteger received = new AtomicInteger(0);

    public PieceState(int index, int length) {
        this.index = index;
        this.length = length;
        this.blocksTotal = (length + BLOCK_SIZE - 1) / BLOCK_SIZE;
        this.blocks = new byte[blocksTotal][];
    }

    public synchronized boolean putBlock(int blockIdx, byte[] data) {
        if (blocks[blockIdx] == null) {
            blocks[blockIdx] = data;
            received.incrementAndGet();
            return true;
        }
        return false;
    }

    public boolean isComplete() {
        return received.get() == blocksTotal;
    }

    // assemble into one byte[]
    public byte[] assemble() {
        int total = 0;
        for (int i = 0; i < blocksTotal; i++) total += blocks[i].length;
        byte[] all = new byte[total];
        int pos = 0;
        for (int i = 0; i < blocksTotal; i++) {
            System.arraycopy(blocks[i], 0, all, pos, blocks[i].length);
            pos += blocks[i].length;
        }
        return all;
    }

    public int blockBegin(int blockIdx) {
        return blockIdx * BLOCK_SIZE;
    }

    public int blockLength(int blockIdx) {
        int begin = blockBegin(blockIdx);
        return Math.min(BLOCK_SIZE, length - begin);
    }
}
