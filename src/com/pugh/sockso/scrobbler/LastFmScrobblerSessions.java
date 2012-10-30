package com.pugh.sockso.scrobbler;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import de.umass.lastfm.Session;

public class LastFmScrobblerSessions
{
	private static ConcurrentHashMap<String, Session> _sessions;
	private static LastFmScrobblerSessions ref = null;
	
	private static Logger log = Logger.getLogger(LastFmScrobblerSessions.class);
	
	private LastFmScrobblerSessions()
	{
		_sessions = new ConcurrentHashMap<String, Session>();
	}
	
	public static LastFmScrobblerSessions getSingletonObject()
	{
		if(ref == null)
			return new LastFmScrobblerSessions();
		
		return ref;
	}
	
	public Session getSessionOf(String username)
	{
		Session session = _sessions.get(username);
		return session;
	}
	
	public void addSessionFor(String username, Session session)
	{
		_sessions.put(username, session);
		log.debug("New session added for " + username + ", nr of sessions = " + _sessions.size());
	}
}
