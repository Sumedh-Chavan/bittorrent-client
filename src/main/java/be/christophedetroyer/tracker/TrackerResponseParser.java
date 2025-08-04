package be.christophedetroyer.tracker;

import be.christophedetroyer.bencoding.Reader;
import be.christophedetroyer.bencoding.types.BByteString;
import be.christophedetroyer.bencoding.types.BDictionary;
import be.christophedetroyer.bencoding.types.BInt;
import be.christophedetroyer.bencoding.types.IBencodable;

import java.text.ParseException;
import java.util.List;

public class TrackerResponseParser {


    public TrackerResponse parseTrackerResponse(String response)
    {
        Reader r = new Reader(response);

        return parseTrackerResponse(r);
    }

    public TrackerResponse parseTrackerResponse(Reader r)
    {
        List<IBencodable> x = r.read();
        if (x.size() != 1)
            throw new Error("Parsing .torrent yielded wrong number of bencoding structs.");
        try
        {
            return parseTrackerResponse(x.get(0));
        } catch (ParseException e)
        {
            System.err.println("Error parsing torrent!");
        }
        return null;
    }

    public TrackerResponse parseTrackerResponse(Object o) throws ParseException
    {
        if (o instanceof IBencodable)
        {
            BDictionary responseDictionary = (BDictionary) o;
            TrackerResponse trackerResponse = new TrackerResponse();

            ///////////////////////////////////
            //// OBLIGATED FIELDS /////////////
            ///////////////////////////////////
            trackerResponse.setInterval(parseInterval(responseDictionary));
            return trackerResponse;
        }

        return  null;
    }

    public Long parseInterval(BDictionary info)
    {
        if (null != info.find(new BByteString("interval")))
            return ((BInt) info.find(new BByteString("interval"))).getValue();
        return null;
    }
}
