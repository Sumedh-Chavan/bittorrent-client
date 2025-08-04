package be.christophedetroyer.tracker;

import be.christophedetroyer.torrent.Torrent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

public class Tracker {

    private Torrent torrent;

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

    public String sendTrackerRequest()
    {
        try {
        String urlWithParams = torrent.getAnnounce() +
                "?info_hash=" + encodeInfoHash(torrent.getInfo_hash()) +
                "&peer_id=" + generatePeerId() +
                "&port=" + 6010 +
                "&uploaded=0&downloaded=0&left="+ torrent.getTotalSize() +"&event=started";

            // Example: Append basic tracker parameters

            System.out.println("url with Params is " + urlWithParams);

            URL url = new URL(urlWithParams);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "BitTorrentSimulator/1.0");

            int responseCode = connection.getResponseCode();
            System.out.println("Tracker responded with HTTP " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String responseLine;
            while ((responseLine = in.readLine()) != null) {
                System.out.println(responseLine);
            }

            in.close();
            connection.disconnect();
            return responseLine;
        } catch (IOException e) {
            e.printStackTrace();
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
}
