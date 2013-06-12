package fr.esrf.icat.ingesterPilot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.rpc.ServiceException;

import org.apache.log4j.Logger;

import jmis1.jboss_net.services.SMISWebService.SMISWebServiceLocalServiceLocator;
import jmis1.jboss_net.services.SMISWebService.SMISWebServiceSoapBindingStub;

//import org.apache.log4j.Logger;


public class SmisSession {

	private static final String SMIS_SERVER_URL = "http://vmis1:8080/jboss-net/services/SMISWebService";
	private static final String SMIS_USERNAME = null;
	private static final String SMIS_PSW = null;
	
	private static SMISWebServiceSoapBindingStub stub = null;
	private static final Logger logger = Logger
			.getLogger(IcatPilotIngester.class.getName());
	

	SmisSession() throws  ServiceException, IOException {
		connect();

	}

	public static SMISWebServiceSoapBindingStub getStub() throws ServiceException, IOException   {
		if (stub == null) {
			connect();
		}
		return stub;
	}

	

	public static void connect() throws ServiceException, IOException {
		
		if (SMIS_USERNAME.isEmpty() || SMIS_PSW.isEmpty() ){
			String str = "SMISS Login namd or passward not set. Open file SmisSession.java and set a value for the two variables.";
			logger.error(str);
			throw new IOException(str);
		}
		
		URL myWSUrl = new URL(SMIS_SERVER_URL);
		stub = (SMISWebServiceSoapBindingStub) new SMISWebServiceLocalServiceLocator()
				.getSMISWebService(myWSUrl);
		stub.setUsername(SMIS_USERNAME);
		stub.setPassword(SMIS_PSW);
		stub.setTimeout(60 * 1000);
		
		//logger.info("CONNECTED to SMIS.");
	}


}
