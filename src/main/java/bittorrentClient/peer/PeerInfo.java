package bittorrentClient.peer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class PeerInfo {
    private String peerId;
    private String ip;
    private int port;
//
//    public boolean sendPeerHandshake(String info_hash, String clientPeerId) throws Exception
//    {
//        try {
//            Socket socket = new Socket();
//            socket.connect(new InetSocketAddress(ip, port), 5000);
//            OutputStream out = socket.getOutputStream();
//            InputStream in = socket.getInputStream();
//
//            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
//            handshake.write(19);
//            handshake.write("BitTorrent protocol".getBytes());
//
//            handshake.write(new byte[8]);
//
//            handshake.write(info_hash.getBytes()); // 20 bytes SHA1 of info dict
//            handshake.write(clientPeerId.getBytes());   // 20 bytes unique client ID
//
//            out.write(handshake.toByteArray());
//            out.flush();
//
//            byte[] responseBytes = in.readAllBytes();
//
//            int pstrlen = responseBytes[0] & 0xFF; // convert unsigned byte
//            String pstr = new String(responseBytes, 1, pstrlen);
//            byte[] reserved = Arrays.copyOfRange(responseBytes, 1 + pstrlen, 1 + pstrlen + 8);
//            byte[] infoHash = Arrays.copyOfRange(responseBytes, 1 + pstrlen + 8, 1 + pstrlen + 8 + 20);
//            byte[] peerId = Arrays.copyOfRange(responseBytes, 1 + pstrlen + 8 + 20, 68);
//
//            System.out.println("pstrlen   = " + pstrlen);
//            System.out.println("pstr      = " + pstr);
//            System.out.println("reserved  = " + bytesToHex(reserved));
//            System.out.println("info_hash = " + bytesToHex(infoHash));
//            System.out.println("peer_id   = " + new String(peerId, "ISO-8859-1")); // use ISO-8859-1 for raw byte mapping
//        }
//        catch (Exception e) {
//            return false;
//        }
//        return true;
//    }

    public boolean sendPeerHandshake(byte[] infoHashBytes, byte[] clientPeerIdBytes, String ip, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 3000); // 3s connect timeout
            socket.setSoTimeout(3000); // 3s read timeout

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            handshake.write(19);
            handshake.write("BitTorrent protocol".getBytes("ISO-8859-1"));
            handshake.write(new byte[8]); // reserved
            handshake.write(infoHashBytes); // 20 raw bytes
            handshake.write(clientPeerIdBytes); // 20 raw bytes

            out.write(handshake.toByteArray());
            out.flush();

            // Read exactly 68 bytes of handshake
            byte[] responseBytes = in.readNBytes(68);

            int pstrlen = responseBytes[0] & 0xFF;
            String pstr = new String(responseBytes, 1, pstrlen, "ISO-8859-1");
            byte[] reserved = Arrays.copyOfRange(responseBytes, 1 + pstrlen, 1 + pstrlen + 8);
            byte[] infoHash = Arrays.copyOfRange(responseBytes, 1 + pstrlen + 8, 1 + pstrlen + 8 + 20);
            byte[] peerId = Arrays.copyOfRange(responseBytes, 1 + pstrlen + 8 + 20, 68);

            System.out.println("pstrlen   = " + pstrlen);
            System.out.println("pstr      = " + pstr);
            System.out.println("reserved  = " + bytesToHex(reserved));
            System.out.println("info_hash = " + bytesToHex(infoHash));
            System.out.println("peer_id   = " + new String(peerId, "ISO-8859-1"));

            socket.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    //////////////////BOILER PLATE CODE BELOW////////////

    public PeerInfo() {}

    public PeerInfo(String peerId, String ip, int port) {
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
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
