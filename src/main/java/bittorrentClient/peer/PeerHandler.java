package bittorrentClient.peer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
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

    public PeerHandler(List<Peer> peers, byte[] infoHash, byte[] peerIdBytes) {
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
        if(peer.sendPeerHandshake(infoHash, peerIdBytes))
            activePeers.add(peer);
    }
}
