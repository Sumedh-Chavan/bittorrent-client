package bittorrentClient;

import bittorrentClient.pojo.Torrent;
import bittorrentClient.pojo.TrackerResponse;
import bittorrentClient.torrent.TorrentParser;
import bittorrentClient.tracker.Tracker;
import bittorrentClient.tracker.TrackerResponseParser;
import bittorrentClient.utils.Utils;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Application {

    public static void main(String[] args) {
        Application application = new Application();
        application.start();
    }

    public void start() {
        try {
            Torrent torrent = TorrentParser.parseTorrent("/home/ritesh/Downloads/sample.torrent");
            System.out.println(torrent);
            Tracker tracker = new Tracker(torrent);
            TrackerResponseParser trackerResponseParser = new TrackerResponseParser();
            TrackerResponse trackerResponse = trackerResponseParser.parseTrackerResponse(tracker.sendTrackerRequest());
            byte[] info_hashBytes = Utils.hexStringToBytes(torrent.getInfo_hash());
            byte[] peerId = Utils.CLIENT_ID.getBytes(StandardCharsets.ISO_8859_1); // 20 bytes
            List<Socket> socketList = trackerResponse.getPeers()
                    .stream()
                    .map(peer -> peer.sendPeerHandshake(info_hashBytes, peerId))
                    .collect(Collectors.toList());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
