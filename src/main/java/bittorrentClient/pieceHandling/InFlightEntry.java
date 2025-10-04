package bittorrentClient.pieceHandling;

public class InFlightEntry {
    String peerId;
    long timestamp;

    void InflightEntry(String peerId) {
        this.peerId = peerId;
        this.timestamp = System.currentTimeMillis();
    }
}
