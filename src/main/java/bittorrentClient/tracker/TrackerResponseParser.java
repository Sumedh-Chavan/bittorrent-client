package bittorrentClient.tracker;

import bittorrentClient.bencoding.Reader;
import bittorrentClient.bencoding.types.BByteString;
import bittorrentClient.bencoding.types.BDictionary;
import bittorrentClient.bencoding.types.BInt;
import bittorrentClient.bencoding.types.IBencodable;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackerResponseParser {

    public TrackerResponse parseTrackerResponse(byte[] responseBytes) throws Exception {
        Bencode bencode = new Bencode();
        Map<String, Object> decoded = bencode.decode(responseBytes, Type.DICTIONARY);

        return parseTrackerResponse(decoded);
    }

    private TrackerResponse parseTrackerResponse(Map<String, Object> decoded) throws Exception {

        TrackerResponse response = new TrackerResponse();

        // Parse common fields
        response.setFailureReason((String) decoded.get("failure reason"));
        response.setWarningMessage((String) decoded.get("warning message"));
        response.setInterval(toLong(decoded.get("interval")));
        response.setMinInterval(toLong(decoded.get("min interval")));
        response.setTrackerId((String) decoded.get("tracker id"));
        response.setComplete(toLong(decoded.get("complete")));
        response.setIncomplete(toLong(decoded.get("incomplete")));

        Object peersObj = decoded.get("peers");

        if (peersObj instanceof List) {
            // Dictionary model
            List<Map<String, Object>> peerDictList = (List<Map<String, Object>>) peersObj;
            List<PeerInfo> peers = new ArrayList<>();
            for (Map<String, Object> peerDict : peerDictList) {
                String peerId = (String) peerDict.get("peer id");
                String ip = (String) peerDict.get("ip");
                Long port = toLong(peerDict.get("port"));
                peers.add(new PeerInfo(peerId, ip, port));
            }
            response.setPeers(peers);

        } else if (peersObj instanceof byte[]) {
            // Compact binary model
            byte[] peersCompact;
            if (peersObj instanceof byte[]) {
                peersCompact = (byte[]) peersObj;
            } else {
                // Convert compact peer string to byte[]
                try {
                    peersCompact = ((String) peersObj).getBytes("ISO-8859-1");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decode compact peer list", e);
                }
            }
            response.setBinaryPeers(peersCompact);
        }
        else if (peersObj instanceof String) {
            // Convert string to byte[] using ISO-8859-1 (1:1 byte mapping)
            response.setPeers(parsePeerListStringType(decoded));
        }

        return response;

    }

    private List<PeerInfo> parsePeerListStringType(Map<String, Object> decoded) throws Exception {
        Object peersObj = decoded.get("peers");
        if (peersObj instanceof String) {
            byte[] peersCompact;
            List<PeerInfo> peers = new ArrayList<>();
            peersCompact = ((String) peersObj).getBytes("ISO-8859-1");
            for (int i = 0; i + 6 < peersCompact.length; i += 6) {
                byte[] ipBytes = new byte[4];
                System.arraycopy(peersCompact, i, ipBytes, 0, 4);
                long port = ((peersCompact[i + 4] & 0xFF) << 8) | (peersCompact[i + 5] & 0xFF);
                String ip = InetAddress.getByAddress(ipBytes).getHostAddress();
                String peerId = null;
                PeerInfo peerInfo = new PeerInfo(peerId, ip, port);
                peers.add(peerInfo);
            }
            return peers;
        }
        return null;
    }

    private byte[] parsePeerListByteArrayType(Map<String, Object> decoded) throws Exception
    {
        // Compact binary model
        Object peersObj = decoded.get("peers");
        byte[] peersCompact;
        if (peersObj instanceof byte[]) {
            peersCompact = (byte[]) peersObj;
        } else {
            // Convert compact peer string to byte[]
            try {
                peersCompact = ((String) peersObj).getBytes("ISO-8859-1");
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode compact peer list", e);
            }
        }

        return peersCompact;
    }

    private Long toLong(Object obj) {
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Long) return (Long) obj;
        return null;
    }
}
