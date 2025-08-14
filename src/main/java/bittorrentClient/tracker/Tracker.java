package bittorrentClient.tracker;

import bittorrentClient.torrent.Torrent;
import bittorrentClient.utils.Utils;

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
    private final String peerId;

    public Tracker(Torrent torrent) {
        this.torrent = torrent;
        peerId = generatePeerId();
    }

    // Convert hex string to byte[]

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
        byte[] infoHashBytes = Utils.hexStringToBytes(hexInfoHash);
        return urlEncodeBytes(infoHashBytes);
    }

    /**
     * the function sends the traker get requests to the tracker
     * and gets the tracker response from the tracker in the bencoded
     * format.
     *
     * @return
     */
    public byte[] sendTrackerRequest() {
        try {
            String urlWithParams = getActiveAnnounce() +
                    "?info_hash=" + encodeInfoHash(torrent.getInfo_hash()) +
                    "&peer_id=" + peerId +
                    "&port=" + 6010 +
                    "&uploaded=0&downloaded=0&left=" + torrent.getTotalSize() + "&event=started";


            System.out.println("url with Params is " + urlWithParams);

            URL url = new URL(urlWithParams);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "BitTorrentSimulator/1.0");
            connection.setRequestProperty("Host", url.getHost()); // Ensure Host header is set
            connection.setRequestProperty("Connection", "close"); // Optional: ensure no keep-alive
            connection.setUseCaches(false);

            int responseCode = connection.getResponseCode();
            System.out.println("Tracker responded with HTTP " + responseCode);

            InputStream in = connection.getInputStream();
            byte[] responseBytes = in.readAllBytes();
            in.close();

            System.out.println(new String(responseBytes));

            connection.disconnect();
            return responseBytes;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

//    public byte[] sendTrackerRequest() {
//        try {
//            String host = "nyaa.tracker.wf";
//            int port = 7777;
//
//            String pathWithParams = "/announce" +
//                    "?info_hash=" + encodeInfoHash(torrent.getInfo_hash()) +
//                    "&peer_id=" + peerId +
//                    "&port=" + 6010 +
//                    "&uploaded=0&downloaded=0&left=" + torrent.getTotalSize() +
//                    "&event=started";
//
//            System.out.println("Request path: " + pathWithParams);
//
//            Socket socket = new Socket(host, port);
//            socket.setSoTimeout(5000);
//
//            OutputStream out = socket.getOutputStream();
//            InputStream in = socket.getInputStream();
//
//            // Full HTTP/1.1 request
//            String request =
//                    "GET " + pathWithParams + " HTTP/1.1\r\n" +
//                            "Host: " + host + ":" + port + "\r\n" + // include port for HTTP/1.1
//                            "User-Agent: BitTorrentSimulator/1.0\r\n" +
//                            "Accept: */*\r\n" +
//                            "Accept-Encoding: identity\r\n" +
//                            "Connection: close\r\n\r\n";
//
//            out.write(request.getBytes("ISO-8859-1")); // preserve raw bytes
//            out.flush();
//
//            // Read the full response
//            ByteArrayOutputStream rawResponse = new ByteArrayOutputStream();
//            byte[] buf = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = in.read(buf)) != -1) {
//                rawResponse.write(buf, 0, bytesRead);
//            }
//
//            socket.close();
//
//            byte[] fullResponse = rawResponse.toByteArray();
//
//            // Separate headers from body
//            String responseText = new String(fullResponse, "ISO-8859-1");
//            int headerEndIndex = responseText.indexOf("\r\n\r\n");
//            if (headerEndIndex == -1) {
//                throw new IOException("Invalid HTTP response from tracker");
//            }
//
//            // The actual tracker data (bencoded) starts after the headers
//            byte[] body = Arrays.copyOfRange(fullResponse, headerEndIndex + 4, fullResponse.length);
//
//            System.out.println("Tracker raw response body (bencoded):");
//            System.out.println(new String(body, "ISO-8859-1"));
//
//            return body;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


//    public byte[] sendTrackerRequest() {
//        try {
//            String pathWithParams = "/announce" +
//                    "?info_hash=" + encodeInfoHash(torrent.getInfo_hash()) +
//                    "&peer_id=" + peerId +
//                    "&port=" + 6010 +
//                    "&uploaded=0&downloaded=0&left=" + torrent.getTotalSize() +
//                    "&event=started";
//
//            System.out.println("path with Params is " + pathWithParams);
//
//            // Extract host & port
//            String host = "nyaa.tracker.wf";
//            int port = 7777;
//
//            // Open TCP connection
//            Socket socket = new Socket(host, port);
//            socket.setSoTimeout(5000); // timeout in ms
//
//            OutputStream out = socket.getOutputStream();
//            InputStream in = socket.getInputStream();
//
//            // Build HTTP/1.0 request manually
//            String request = "GET " + pathWithParams + " HTTP/1.0\r\n" +
//                    "Host: " + host + "\r\n" +
//                    "User-Agent: BitTorrentSimulator/1.0\r\n" +
//                    "Connection: close\r\n\r\n";
//
//            out.write(request.getBytes("ISO-8859-1")); // tracker expects raw bytes
//            out.flush();
//
//            // Read full response
//            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//            byte[] temp = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = in.read(temp)) != -1) {
//                buffer.write(temp, 0, bytesRead);
//            }
//
//            socket.close();
//
//            byte[] responseBytes = buffer.toByteArray();
//            System.out.println(new String(responseBytes, "ISO-8859-1"));
//
//            return responseBytes;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


    private String getActiveAnnounce() {
        List<String> announceList = torrent.getAnnounceList();

        for (String announce : announceList) {
            if (announce.startsWith("http") && isTrackerReachable(announce)) {
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

            // If HTTP(S), try to get the response code
            if (url.getProtocol().startsWith("http")) {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");

                int code = conn.getResponseCode(); // triggers actual connection
                conn.disconnect();

                // If we get any valid HTTP code, tracker is reachable
                return (code >= 100 && code < 600);
            }

            // Otherwise, fall back to simple TCP socket check
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 3000);
                socket.close();
                return true;
            }

        } catch (IOException e) {
            return false;
        }
    }

    public String getPeerId() {
        return peerId;
    }
    // Example usage
}
