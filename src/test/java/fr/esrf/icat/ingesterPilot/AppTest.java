package fr.esrf.icat.ingesterPilot;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.esrf.icat.ingesterPilot.IcatPilotIngester;

public class AppTest {

	private static final Logger log = Logger.getLogger(IcatPilotIngester.class
			.getName());
	static String rootDirectory = "/users/bessone/git/Pilot_ID19/";

	/**
	 * Test the data format conversion from the form "1111-11-22T10:20:30" to
	 * "1111/11/22T10:20:30"
	 */
	@Ignore
	@Test
	public void testFormatDate() {

		String res = IcatPilotIngester.formatDate("2012-06-01T18:30:31", "yyyy-MM-dd'T'HH:mm:ss", "yyyy/MM/dd'T'HH:mm:ss");
		assertEquals("2012/06/01T18:30:31", res);
	}

	/**
	 * Test the correctness of the investigation name format standard.
	 */
	@Ignore
	@Test
	public void proposalNameFormatTest() {
		assertEquals("MX410 ID11 10-10-2006/11-10-2006",
				SmisUtils.formatInvestigationName(
						"MX/410 ID11 10-10-2006/11-10-2006", "MX",
						Integer.parseInt("410"), "ID11"));
	}

	/**
	 * Test the transformation of a tomoDb xml file in an Icat compatible xml
	 * file
	 */
	@Ignore
	@Test
	public void testTransformTomoToIcatXml() {

		String currentDir = System.getProperty("user.dir");
		String path = currentDir.replace("/", "//").concat("\\resources\\");
		String inXML = path + "tomoDB_Catalogue.xml";
		String inXSL = path + "tomoDB_to_Icat.xsl";
		String outTXT = path + "tomoTest.xml";

		// Transform the TomoDB file
		// ---------------------------------------------------------------------
		// TomoDBtoICAT.trasformTomo_in_ICAT(inXSL, inXML, outTXT);
	}

	@Ignore
	@Test
	public void copyDirectoryTest() throws IOException {

		
		  File source = new File(
		  "\\\\gy\\visitor\\si2539\\id13\\PROCESS\\SESSION2\\tmp-volatile-tmp\\out_0108.edf"
		  ); File target = new File(
		  "C:\\Users\\bessone.ESRF\\git\\Pilot_ReplaceTomoDB\\ingesterPilot\\resources\\out_0108.edf"
		  );
		  
		  InputStream in = new FileInputStream(source); OutputStream out = new
		  FileOutputStream(target);
		  
		  // Transfer bytes from in to out 
		  byte[] buf = new byte[1024]; 
		  int len; 
		  while ((len = in.read(buf)) > 0) { out.write(buf, 0, len); }
		  in.close(); out.close();
		 
	}

	/**
	 * Remove all data from the Icat DB and import the ID19 parameter types.
	 */
	@Ignore
	@Test
	public void empty_and_reinit_ICAT_DB() {
		String filename =
		// Clean ALL data in the DG except Facility
		rootDirectory+"resources"+File.separatorChar+"clean.xml";
		try {
			 IcatPilotIngester.importXmlFile(filename);

			// Import all ParameterType relative to ID19
			filename = rootDirectory+"resources"+File.separatorChar+"ParamTypeID19.xml";
			 IcatPilotIngester.importXmlFile(filename);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Test the
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void replaceValueInXmlFileTest() throws Exception {
		String currentDir = System.getProperty("user.dir");
		String path = currentDir.replace("/", "//").concat(
				"\\resources\\tomoTest.xml");
		File xmlFile = new File(path);
		try {
			// IcatPilotIngester.replaceStringInFile(xmlFile, "placeholder",
			// "2917");
			// IcatPilotIngester.removeLineContainigStringFromFile(xmlFile,
			// "<ExperimentName>");
			IcatPilotIngester.removeLineContainigStringFromFile(xmlFile,
					"<Instrument>");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Read the content of a remote folder (only .edf file) and append the the
	 * relative data formatted as Icat xml to the file to import.
	 */
	@Ignore
	@Test
	public void readRemoteFolderContentTest() {

		String remoteDir = "\\\\gy\\visitor\\si2539\\id13\\PROCESS\\SESSION2\\tmp-volatile-tmp";
		String currentDir = System.getProperty("user.dir");
		File xmlFile = new File(currentDir.replace("/", "//").concat(
				"\\resources\\tomoTest.xml"));
		// IcatPilotIngester.readRemoteFolderContent(remoteDir, xmlFile);

	}

	@Ignore
	@Test
	public void searchID19Exp() {

		System.out
				.println("------------Test to find experiment from ID19 ---------------");
		
		
		Process p = null;
		String remoteDir = "\\\\gy\\visitor\\";
		String command = "cmd  /c dir " + remoteDir + " /AD /ON /B"; // loop over PROPOSALs

		try {
			p = Runtime.getRuntime().exec(command);
		} catch (IOException e){
			log.error("Impossible run command: '"
					+ command + "' at remote location: '" + remoteDir + "'. "
					+ e.getMessage());
		}
		
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(	p.getInputStream()));

		try {
			String s1;
			while ((s1 = stdInput.readLine()) != null) { // proposal level

				if (!s1.contains("in")){
					
					String path1 = remoteDir + s1;
					String command1 = "cmd  /c dir " + path1	+ " /AD-H /ON /B ^id19*"; // loop over BEAMLINEs
					log.info("** "+ command1);
					
					Process p1 = Runtime.getRuntime().exec(command1);
					BufferedReader stdInput1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
					
					String s2 = null;
					while ((s2 = stdInput1.readLine()) != null) { // Beamline level
	
						//if (s2.contains("id19")) { // System.out.println("TROVATA");
						log.info("---"+s2 );
							String path2 = path1 +File.separatorChar+ s2;
							
						//	System.out.println(path1);
							String command2 = "cmd  /c dir " + path2 + " /AD /B";// loop over DATASETs
							Process p2 = null;
	
							p2 = Runtime.getRuntime().exec(command2);
							BufferedReader stdInput2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
							
							String s3 = null;
							while ((s3 = stdInput2.readLine()) != null) {// DataSet level
								
								
								String path3 = path2 +File.separatorChar+ s3;						
								String command3 = "cmd  /c dir " + path3 + " /A-D /B *.xml";
								//String command3 = "exist { " + path3 + File.separatorChar+s3+ ".xml}";
								
								Process p3 = Runtime.getRuntime().exec(command3);
								BufferedReader stdInput3 = new BufferedReader(new InputStreamReader(p3.getInputStream()));
								
								String s4 = null;
								while ((s4 = stdInput3.readLine()) != null) {
									if (s4.contains(s3+".xml")) {
										
										log.info(path3 + File.separatorChar+s4);
									}
									
								}
								
								
	
							}
				
						//}
					}

				}

			}
		} catch (IOException e1) { // TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	/**
	 * Retrieve the Experiment perfomed ona specific Beamline in a certan period of time 
	 */
	@Test
	public void getLatestInvestigationsTest() {
		
		
		SmisUtils.getInvestigationFromSmisByBeamlinaAndDate("ID19", "2013/04/22T00:00:00", "2013/04/30T00:00:00");
		
		
		
		
		
		
		
		
	}
	

}
