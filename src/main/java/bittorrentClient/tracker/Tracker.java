package bittorrentClient.tracker;

import bittorrentClient.torrent.Torrent;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Random;

public class Tracker {

    private final Torrent torrent;

    public Tracker(Torrent torrent) {
        this.torrent = torrent;
    }

    // Convert hex string to byte[]
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }

    // Byte-by-byte percent encoding
    public static String urlEncodeBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%%%02X", b & 0xFF));
        }
        return sb.toString();
    }

    // Given hex info_hash string, return correctly URL-encoded value
    public static String encodeInfoHash(String hexInfoHash) {
        byte[] infoHashBytes = hexToBytes(hexInfoHash);
        return urlEncodeBytes(infoHashBytes);
    }

    /**
     * the function sends the traker get requests to the tracker
     * and gets the tracker response from the tracker in the bencoded
     * format.
     * @return
     */
    public byte[] sendTrackerRequest()
    {
        try {
        String urlWithParams = getActiveAnnounce() +
                "?info_hash=" + encodeInfoHash(torrent.getInfo_hash()) +
                "&peer_id=" + generatePeerId() +
                "&port=" + 6010 +
                "&uploaded=0&downloaded=0&left="+ torrent.getTotalSize() +"&event=started" +
                "&compact=0";

            // Example: Append basic tracker parameters

            System.out.println("url with Params is " + urlWithParams);

            URL url = new URL(urlWithParams);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "BitTorrentSimulator/1.0");

            int responseCode = connection.getResponseCode();
            System.out.println("Tracker responded with HTTP " + responseCode);

            InputStream in = connection.getInputStream();
            byte[] responseBytes = in.readAllBytes();
            in.close();

            System.out.println(new String(responseBytes));

            in.close();
            connection.disconnect();
            return responseBytes;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getActiveAnnounce()
    {
        List<String> announceList = torrent.getAnnounceList();

        for(String announce: announceList) {
            if (announce.startsWith("http") && isTrackerReachable(announce))
            {
                System.out.println("active announce: " + announce);
                return announce;
            }
        }

        return null;
    }

    public static String generatePeerId() {
        String prefix = "-SIM1000-"; // "SIM" is your client ID; "1000" is version 1.0.0
        StringBuilder sb = new StringBuilder(prefix);

        Random random = new Random();

        for (int i = 0; i < 20 - prefix.length(); i++) {
            char c = (char) ('0' + random.nextInt(10)); // digits only
            sb.append(c);
        }

        return sb.toString(); // 20 bytes total
    }

    public static boolean isTrackerReachable(String trackerUrl) {
        try {
            URL url = new URL(trackerUrl);
            String host = url.getHost();
            int port = (url.getPort() != -1) ? url.getPort() : url.getDefaultPort();

            // Try connecting via TCP
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 3000); // 3 sec timeout
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
