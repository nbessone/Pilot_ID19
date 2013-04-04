package fr.esrf.icat.ingesterPilot;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.rpc.ServiceException;

import jmis1.jboss_net.services.SMISWebService.SMISWebServiceLocalServiceLocator;
import jmis1.jboss_net.services.SMISWebService.SMISWebServiceSoapBindingStub;

import org.apache.log4j.Logger;


public class SmisSession {

	private static final String SERVER_URL = "http://vmis1:8080/jboss-net/services/SMISWebService";
	private static final String SMIS_USERNAME = "ISPYB";
	private static final String SMIS_PSW = "ISPYB!=PXWEB";
	
	private static SMISWebServiceSoapBindingStub stub = null;
	private static final Logger logger = Logger
			.getLogger(IcatPilotIngester.class.getName());

	SmisSession() throws  ServiceException, MalformedURLException {
		connect();

	}

	public static SMISWebServiceSoapBindingStub getStub() throws MalformedURLException, ServiceException   {
		if (stub == null) {
			connect();
		}
		return stub;
	}

	

	public static void connect() throws ServiceException, MalformedURLException {
		URL myWSUrl = new URL(SERVER_URL);
		stub = (SMISWebServiceSoapBindingStub) new SMISWebServiceLocalServiceLocator()
				.getSMISWebService(myWSUrl);
		stub.setUsername(SMIS_USERNAME);
		stub.setPassword(SMIS_PSW);
		stub.setTimeout(60 * 1000);
		
		logger.info("CONNECTED to SMIS.");
	}


}
