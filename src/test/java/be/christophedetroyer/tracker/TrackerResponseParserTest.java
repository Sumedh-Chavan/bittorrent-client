package be.christophedetroyer.tracker;

import be.christophedetroyer.torrent.Torrent;
import be.christophedetroyer.torrent.TorrentParser;
import junit.framework.TestCase;
import org.junit.Test;

public class TrackerResponseParserTest {
    @Test
    public void testParseTrackerResponse() throws Exception
    {
        Torrent torrent = TorrentParser.parseTorrent("/home/ritesh/Downloads/2001259.torrent");
        Tracker tracker = new Tracker(torrent);
        TrackerResponseParser trackerResponseParser = new TrackerResponseParser();
        TrackerResponse trackerResponse = trackerResponseParser.parseTrackerResponse(tracker.sendTrackerRequest());
        System.out.println(trackerResponse);
    }
}