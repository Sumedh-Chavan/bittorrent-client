package bittorrentClient;

import bittorrentClient.peer.Peer;
import bittorrentClient.peer.PeerHandler;
import bittorrentClient.pieceHandling.DownloadManager;
import bittorrentClient.pojo.Torrent;
import bittorrentClient.pojo.TrackerResponse;
import bittorrentClient.torrent.TorrentParser;
import bittorrentClient.tracker.Tracker;
import bittorrentClient.tracker.TrackerResponseParser;
import bittorrentClient.utils.Utils;
import bittorrentClient.utils.myLogs;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Application {

    public static void main(String[] args) {
        Application application = new Application();
        application.start();
    }

    public void start() {
        try {
            myLogs.info("Starting Application...");

            Torrent torrent = TorrentParser.parseTorrent("../torrents/sample.torrent");
//            System.out.println(torrent);

            //torrent info
            myLogs.info("*** Torrent info ***");
            myLogs.info("annouce: " + torrent.getAnnounce());
            myLogs.info("name: " + torrent.getName());
            myLogs.info("pieceLength: " + torrent.getPieceLength());
            myLogs.info("totalSize: " + torrent.getTotalSize());
            myLogs.info("*** Torrent info ***");

            DownloadManager manager = new DownloadManager(torrent.getTotalPieces(), torrent.getPieceLength(), torrent.getLastPieceLength(), torrent.getPiecesHashesinBytes(), torrent.getName());

            Tracker tracker = new Tracker(torrent);
            TrackerResponseParser trackerResponseParser = new TrackerResponseParser();
            TrackerResponse trackerResponse = trackerResponseParser.parseTrackerResponse(tracker.sendTrackerRequest());
            byte[] info_hashBytes = Utils.hexStringToBytes(torrent.getInfo_hash());
            byte[] peerId = Utils.CLIENT_ID.getBytes(StandardCharsets.ISO_8859_1); // 20 bytes
            List<Peer> peers = trackerResponse.getPeers();
            for(Peer peer : peers) {
                Socket socket = peer.sendPeerHandshake(info_hashBytes, peerId);
                if(socket == null) continue;
                peer.setPeerSocket(socket);
                manager.addPeer(peer);
                manager.ioPool.execute(Objects.requireNonNull(PeerHandler.parsePeerMessage(socket)));
            }


            PeerHandler handlePeer = new PeerHandler(trackerResponse.getPeers(),  info_hashBytes, peerId, torrent);

//            if(!socketList.isEmpty())
//            {
//                handlePeer.parsePeerMessage(socketList.get(1));
//            }
//            else{
//                System.out.println("socketList size: " + socketList.size());
//            }


            myLogs.info("Ending application...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
