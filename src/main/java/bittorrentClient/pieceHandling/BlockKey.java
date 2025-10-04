package bittorrentClient.pieceHandling;

public class BlockKey {
    public final int pieceIndex;
    public final int begin;

    public BlockKey(int pieceIndex, int begin) {
        this.pieceIndex = pieceIndex;
        this.begin = begin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockKey)) return false;
        BlockKey other = (BlockKey) o;
        return this.pieceIndex == other.pieceIndex && this.begin == other.begin;
    }

    @Override
    public int hashCode() {
        return 31 * pieceIndex + begin;
    }

    @Override
    public String toString() {
        return "BlockKey[" + pieceIndex + "," + begin + "]";
    }
}
