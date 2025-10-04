package bittorrentClient.peer;

import bittorrentClient.pieceHandling.pieceHandler;
import bittorrentClient.pojo.Torrent;
import bittorrentClient.utils.myLogs;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PeerHandler {

    private List<Peer> peers;
    private List<Peer> activePeers;
    private byte[] infoHash;
    private byte[] peerIdBytes;
    public static final int DEFAULT_PIPELINE = 6;

    // todo: just put here temp. find a better way out
    private static Torrent torrent;

    public PeerHandler(List<Peer> peers, byte[] infoHash, byte[] peerIdBytes, Torrent torrent) {
        this.torrent = torrent;
        this.peers = peers;
        this.activePeers = Collections.synchronizedList(new ArrayList<>());
        this.infoHash = infoHash;
        this.peerIdBytes = peerIdBytes;
    }

    public void setActivePeers() {
        ExecutorService executor = Executors.newFixedThreadPool(peers.size());
        for(Peer peer : peers)
            executor.submit(()-> addActivePeer(peer));
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {

        }
    }

    public void addActivePeer(Peer peer) {
        if(peer.sendPeerHandshake(infoHash, peerIdBytes) != null)
            activePeers.add(peer);
    }

    // todo: maybe add socket datafield inside the peer class itself?



    public static void sendPeerMessage(Socket socket, byte id, byte[] payload)
    {
        try {
            DataOutputStream out = new DataOutputStream((new BufferedOutputStream(socket.getOutputStream())));

            int length = 1 + payload.length;
            out.writeInt(length);
            out.writeByte(id);
            out.write(payload);
            out.flush();

            myLogs.warn("Peer message sent with id: " + id);
        } catch (IOException e) {
            myLogs.error("Some error with IO for message id: " + id);
            throw new RuntimeException(e);
        }
    }

    public static void sendRequestMessage(Socket socket, int index, int begin, int length)
    {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.order(ByteOrder.BIG_ENDIAN); // ensure network order

        buffer.putInt(index);
        buffer.putInt(begin);
        buffer.putInt(length);

        byte[] result = buffer.array();
        sendPeerMessage(socket, (byte) 6, result);
    }
}
