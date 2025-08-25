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

    // todo: just put here temp. find a better way out
    private Torrent torrent;

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

    public void parsePeerMessage(Socket connection) throws IOException {

        myLogs.info("---> parsePeerMessage()");

        pieceHandler pieceHandler = new pieceHandler(torrent);
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(connection.getInputStream()))) {
            while(true)
            {
                try {
                    int prefixLength = in.readInt();
                    if (prefixLength == 0) {
                        continue; // Keep-alive message, no payload
                    }

                    byte id = in.readByte();
                    int payloadLength = prefixLength - 1;
                    byte[] payload = new byte[payloadLength];
                    in.readFully(payload);


                    if(id == 5)     // bitfield message
                    {
                        processBitfieldMessage(connection);
                    }
                    if(id == 1)     // unchoke message
                    {

                        // directly sending request for piece 0 and offset 0 for now
                        ByteBuffer buffer = ByteBuffer.allocate(12);
                        buffer.order(ByteOrder.BIG_ENDIAN); // ensure network order

                        buffer.putInt(0);
                        buffer.putInt(0);
                        buffer.putInt(16384);

                        myLogs.info("Sending request message for first piece and first block...");
                        byte[] result = buffer.array();
                        sendPeerMessage(connection, (byte) 6, result);
                    }
                    if(id == 7)     // piece message
                    {
                        DataInputStream payloadStream = new DataInputStream(new ByteArrayInputStream(payload));

                        int index = payloadStream.readInt();
                        int offset = payloadStream.readInt();

                        byte[] payloadBytes = new byte[payloadLength - 8];
                        payloadStream.readFully(payloadBytes);

                        ArrayList<Integer> indices = pieceHandler.handleBlock(index, offset, payloadBytes);

                        if(indices.get(0) != -1)
                        {
                            myLogs.info("Sending request message for index: " + indices.get(0) + " and offset: " + indices.get(1) + " for length: " + indices.get(2));
                            sendRequestMessage(connection, indices.get(0), indices.get(1), indices.get(2));
                        }
                        else
                        {

                            pieceHandler.downloadPieces();

                            System.out.println("ok done bye");
                            break;
                        }
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }

        myLogs.info("<--- parsePeerMessage()");
    }

    public void processBitfieldMessage(Socket socket)
    {
        // not doing anything for now but directly sending interested message.
        // once confirm what is being sending wrt to this bitfield message and then decide the process of bitfield.


        myLogs.info("Sending interested message...");
        byte[] payload = new byte[0]; // length = 0
        byte id = 2;
        sendPeerMessage(socket, id, payload);

    }

    public void sendPeerMessage(Socket socket, byte id, byte[] payload)
    {
        try {
            DataOutputStream out = new DataOutputStream((new BufferedOutputStream(socket.getOutputStream())));

            int length = 1 + payload.length;
            out.writeInt(length);
            out.writeByte(id);
            out.write(payload);
            out.flush();
        } catch (IOException e) {
            myLogs.error("Some error with IO for message id: " + id);
            throw new RuntimeException(e);
        }
    }

    public void sendRequestMessage(Socket socket, int index, int begin, int length)
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
