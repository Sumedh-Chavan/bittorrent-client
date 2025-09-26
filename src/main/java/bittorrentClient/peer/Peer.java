package bittorrentClient.peer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Peer {

    public static final int DEFAULT_PIPELINE = 6;

    private String peerId;
    private String ip;
    private int port;
    public Socket socket = null;
    public volatile boolean[] bitfield;
    public volatile boolean choked = false;
    public int pipelineLimit = DEFAULT_PIPELINE;

    public Socket sendPeerHandshake(byte[] infoHashBytes, byte[] peerIdBytes) {
        Socket socket = null;
        try {
            socket = new Socket();
            // 1. Connect
            socket.connect(new InetSocketAddress(ip, port), 3000); // 3s connect timeout
            socket.setSoTimeout(10000); // 10s read timeout for handshake

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 2. Build handshake
            ByteArrayOutputStream handshake = new ByteArrayOutputStream();

            // pstrlen
            handshake.write(19);

            // pstr
            handshake.write("BitTorrent protocol".getBytes(StandardCharsets.ISO_8859_1));

            // reserved bytes (DHT + Fast extension bits set)
//            byte[] reserved = {0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x05};
            byte[] reserved = new byte[8];
            handshake.write(reserved);

            // info_hash (20 raw bytes)
            handshake.write(infoHashBytes);

            // peer_id (20 raw bytes)
            handshake.write(peerIdBytes);

            // 3. Send handshake
            out.write(handshake.toByteArray());
            out.flush();

            // 4. Read handshake response (exactly 68 bytes)
            byte[] responseBytes = new byte[68];
            int totalRead = 0;
            while (totalRead < 68) {
                int bytesRead = in.read(responseBytes, totalRead, 68 - totalRead);
                if (bytesRead == -1) {
                    throw new IOException("Peer closed connection before handshake completed");
                }
                totalRead += bytesRead;
            }

            // 5. Parse handshake response
            int pstrlen = responseBytes[0] & 0xFF;
            String pstr = new String(responseBytes, 1, pstrlen, StandardCharsets.ISO_8859_1);
            byte[] reservedResp = Arrays.copyOfRange(responseBytes, 1 + pstrlen, 1 + pstrlen + 8);
            byte[] infoHashResp = Arrays.copyOfRange(responseBytes, 1 + pstrlen + 8, 1 + pstrlen + 8 + 20);
            byte[] peerIdResp = Arrays.copyOfRange(responseBytes, 1 + pstrlen + 8 + 20, 68);

            // 6. Debug prints
            System.out.println("pstrlen   = " + pstrlen);
            System.out.println("pstr      = " + pstr);
            System.out.println("reserved  = " + bytesToHex(reservedResp));
            System.out.println("info_hash = " + bytesToHex(infoHashResp));
            System.out.println("peer_id   = " + new String(peerIdResp, StandardCharsets.ISO_8859_1));

            // 7. Validate info_hash matches
            if (!Arrays.equals(infoHashBytes, infoHashResp)) {
                throw new IOException("Mismatched info_hash from peer");
            }

            return socket;

        } catch (Exception e) {
            try {
                socket.close();
            }
            catch (IOException ex) {

            }
            System.err.println("Handshake failed: " + e.getMessage());
            return null;
        }
    }


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public Peer() {
    }

    public Peer(String peerId, String ip, int port) {
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

    public void setPeerSocket(Socket socket) {
        this.socket = socket;
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
