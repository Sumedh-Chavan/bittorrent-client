package bittorrentClient;

import bittorrentClient.torrent.Torrent;
import bittorrentClient.torrent.TorrentParser;
import bittorrentClient.tracker.Tracker;

public class Application {

    public static void main(String[] args)
    {
        Application application = new Application();
        application.start();
    }

    public void start()
    {
        try {
            Torrent torrent = TorrentParser.parseTorrent("/home/ritesh/Downloads/2001259.torrent");
            System.out.println(torrent);
            Tracker tracker = new Tracker(torrent);
            byte[] trackerRequestResponse = tracker.sendTrackerRequest();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
