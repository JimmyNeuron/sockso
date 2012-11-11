package com.pugh.sockso.scrobbler;

// import org.apache.log4j.Logger;

import de.umass.lastfm.scrobble.ScrobbleData;

public class LastFmScrobblerData
{
	// private static Logger log = Logger.getLogger(LastFmScrobblerData.class);
	
	public static enum State { NOTIFY, SUBMIT, IDLE };
	
	public String username;
	public String md5Pass;
	public long starttime;
	public long submissiontime;
	public long endtime;
	public State state;
	public ScrobbleData scrobbleData;
	
	public LastFmScrobblerData(String username, String md5Pass, String artist, String trackName, String album, int trackNbr, int trackLength, String mbTrackId) throws Exception
	{
		if(trackLength == 0 ||
				   username == null || md5Pass == null ||
				   username.compareTo("") == 0 || md5Pass.compareTo("") == 0)
		{
			throw new Exception("Invalid data.");
		}
		
		this.username = username;
		this.md5Pass = md5Pass;
		
		this.state = State.NOTIFY;
		
		this.starttime = System.currentTimeMillis()/1000;	
		this.endtime = this.starttime + trackLength; // Submit at end of song
	
		// Count until:
		// 1. If track length <= 30 seconds: submit at end.
		// 2. If track length < 30 && <= 480 seconds: submit at half.
		// 3. If track length > 480 seconds: submit after 240 seconds.
		if(trackLength <= 30)
		{
			this.submissiontime = this.starttime + trackLength;
		}
		else if(trackLength > 30 && trackLength <= 480)
		{
			this.submissiontime = this.starttime + (trackLength / 2);
		}
		else
		{
			this.submissiontime = this.starttime + (trackLength / 2);
		}
		
		this.scrobbleData = new ScrobbleData(
                artist,
                trackName,
                (int) this.starttime,
                trackLength,
                album,
                null,
                (mbTrackId != null && !mbTrackId.isEmpty() ? mbTrackId : null),
                trackNbr,
                null);
	}
	
	public LastFmScrobblerData(LastFmScrobblerData data)
	{
		this.username = data.username;
		this.md5Pass = data.md5Pass;
		this.starttime = data.starttime;
		this.submissiontime = data.submissiontime;
		this.endtime = data.endtime;
		this.state = data.state;
		this.scrobbleData = data.scrobbleData;
	}
}
