package bittorrentClient.tracker;

import bittorrentClient.pojo.Torrent;
import bittorrentClient.torrent.TorrentParser;
import org.junit.Test;

import java.io.IOException;

public class TrackerTest {

    @Test
    public void sendTrackerRequestTest() throws IOException {
        try {
            Torrent torrent = TorrentParser.parseTorrent("/home/ritesh/Downloads/2001259.torrent");
            Tracker tracker = new Tracker(torrent);
            byte[] bytes = tracker.sendTrackerRequest();
        }
        catch (Exception e) {}
    }

}