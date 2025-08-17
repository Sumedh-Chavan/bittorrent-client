package bittorrentClient.pieceHandling;

import bittorrentClient.pojo.Torrent;

import java.util.ArrayList;

public class pieceHandler
{
    byte [][] pieces;
    private int pieceBlockOffset;
    private int pieceIndex;
    private int totalPieces;
    private int totalBlocks;

    public pieceHandler(Torrent torrent)
    {
        pieces = new byte[(int) (torrent.getTotalSize() / torrent.getPieceLength()) + 1][];
        pieceBlockOffset = 0;
        pieceIndex = 0;

        totalPieces = (int) (torrent.getTotalSize() / torrent.getPieceLength()) + 1;
        totalBlocks = (int) (torrent.getPieceLength() / 2^14) + 1;
    }

    public ArrayList<Integer> handleBlock(int index, int offset, byte[] payload)
    {
        System.arraycopy(payload, 0, pieces[index], offset, payload.length);

        ArrayList<Integer> res = new ArrayList<Integer>();
        pieceBlockOffset++;
        if(pieceBlockOffset == totalPieces)
        {
            pieceBlockOffset = 0;
            pieceIndex++;
        }

        if(pieceBlockOffset == totalPieces)
        {

            res.add(-1);
            res.add(-1);

            return res;
        }

        res.add(pieceIndex);
        res.add(pieceBlockOffset);

        return res;
    }
}
