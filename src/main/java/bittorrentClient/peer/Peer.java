package bittorrentClient.peer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Peer {

    private PeerInfo peerInfo;

    public void sendPeerHandshake(PeerInfo peerInfo, String info_hash, String peerId) throws Exception
    {
        Socket socket = new Socket(peerInfo.getIp(), Integer.parseInt(Long.toString(peerInfo.getPort())));
        OutputStream out = socket.getOutputStream();

        ByteArrayOutputStream handshake = new ByteArrayOutputStream();
        handshake.write(19);  // pstrlen
        handshake.write("BitTorrent protocol".getBytes()); // pstr

        handshake.write(new byte[8]); // reserved bytes

        handshake.write(info_hash.getBytes()); // 20 bytes SHA1 of info dict
        handshake.write(peerId.getBytes());   // 20 bytes unique client ID

        out.write(handshake.toByteArray());
        out.flush();

    }
}
