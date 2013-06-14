package fr.esrf.icat.ingesterPilot;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import fr.esrf.icat.ingesterPilot.IcatPilotIngester;
import fr.esrf.icat.ingesterPilot.SmisUtils;

public class AppTest {

	private static final Logger log = Logger.getLogger(IcatPilotIngester.class.getName());
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
	 * Test it the transformation of a tomoDb xml file in an Icat compatible xml
	 * file works.
	 */
	@Ignore
	@Test
	public void testTransformTomoToIcatXml() {

		String currentDir = System.getProperty("user.dir");
		//String path = currentDir.replace("/", "//").concat("\\resources\\");
		String path = currentDir.concat(File.separatorChar+"resources"+File.separatorChar);
		String inXML  = path + "TomoDB_test_file.xml";
		String inXSL  = path + "tomoDB_to_Icat.xsl";
		String outXML = path + "tomoTest.xml";
		//String expectedXML = path + "expected_tomoTest.xml";

		// Transform the TomoDB file
		// ---------------------------------------------------------------------
		try {
			TomoDBtoICAT.trasformTomo_in_ICAT(inXSL, inXML, outXML);
		} catch (TransformerException e) {
			log.error("Impossible to convert TomoDB xml file in ICAT xml file");
		}
		
		
	}

	/**
	 * Copy a file from a remote location to another location (local).
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void copyDirectoryTest() throws IOException {

		
		  File source = new File(
		  "\\\\gy\\visitor\\si2539\\id13\\PROCESS\\SESSION2\\tmp-volatile-tmp\\out_0108.edf"); 
		  File target = new File(
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
		// rootDirectory+"resources"+File.separatorChar+"icat_Catalogue.xml";
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
	 * Test to replase a "placeholder"in a xml file
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
		try {
			IcatPilotIngester.readRemoteFolder_FormatXML_AppendToFile(remoteDir, xmlFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
		/**
	 * Stript to:
	 * Deleta ALL content of ICAT database
	 * Create root user and rules
	 * Create ESRF main users, groups and other data
	 * Import all parameter required by ID19, ID11 and ID22  
	 * 
	 * Aftere this, the ingester should be able to ingest any TomoDB files coming from ID19, ID11 or ID22. 
	 */
	@Ignore
	@Test
	public void InitIcatDB() {
		try {
			
			IcatDbSetup.SetupDB();
			
			
		/*	// Clean ALL data in the DB and create ROOT user and group
			String filename = rootDirectory+"resources"+File.separatorChar+"clean-all.xml";
			// to Delete needs to be ROOT.
			IcatPilotIngester.importXmlFile(filename,IcatSession.ROOT_USER);

			
			// Create all requiered users and rules
			IcatDbSetup.SetupEsrfSpecificData();
			
			
			// Import all ParameterType relative to ID19
			/*filename = rootDirectory+"resources"+File.separatorChar+"ParamTypeID19.xml";
			IcatPilotIngester.importXmlFile(filename);* /
			IcatDbSetup.SetupBeamlineSpecificData();
        */
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	@Ignore
	@Test
	public void testSMISgetInvestigationByInstrumentAndDate() throws Exception {
		
		
		Calendar calFrom = new GregorianCalendar(2013, 01, 01);
		Calendar calTo   = new GregorianCalendar(2013, 05, 31);
		SmisUtils.getInvestigationsByDates(calFrom, calTo);
	
	
	}
	
	@Ignore
	@Test
	public void testCommentXmlNodeInFile() throws Exception {
	
		String currentDir = System.getProperty("user.dir");
		// Clean ALL data in the DB and create ROOT user and group
		String filename = currentDir+File.separatorChar+"resources"+File.separatorChar+"tomoTest.xml";
		File xmlFile = new File(filename);
		IcatPilotIngester.commentXmlNodeInFile(xmlFile, "investigationParameter");
	}


	//@Ignore
	@Test
	public void testCreateNewRules() throws Exception {
		
		IcatDbSetup.addRulesToIcat();
	
	}
	
}