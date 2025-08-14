package bittorrentClient.utils;

public class Utils {

    public static String CLIENT_ID = "-JV0001-7H9K3X2B4L5M";

    public static byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }
}
