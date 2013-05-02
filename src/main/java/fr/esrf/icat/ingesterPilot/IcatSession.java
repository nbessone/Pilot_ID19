package fr.esrf.icat.ingesterPilot;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.icatproject.ICAT;
import org.icatproject.ICATService;
import org.icatproject.IcatException_Exception;
import org.icatproject.Login.Credentials;
import org.icatproject.Login.Credentials.Entry;

public class IcatSession {

	public static String ICAT_HOSTNAME = "http://ovm-icat.esrf.fr:8080";
	public static String ICAT_SECURITY_PLUGIN = "db"; // db|ldap
	public static String ICAT_USERNAME = "root";
	public static String ICAT_PSW = "icat";
	private static String sessionId = null;
	private static ICAT icat = null;
	private static final Logger logger = Logger
			.getLogger(IcatPilotIngester.class.getName());

	IcatSession() throws MalformedURLException, IcatException_Exception {
		connect();

	}

	public static String getSession() throws MalformedURLException,
			IcatException_Exception {
		if (sessionId == null) {
			connect();
		}
		return sessionId;
	}

	public static ICAT getIcat() throws MalformedURLException,
			IcatException_Exception {
		if (icat == null) {
			connect();
		}
		return icat;
	}

	public static void connect() throws IcatException_Exception,
			MalformedURLException {
		
		URL icatUrl = new URL(ICAT_HOSTNAME + "/icat/ICATService/ICAT?wsdl");
		QName qName = new QName("http://icatproject.org", "ICATService");
		ICATService service = new ICATService(icatUrl, qName);
		icat = service.getICATPort();

		Credentials credentials = new Credentials();
		java.util.List<Entry> entries = credentials.getEntry();
		Entry e = new Entry();
		e.setKey("username");
		e.setValue(ICAT_USERNAME);
		entries.add(e);

		e = new Entry();
		e.setKey("password");
		e.setValue(ICAT_PSW);
		entries.add(e);
		
		System.out.println(icat.getApiVersion());

		sessionId = icat.login(ICAT_SECURITY_PLUGIN, credentials);
		logger.info("CONNECTED to Icat. Session id: " + sessionId);
	}

	public static void logOutIcat() {
		if (icat != null && sessionId != null) {
			try {
				icat.logout(sessionId);
				sessionId = null;
				icat = null;
			} catch (IcatException_Exception e) {
				logger.error("Unable to log out from Icat '"+sessionId+"' session.");
			}
		}

	}
}
