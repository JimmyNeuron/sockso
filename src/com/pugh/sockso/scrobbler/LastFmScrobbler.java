package com.pugh.sockso.scrobbler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.pugh.sockso.Constants;
import com.pugh.sockso.Properties;
import com.pugh.sockso.PropertiesListener;
import com.pugh.sockso.db.Database;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.cache.Cache;
import de.umass.lastfm.cache.DatabaseCache;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class LastFmScrobbler extends Thread implements PropertiesListener
{
	private boolean _run;
	private final LastFmScrobblerSessions _sharedSessions;	// Handle to shared Last.fm sessions
	private final LastFmScrobblerQueue _sharedQueue;		// Handle to shared queue
	private final Properties _properties;
	
	private final HashMap<String, LastFmScrobblerData> _submissionRequests; // The current data to submit.
	
	private static Logger log = Logger.getLogger(LastFmScrobbler.class);
	
	private final static String LastFmKey = "457dba6ea55d766e65eacd28a927b273";
	private final static String LastFmSecret = "610e2b9a553e39a8896a27c1130a523a";
	
	@Inject
	public LastFmScrobbler(final Properties p, final Database db)
	{
		_properties = p;
		
		_submissionRequests = new HashMap<String, LastFmScrobblerData>(32);
		_sharedQueue = LastFmScrobblerQueue.getSingletonObject();
		_sharedSessions = LastFmScrobblerSessions.getSingletonObject();
		
		_run = true;
		
		_properties.addPropertiesListener(this);
		
		Caller.getInstance().setUserAgent("sockso");
		
		try
		{
			Cache lastfmCache = new DatabaseCache(db.getConnection());
			Caller.getInstance().setCache(lastfmCache);
		}
		catch(Exception exc)
		{ } // Ignore error if db cannot be initialized: use file system.
	}
	
	private boolean initSession(String username, String md5Pass)
	{
		Session session;
		
		// Try to get data from the user, otherwise from shared account.
		if(username != null && username.compareTo("") != 0 && md5Pass != null && md5Pass.compareTo("") != 0)
		{
			session = Authenticator.getMobileSession(username, md5Pass, LastFmKey, LastFmSecret);
			log.debug("Created new personal session for Last.fm user " + username + " with password " + md5Pass + ".");
		}
		else
		{
			session = Authenticator.getMobileSession(
	                					_properties.get(Constants.LASTFM_USERNAME),
                						_properties.get(Constants.LASTFM_PASSWORD),
                						LastFmKey,
                						LastFmSecret);
			
			log.debug("Created new session from shared Last.fm account.");
		}
		
		log.debug("Storing session " + (session == null?"=null ":"with data ") + "in shared sessions.");
		
		// Store session
		_sharedSessions.addSessionFor(username, session);
		
		return session != null;
	}
	
	private void checkSubmissionResult(ScrobbleResult result, LastFmScrobblerData data)
	{
		if(!result.isSuccessful())
			if(result.getErrorCode() == 9) // Re-authentication needed
			{
				initSession(data.username, data.md5Pass);
			}
			else
			{
				log.info("Last.fm operation failed with error code = " + result.getErrorCode() + ", http error " + result.getHttpErrorCode());
			}
	}
	
	/**
	 * Update "Now playing".
	 * @param data
	 */
	private void updateNowPlaying(LastFmScrobblerData data, Session session)
	{
		ScrobbleResult result = de.umass.lastfm.Track.updateNowPlaying(data.scrobbleData, session);
		checkSubmissionResult(result, data);
		
		// Caching failed submissions is handled by de.umass.lastfm package.
	}
	
	/**
	 * Submit track data.
	 * @param data
	 */
	private void submit(LastFmScrobblerData data, Session session)
	{
		ScrobbleResult result = de.umass.lastfm.Track.scrobble(data.scrobbleData, session);
		checkSubmissionResult(result, data);
	}
	
	@Override
	public void run() {
		long currentTime;
		long checkTime;
		Session session;
		Iterator<Map.Entry<String, LastFmScrobblerData>> it;
		Map.Entry<String, LastFmScrobblerData> pairKeyValue;
		LastFmScrobblerData scrobbleRequest;
		
		log.info("Starting Last.fm submission thread.");
		
		// Loop...
		while (_run)
		{
			try
			{
				// Update working time.
				currentTime = System.currentTimeMillis()/1000;
				
				// Check if there is new data to submit. Loop during at most 2 seconds.
				checkTime = currentTime;
				scrobbleRequest = _sharedQueue.getPlay();
				while(scrobbleRequest != null && checkTime < currentTime + 2)
				{
					log.debug("Got new scrobble request from user " + scrobbleRequest.username + " with track " + scrobbleRequest.scrobbleData.getArtist() + " - " + scrobbleRequest.scrobbleData.getTrack());
					
					// Init user session.
					session = _sharedSessions.getSessionOf(scrobbleRequest.username);
					if(session == null)
					{
						initSession(scrobbleRequest.username, scrobbleRequest.md5Pass);
					}
					
					// Submit previous if required.
					LastFmScrobblerData previousRequest = _submissionRequests.get(scrobbleRequest.username);
					if(previousRequest != null && previousRequest.state == LastFmScrobblerData.State.SUBMIT && checkTime >= previousRequest.submissiontime)
					{
						log.debug("A previous request was ready for submission. Submitting now...");
						
						submit(previousRequest, session);
					}
					
					// Store new request locally.
					_submissionRequests.put(scrobbleRequest.username, new LastFmScrobblerData(scrobbleRequest));
					
					// Step forward in loop.
					scrobbleRequest = _sharedQueue.getPlay();
					checkTime = System.currentTimeMillis()/1000;
				}
				
				// Update working time.
				currentTime = System.currentTimeMillis()/1000;
				
				// We have all our data. Start scrobbling.
				it = _submissionRequests.entrySet().iterator();
				while(it.hasNext() && checkTime < currentTime + 2)
				{	
					// Get next key-value pair from our local store.
					pairKeyValue = it.next();
					
					scrobbleRequest = pairKeyValue.getValue();
					
					// Init user session.
					session = _sharedSessions.getSessionOf(scrobbleRequest.username);
					if(session == null)
					{
						initSession(scrobbleRequest.username, scrobbleRequest.md5Pass);
					}
					
					if(scrobbleRequest.state == LastFmScrobblerData.State.NOTIFY)
					{
						log.debug("Sending now playing update to Last.fm for user " + scrobbleRequest.username + " with\n" +
					              "artist " + scrobbleRequest.scrobbleData.getArtist() + "\n" +
					              "track " + scrobbleRequest.scrobbleData.getTrack()  + "\n" +
					              "album " + scrobbleRequest.scrobbleData.getAlbum()  + "\n" +
					              "track length " + scrobbleRequest.scrobbleData.getDuration()  + "\n" +
					              "track number " + scrobbleRequest.scrobbleData.getTrackNumber()  + "\n" +
					              "mb track id " + scrobbleRequest.scrobbleData.getMusicBrainzId()  + "\n");
						updateNowPlaying(scrobbleRequest, session);
						scrobbleRequest.state = LastFmScrobblerData.State.SUBMIT;
					}
					
					// Default is to send the scrobble at the end of song, unless the song has been interrupted after half of its track length.
					// This specific situation is handled in the previous while loop.
					if(scrobbleRequest.state == LastFmScrobblerData.State.SUBMIT && currentTime >= scrobbleRequest.endtime)
					{
						log.debug("Sending new submission to Last.fm for user " + scrobbleRequest.username + " with track " + scrobbleRequest.scrobbleData.getArtist() + " - " + scrobbleRequest.scrobbleData.getTrack());
						submit(scrobbleRequest, session);
						scrobbleRequest.state = LastFmScrobblerData.State.IDLE;
					}
				}
				
				scrobbleRequest = null;

				// Sleep 1 second.
				Thread.sleep(1000);
			}
			catch(Exception exc)
			{
				_run = false;
				log.info("Exception caught in Last.fm submission thread: " + exc.toString());
				break;
			}
		}
		
		log.info("Last.fm submission thread ended.");
	}

	@Override
	public void propertiesSaved(final Properties properties)
	{
		String sharedUsername = properties.get(Constants.LASTFM_USERNAME);
		if(sharedUsername != null && sharedUsername.compareTo("") != 0)
		{
			Session session = _sharedSessions.getSessionOf(sharedUsername);
			if(session != null && session.getUsername().compareTo(properties.get(Constants.LASTFM_USERNAME)) != 0)
			{
				log.info("Last.fm credentials updated. Re-initializing session...");
				initSession(sharedUsername, properties.get(Constants.LASTFM_PASSWORD));
			}
		}
	}

	public void shutdown()
	{
		_run = false;
	}
}
