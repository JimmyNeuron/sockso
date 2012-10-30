package com.pugh.sockso.scrobbler;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.pugh.sockso.music.Track;
import com.pugh.sockso.web.User;

public class LastFmScrobblerQueue
{
	private static LastFmScrobblerQueue ref;
	private static Logger log = Logger.getLogger(LastFmScrobblerQueue.class);
	private final ConcurrentLinkedQueue<LastFmScrobblerData> dataQueue;
	
	private LastFmScrobblerQueue()
	{
		dataQueue = new ConcurrentLinkedQueue<LastFmScrobblerData>();
	}
	
	public static synchronized LastFmScrobblerQueue getSingletonObject()
    {
      if (ref == null)
          ref = new LastFmScrobblerQueue();
      
      return ref;
    }
	
	public void addPlay(User user, Track track)
	{
		log.debug("New track update created for submission.");
		
		try
		{
			dataQueue.add(new LastFmScrobblerData(
					              user.getLastfmName(),
					              user.getLastfmPass(),
					              track.getArtist().getName(),
					              track.getName(),
					              track.getAlbum().getName(),
					              track.getNumber(),
					              track.getLength()));
		}
		catch(Exception exc)
		{
			log.info("User requested scrobble, but data was invalid.");
		}
	}
	
	public LastFmScrobblerData getPlay()
	{
		LastFmScrobblerData data = dataQueue.poll();
		
		if(data != null)
			log.debug("Fetching top of queue with data.");
		
		return data;
	}
}
