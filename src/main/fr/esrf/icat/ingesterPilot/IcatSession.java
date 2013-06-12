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

	public static String ICAT_HOSTNAME = "http://ovm-icat.esrf.fr:8080";// "https://wwws.esrf.fr";
	public static String ICAT_ESRF_URL_ENDPOINT = "/icat";
	public static String ICAT_SECURITY_PLUGIN = "db"; // db|ldap

	// Installation User: to be use ONLY to create the ADMIN user
	public static String ROOT_USER = "ROOT";
	public static String ROOT_USERNAME = "root";
	public static String ROOT_PSW = "icat";

	
	
	// Administrator: to be used to enter ESRF specific metadata in an EMPTY DB
	public static String ADMIN_USER = "ADMIN";
	public static String ADMIN_USERNAME = "admin";
	public static String ADMIN_PSW = "admin";

	// User specific to the Beamline serving
	public static String INGESTER_USER = "INGESTER_ID19";
	public static String INGESTER_USERNAME = "ingester_id19"; 
	public static String INGESTER_PSW = "ingester";

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

		URL icatUrl = new URL(ICAT_HOSTNAME + ICAT_ESRF_URL_ENDPOINT+ "/ICATService/ICAT?wsdl");
		QName qName = new QName("http://icatproject.org", "ICATService");
		ICATService service = new ICATService(icatUrl, qName);
		icat = service.getICATPort();

		Credentials credentials = new Credentials();
		java.util.List<Entry> entries = credentials.getEntry();
		Entry e = new Entry();
		e.setKey("username");
		e.setValue(INGESTER_USERNAME);
		entries.add(e);

		e = new Entry();
		e.setKey("password");
		e.setValue(INGESTER_PSW);
		entries.add(e);
		
		logger.info("Connect to ICAT as user: '"+INGESTER_USERNAME+"'");
		logger.info("ICAT Version: "+ icat.getApiVersion());

		sessionId = icat.login(ICAT_SECURITY_PLUGIN, credentials);
		logger.info("CONNECTED to Icat. Session id: " + sessionId);
	}

	public static void connect(String user) throws IcatException_Exception,
			MalformedURLException {

		String usr = null, psw = null;
		if(user.equals(ROOT_USER)){
			usr = ROOT_USERNAME;
			psw = ROOT_PSW;
		}else if(user.equals(ADMIN_USER)){
			usr = ADMIN_USERNAME;
			psw = ADMIN_PSW;
		}else if(user.equals(INGESTER_USER)){
			usr = INGESTER_USERNAME;
			psw = INGESTER_PSW;
		} 	
		
		URL icatUrl = new URL(ICAT_HOSTNAME + "/icat/ICATService/ICAT?wsdl");
		QName qName = new QName("http://icatproject.org", "ICATService");
		ICATService service = new ICATService(icatUrl, qName);
		icat = service.getICATPort();

		Credentials credentials = new Credentials();
		java.util.List<Entry> entries = credentials.getEntry();
		Entry e = new Entry();
		e.setKey("username");
		e.setValue(usr);
		entries.add(e);

		e = new Entry();
		e.setKey("password");
		e.setValue(psw);
		entries.add(e);

		System.out.println(icat.getApiVersion());

		sessionId = icat.login(ICAT_SECURITY_PLUGIN, credentials);
		logger.info("CONNECTED to Icat. Session id: " + sessionId);
		System.out.println("CONNECTED to Icat. Session id: " + sessionId);
	}

	public static void logOutIcat() {
		if (icat != null && sessionId != null) {
			try {
				icat.logout(sessionId);
				sessionId = null;
				icat = null;
			} catch (IcatException_Exception e) {
				logger.error("Unable to log out from Icat '" + sessionId
						+ "' session.");
			}
		}

	}
}
