package com.sapereapi.db;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.Session;
import com.sapereapi.util.UtilDates;

public class SessionManager {
	private static Session session = null;
	private static SapereLogger logger = SapereLogger.getInstance();

	static {
		String sessionNumber = UtilDates.generateSessionNumber();
		session = new Session();
		session.setNumber(sessionNumber);
	}

	public static String getSessionNumber() {
		if (session == null) {
			return null;
		}
		return session.getNumber();
	}

	public static Session getSession() {
		return session;
	}

	public static Long getSessionId() {
		if (session == null) {
			return null;
		}
		return session.getId();
	}

	public static void registerSession() {
		String sessionNumber = getSessionNumber();
		try {
			session = EnergyDbHelper.registerSession(sessionNumber);
		} catch (HandlingException e) {
			logger.error(e);
		}
	}

	public static void changeSessionNumber() {
		String sessionNumber = UtilDates.generateSessionNumber();
		session.setNumber(sessionNumber);
		registerSession();
	}
}
