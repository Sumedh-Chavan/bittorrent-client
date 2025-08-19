package bittorrentClient.pieceHandling;

import bittorrentClient.pojo.Torrent;

import java.util.ArrayList;

public class pieceHandler
{
    static byte [][] pieces;
    private int pieceBlockOffset;
    private int pieceIndex;
    private int totalPieces;
    private int totalBlocks;
    private int blockSize = 2^14;
    private long fileSize;

    public pieceHandler(Torrent torrent)
    {
        pieces = new byte[(int) (torrent.getTotalSize() / torrent.getPieceLength()) + 1][];
        pieceBlockOffset = 0;
        pieceIndex = 0;

        totalPieces = (int) (torrent.getTotalSize() / torrent.getPieceLength());
        totalBlocks = (int) (torrent.getPieceLength() / blockSize);
        long fileSize = torrent.getTotalSize();
    }

    public ArrayList<Integer> handleBlock(int index, int offset, byte[] payload)
    {
        System.arraycopy(payload, 0, pieces[index], offset, payload.length);

        ArrayList<Integer> res = new ArrayList<Integer>(3);
        pieceBlockOffset++;
        if(pieceBlockOffset <= totalBlocks)
        {
            res.set(0, pieceIndex);
            res.set(1, (offset + 2 ^ 14));
            res.set(2, blockSize);
        }
        else
        {
            pieceBlockOffset = 0;
            pieceIndex++;

            if(pieceIndex <= totalPieces)
            {
                res.set(0, pieceIndex);
                res.set(1, 0);
                res.set(2, (int) (fileSize - totalPieces * blockSize));
            }
            else {
                res.set(0, -1);
                res.set(1, -1);
                res.set(2, -1);

            }
        }

        return res;
    }
}
