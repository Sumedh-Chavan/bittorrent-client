package bittorrentClient.pieceHandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InflightManager {

    // inflight reservation record
    static final class InflightEntry {
        final String peerId;
        final long timestamp;
        InflightEntry(String peerId) {
            this.peerId = peerId;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final ConcurrentHashMap<BlockKey, InflightEntry> map = new ConcurrentHashMap<>();

    // reserve returns true if we successfully reserved; false if already reserved
    public boolean reserve(int pieceIndex, int begin, String peerId) {
        BlockKey key = new BlockKey(pieceIndex, begin);
        InflightEntry e = new InflightEntry(peerId);
        return map.putIfAbsent(key, e) == null;
    }

    public void release(int pieceIndex, int begin) {
        map.remove(new BlockKey(pieceIndex, begin));
    }

    public InflightEntry owner(int pieceIndex, int begin) {
        return map.get(new BlockKey(pieceIndex, begin));
    }

    // remove entries older than timeout and return the removed keys
    public List<BlockKey> removeTimedOut(long timeoutMs) {
        long now = System.currentTimeMillis();
        List<BlockKey> removed = new ArrayList<>();
        for (Map.Entry<BlockKey, InflightEntry> en : map.entrySet()) {
            if (now - en.getValue().timestamp > timeoutMs) {
                if (map.remove(en.getKey(), en.getValue())) {
                    removed.add(en.getKey());
                }
            }
        }
        return removed;
    }

    // remove all entries owned by a peer (on disconnect)
    public void removePeerEntries(String peerId) {
        for (Map.Entry<BlockKey, InflightEntry> en : map.entrySet()) {
            if (en.getValue().peerId.equals(peerId)) {
                map.remove(en.getKey(), en.getValue());
            }
        }
    }
}
