package bittorrentClient.peer;

import bittorrentClient.torrent.Torrent;
import bittorrentClient.torrent.TorrentParser;
import bittorrentClient.tracker.Tracker;
import bittorrentClient.tracker.TrackerResponse;
import bittorrentClient.tracker.TrackerResponseParser;
import bittorrentClient.utils.Utils;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PeerInfoTest {

    public void something()
    {
        PeerInfo peerInfo = new PeerInfo();
        peerInfo.getPort();
    }

    @Test
    public void testPeerInfo() throws Exception
    {
        Torrent torrent = TorrentParser.parseTorrent("/home/ritesh/Downloads/2001259.torrent");
        Tracker tracker = new Tracker(torrent);
        TrackerResponseParser trackerResponseParser = new TrackerResponseParser();
        TrackerResponse trackerResponse = trackerResponseParser.parseTrackerResponse(tracker.sendTrackerRequest());
        for(PeerInfo peerInfo: trackerResponse.getPeers())
        {
            byte[] info_hashBytes = Utils.hexStringToBytes(torrent.getInfo_hash());
            byte[] peerId = Utils.CLIENT_ID.getBytes(StandardCharsets.ISO_8859_1); // 20 bytes

            if(peerInfo.sendPeerHandshake(info_hashBytes, peerId))
                System.out.println("success for ip: " + peerInfo.getIp() + " and for port " + peerInfo.getPort());
            else
                System.out.println("error for ip: " + peerInfo.getIp() + " and for port " + peerInfo.getPort());
        }

        System.out.println("tracker response is ");
        System.out.println(trackerResponse);
    }
}