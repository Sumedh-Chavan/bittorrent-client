package be.christophedetroyer.tracker;

import be.christophedetroyer.torrent.Torrent;
import be.christophedetroyer.torrent.TorrentParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;

public class TrackerTest {

    @Test
    public void sendTrackerRequestTest() throws IOException {
        try {
            Torrent torrent = TorrentParser.parseTorrent("/home/ritesh/Downloads/2001259.torrent");
            Tracker tracker = new Tracker(torrent);
            String response = tracker.sendTrackerRequest();

        }
        catch (Exception e) {}
    }

}