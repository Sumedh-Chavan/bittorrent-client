package bittorrentClient.peer;

import bittorrentClient.pojo.Torrent;
import bittorrentClient.torrent.TorrentParser;
import bittorrentClient.tracker.Tracker;
import bittorrentClient.pojo.TrackerResponse;
import bittorrentClient.tracker.TrackerResponseParser;
import bittorrentClient.utils.Utils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class PeerHandlerInfoTest {

    @Test
    public void testPeerInfo() throws Exception
    {
        Torrent torrent = TorrentParser.parseTorrent("../torrents/sample.torrent");
        Tracker tracker = new Tracker(torrent);
        TrackerResponseParser trackerResponseParser = new TrackerResponseParser();
        TrackerResponse trackerResponse = trackerResponseParser.parseTrackerResponse(tracker.sendTrackerRequest());
        System.out.println(trackerResponse);
        for(Peer peer : trackerResponse.getPeers())
        {
            byte[] info_hashBytes = Utils.hexStringToBytes(torrent.getInfo_hash());
            byte[] peerId = Utils.CLIENT_ID.getBytes(StandardCharsets.ISO_8859_1); // 20 bytes

            if(peer.sendPeerHandshake(info_hashBytes, peerId) != null)
                System.out.println("success for ip: " + peer.getIp() + " and for port " + peer.getPort());
            else
                System.out.println("error for ip: " + peer.getIp() + " and for port " + peer.getPort());
        }
    }

    @Test
    public void testPeerHandler() throws Exception
    {
        Torrent torrent = TorrentParser.parseTorrent("../torrents/sample.torrent");
        Tracker tracker = new Tracker(torrent);
        TrackerResponseParser trackerResponseParser = new TrackerResponseParser();
        TrackerResponse trackerResponse = trackerResponseParser.parseTrackerResponse(tracker.sendTrackerRequest());
        byte[] info_hashBytes = Utils.hexStringToBytes(torrent.getInfo_hash());
        byte[] peerId = Utils.CLIENT_ID.getBytes(StandardCharsets.ISO_8859_1); // 20 bytes

        PeerHandler peerHandler = new PeerHandler(trackerResponse.getPeers(), info_hashBytes, peerId, torrent);
        peerHandler.setActivePeers();
    }
}