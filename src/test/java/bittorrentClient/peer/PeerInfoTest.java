package bittorrentClient.peer;

import bittorrentClient.torrent.Torrent;
import bittorrentClient.torrent.TorrentParser;
import bittorrentClient.tracker.Tracker;
import bittorrentClient.tracker.TrackerResponse;
import bittorrentClient.tracker.TrackerResponseParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;

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
            if(peerInfo.sendPeerHandshake(torrent.getInfo_hash(), tracker.getPeerId()))
                System.out.println("success for ip: " + peerInfo.getIp() + " and for port " + peerInfo.getPort());
            else
                System.out.println("error for ip: " + peerInfo.getIp() + " and for port " + peerInfo.getPort());
        }


        trackerResponse.getPeers().get(0).sendPeerHandshake(torrent.getInfo_hash(), tracker.getPeerId());

        System.out.println("tracker response is ");
        System.out.println(trackerResponse);
    }
}