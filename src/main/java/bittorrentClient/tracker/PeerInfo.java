package bittorrentClient.tracker;

public class PeerInfo {
    private String peerId;
    private String ip;
    private long port;

    public PeerInfo() {}

    public PeerInfo(String peerId, String ip, long port) {
        this.peerId = peerId;
        this.ip = ip;
        this.port = port;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getPort() {
        return port;
    }

    public void setPort(long port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "peerId='" + peerId + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
