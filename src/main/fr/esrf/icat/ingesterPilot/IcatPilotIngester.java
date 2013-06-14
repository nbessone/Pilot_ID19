package fr.esrf.icat.ingesterPilot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.icatproject.testclient.IcatXmlTestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IcatPilotIngester {

	/**
	 * @param args
	 */
	static String ROOT_DIRECTORY ;
	public static final String[] BEAMLINES = { "id19", "id11", "id22" };

	public static final List<String> instruments = Arrays.asList(BEAMLINES);
	static String proposalName;
	static Map<String, String> map = new HashMap<String, String>();
	static Long icatInvestigationID = (long) 0;
	private static final Logger logger = Logger
			.getLogger(IcatPilotIngester.class.getName());
	private final static String configFile = "config.proprerties";
	
	

	public static void main(String[] args) {

		
		Properties prop = new Properties();
		try {
			
			prop.load(new FileInputStream(configFile));
			SmisSession.SMIS_USERNAME = prop.getProperty("SMIS_USERNAME");
			SmisSession.SMIS_PSW      = prop.getProperty("SMIS_PSW");
			ROOT_DIRECTORY      = prop.getProperty("ROOT_DIRECTORY");
			

			// run("/data/visitor/md745/id19/0001_HA_769/0001_HA_769.xml");
			//testAllRun();
			importInvestigatinsGotFromSMIS();
			
		} catch (FileNotFoundException e1) {
			logger.error("File '"+configFile+"' not found. It contains necessary information for the execution."+ e1.getMessage());
			
		} catch (Exception e) {
			logger.error("Impossible ingest metadata file. \n" + e.getMessage());
		}
	}

	public static void importInvestigatinsGotFromSMIS() throws Exception {

		/*
		 * Specify the time slot you are interest in the form of <initialDate> : <finalDate>
		 */
		Calendar calFrom = Calendar.getInstance();//  new GregorianCalendar(2013, 01, 01);
		calFrom.add(Calendar.DATE, -1);			  //  YESTERDAY  
		Calendar calTo = Calendar.getInstance();  //  TODAY      new GregorianCalendar(2013, 05, 31);
		
		List<String> listOfTomoDBFiles = SmisUtils
				.getInvestigationsByDates(calFrom, calTo);

		if (listOfTomoDBFiles.isEmpty()){
			logger.info("No metadata to import.");
		}

		for (String file : listOfTomoDBFiles) {

			System.out.println("--> IMPORTING file: " + file);
			try {
				run(file);
			} catch (Exception e) {
				logger.error("Unable to inport in ICAT the TomoDB file: "
						+ file + " . " + e.getMessage());
			}
		}

	}

	/**
	 * Scan all folders in "data/visitor" find all containing a folder
	 * id19,Id11,Id22 and search if it contain datasets with TomoDB xml file.
	 * 
	 * @throws Exception
	 */
	public static void testAllRun() throws Exception {

		/*
		 * LINUX: mount the visitor disk, $ su - $ mkdir -p /visitor/ $ mount
		 * gy.esrf.fr:/data/visitor /visitor/
		 */

		File file = new File(ROOT_DIRECTORY); // === ProposalName directory ===
		String[] directories = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory();
			}
		});

		String proposalDir;

		// Loop over ALL proposals in /data/visitor/
		for (int i = 0; i < directories.length; i++) {
			proposalDir = directories[i];

			if (!proposalDir.contains("in")) { // exclude all INDUSTRIAL proposals

				String beamlinePath = ROOT_DIRECTORY + proposalDir;
				File beamlineFile = new File(beamlinePath); // === BeamlineName directory
				// ===
				FilenameFilter textFilter = new FilenameFilter() {
					// Select only folder listed in "instruments"
					public boolean accept(File dir, String name) {
						String lowercaseName = name.toLowerCase();
						if (instruments.contains(lowercaseName)) {
							return true;
						} else {
							return false;
						}
					}
				};
				String[] beamlineDirectories = beamlineFile.list(textFilter);

				// Loop over all beamlines sub-directory listed in the
				// "filenamefilter"
				for (int n = 0; beamlineDirectories != null && n < beamlineDirectories.length; n++) {

					String beamlineDir = beamlineDirectories[n];

					String dataSetPath = beamlinePath + File.separatorChar + beamlineDir;

					File datasetFile = new File(dataSetPath); // === DatasetName
					// directory ===
					String[] datasetDirectories = datasetFile.list(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return new File(dir, name).isDirectory();
						}
					});

					for (int j = 0; j < datasetDirectories.length; j++) {

						final String datasetDir = datasetDirectories[j];

						String tomoDB_filePath = dataSetPath
								+ File.separatorChar + datasetDir;

						// === Search for TomoDB_fileName ===
						File tomodbFile = new File(tomoDB_filePath);

						FilenameFilter filter = new FilenameFilter() {
							// select only file with a specific name
							public boolean accept(File dir, String name) {
								if (name.contains(datasetDir + ".xml")) {
									return true;
								} else {
									return false;
								}
							}
						};
						// Htere must be only one (can't exist 2 files with same name and location)
						File[] tomodbFilename = tomodbFile.listFiles(filter);

						if (tomodbFilename != null && tomodbFilename.length > 0) {

							try {

								// System.out.println(tomoDB_filePath +
								// File.separatorChar + s3 + ".xml");

								run(tomoDB_filePath + File.separatorChar + datasetDir
										+ ".xml");

							} catch (Exception e) {
								logger.error(e.getMessage());
							}
						}
					}
				}
			}
		}

		logger.info("FINISH - All "+BEAMLINES+" proposals with investigations has been inported....");

	}

	// =======================================================================================================================
	// =======================================================================================================================

	/**
	 * Giving a file path to a TomoDB xml file, the procedure ingest all
	 * required data in ICAT
	 */
	@SuppressWarnings("unchecked")
	public static void run(String remoteFileName) throws Exception {

		/*
		 * Define name and location of required files  
		 */
		String currentDir = System.getProperty("user.dir");
		String path = currentDir.concat(File.separatorChar + "resources"
				+ File.separatorChar);
		String tomoDB_XML = path + "imput.xml";
		String inXSL      = path + "tomoDB_to_Icat.xsl";
		String outTXT     = path + "tomoTest.xml";

		
		logger
		.info("BEGIN ingestion of TomoDB file: '" + remoteFileName
				+ "'.");
		
		// Retrieve TomoDB XML file and copy it locally
		// ---------------------------------------------------------------------
		try {
			copyFileFromTo(remoteFileName, tomoDB_XML);
		} catch (IOException e1) {
			logger.error("Impossible access file: '" + remoteFileName + "'. "
					+ e1.getMessage());
			Exception ex = new IOException("Impossible access file: '"
					+ remoteFileName + "'. " + e1.getMessage());
			throw (ex);
		}

		// Transform the TomoDB file
		// ---------------------------------------------------------------------
		TomoDBtoICAT.trasformTomo_in_ICAT(inXSL, tomoDB_XML, outTXT);

		proposalName = extractProposalFromPath(remoteFileName);

		map = getParametersFromXmlFile(outTXT);
		String beamline = map.get("instrument");
		String date = formatDate(map.get("date"), "yyyy-MM-dd'T'HH:mm:ss",
				"yyyy/MM/dd'T'HH:mm:ss");
		String dataSetName = map.get("dataset");

		// Check if Icat already contain the dataset????
		if (!IcatUtils.existDataset(dataSetName)) {

			// Collect info from SMIS and ingest them in Icat
			// ---------------------------------------------------------------------
			try {
				icatInvestigationID = SmisUtils.pushSmisMetadataToIcat(
						proposalName, beamline, date);
				logger.info("Imported investigation: <" + proposalName + ", "
						+ beamline + ", " + date + "> \n");
			} catch (Exception e) {
				Exception ex = new IOException(
						"Impossible push SMIS data into ICAT for <Proposal, beamline, date>: <"
								+ proposalName + ", " + beamline + ", " + date
								+ "> \n" + e.getMessage());
				throw (ex);
			}

			// Remove support variable from XML file and replace placeholder
			// ---------------------------------------------------------------------
			File xmlFile = new File(outTXT);
			replaceStringInFile(xmlFile, "placeholder", icatInvestigationID
					.toString());
			removeLineContainigStringFromFile(xmlFile, "<ExperimentName>");
			removeLineContainigStringFromFile(xmlFile, "<Instrument>");
			removeLineContainigStringFromFile(xmlFile, "<DataSetName>");

			// Do an 'ls -al *.edf' on the folder containing the TomoDB XML
			// file, format filename and sizes as ICAT Datafiles parameeters 
			// xml record and append them at the end of the XML file.
			// ---------------------------------------------------------------------
			readRemoteFolder_FormatXML_AppendToFile(remoteFileName.substring(0,
					remoteFileName.lastIndexOf(File.separatorChar)), xmlFile);

			// Import XML with Stefan client
			// ---------------------------------------------------------------------
			importXmlFile(outTXT);

		}
		logger
				.info("END ingestion of TomoDB file: '" + remoteFileName
						+ "'.\n");
		// }
	}

	// =======================================================================================================================
	// =======================================================================================================================
	/**
	 * From a specific path structure
	 * (\root_directory\<PROPOSALNAME>\<DATASETNAME>... extract the dataSet name
	 * 
	 * @param remoteFileName
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private static String extractDataSetFromPath(String remoteFileName)
			throws IOException {

		String datsetName = null;
		String headerPath = ROOT_DIRECTORY;

		if (remoteFileName.indexOf(headerPath) == 0) {

			int i = remoteFileName.indexOf(File.separatorChar, headerPath
					.length()) + 1;
			int j = remoteFileName.indexOf(File.separatorChar, i) + 1;
			i = remoteFileName.indexOf(File.separatorChar, j + 2);
			datsetName = remoteFileName.substring(j, i);

		} else {
			throw (new IOException("Unknown file path format. Expected: '"
					+ headerPath + "' Received: " + remoteFileName));
		}

		return datsetName;
	}

	// =======================================================================================================================
	// =======================================================================================================================

	/**
	 * From a specific path structure (\root_directory\<PROPOSALNAME>\...
	 * extract the proposal name
	 * 
	 * @param remoteFileName
	 * @return
	 * @throws IOException
	 */
	private static String extractProposalFromPath(String remoteFileName)
			throws IOException {

		String proposalName = null;
		String headerPath = ROOT_DIRECTORY;

		if (remoteFileName.indexOf(headerPath) == 0) {
			int proposalEndIndex = remoteFileName.indexOf(File.separatorChar,
					headerPath.length());
			int proposalBeginIndex = headerPath.length();
			proposalName = remoteFileName.substring(proposalBeginIndex,
					proposalEndIndex);

			// NOTE: it make sense only for "standard" proposals name.
			// Test Proposal Name format
			if (proposalName.substring(0, 2).matches("^[a-zA-Z]+$")) {
				try {
					Integer.parseInt(proposalName.substring(2));
				} catch (NumberFormatException e) {
					throw (new IOException(
							"Unexpected porosal Number. "
									+ "Proposal number must be an Integer. Expected: IS1234; Received: "
									+ proposalName));
				}
			} else {
				// contains other chars
				throw (new IOException(
						"Unexpected porosal Name."
								+ "Proposal type must contain only characters. Expected: IS1234;  Received: "
								+ proposalName));
			}

		} else {
			throw (new IOException("Unknown file path format. Expected: '"
					+ headerPath + "' Received: " + remoteFileName));
		}

		return proposalName;
	}

	// ==========================================================================
	// ==========================================================================

	/**
	 * Read from an xml file the content of several xml tags. Return: Map with
	 * key:value: (study, instrument, date)
	 * 
	 * @param fileName
	 * @return Map
	 * @throws Exception
	 */
	private static Map<String, String> getParametersFromXmlFile(String fileName)
			throws Exception {

		String expName = null;
		String instrument = null;
		String date = null;
		String dataSet = null;
		try {
			File xmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);

			((Document) doc).getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("ExperimentName");
			expName = nList.item(0).getFirstChild().getTextContent();

			nList = doc.getElementsByTagName("Instrument");
			instrument = nList.item(0).getFirstChild().getTextContent();

			nList = doc.getElementsByTagName("startDate");
			date = nList.item(0).getFirstChild().getTextContent();

			nList = doc.getElementsByTagName("DataSetName");
			dataSet = nList.item(0).getFirstChild().getTextContent();

			if (expName.isEmpty() || instrument.isEmpty() || date.isEmpty()) {
				throw (new Exception(
						"Missing information from XML file: Required StudyName; InstrumentName and Date."));
			}

		} catch (Exception e) {
			logger.error("Impossible retrieve information from Icat xml file. "
					+ e.getMessage());
			throw (e);
		}
		Map<String, String> mp = new HashMap<String, String>();
		mp.put("study", expName);
		mp.put("instrument", instrument);
		mp.put("date", date);
		mp.put("dataset", dataSet);

		return mp;
	}

	// =======================================================================================================================
	// =======================================================================================================================

	/**
	 * Search the FIRST occurrence of the specified String in a file, and delete
	 * the entire line.
	 * 
	 * @param xmlFile
	 * @param match
	 * @throws Exception
	 */
	public static void removeLineContainigStringFromFile(File xmlFile, String match)
			throws Exception {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(xmlFile, "rw");
		} catch (FileNotFoundException e) {
			logger.error("Impossible to open in R/W mode file: '"+xmlFile+"'. "+e.getMessage());
			throw(e);
		}
		String replacingString = " ";
		long pointer;
		try {
			pointer = raf.getFilePointer();
			String lineData = "";

			while ((lineData = raf.readLine()) != null) {
				pointer = raf.getFilePointer() - lineData.length() - 2;
				if (lineData.indexOf(match) >= 0) {
					raf.seek(pointer);

					raf.writeBytes(replacingString);

					// if the replacingString has less number of characters than
					// the matching string line then enter blank spaces.
					if (replacingString.length() < lineData.length()) {
						int difference = (lineData.length() - replacingString
								.length());// +1;
						for (int i = 0; i <= difference; i++) {
							raf.writeBytes(" ");
						}
					}
					raf.close();
					break;
				}
			}
		} catch (IOException e) {
			logger.error("Impossible access file: '"
					+ xmlFile + "'. " + e.getMessage());
			Exception ex = new IOException("Impossible access file: '"
					+ xmlFile + "'. " + e.getMessage());
			throw (ex);
		}
	}

	// --------------------------------------------------------------------
	/**
	 * Search for a specific xml tag in a file, and replace the existing value
	 * with a replace value. I case the length(newStr) < length(oldStr) fill the
	 * gap with 'blanks'. Is used to replace a "placeholder" with the real
	 * value.
	 * 
	 * @param xmlFile
	 * @param match
	 * @param replacingString
	 * @throws Exception
	 * @throws FileNotFoundException
	 */
	static void replaceStringInFile(File xmlFile, String match,
			String replacingString) throws Exception {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(xmlFile, "rw");
		} catch (FileNotFoundException e) {
			logger.error("Impossible to open in R/W mode file: '"+xmlFile+"'. "+e.getMessage());
			throw(e);
		}
		long pointer;
		try {
			pointer = raf.getFilePointer();

			String lineData = "";
			while ((lineData = raf.readLine()) != null) {
				pointer = raf.getFilePointer() - lineData.length() - 2;
				if (lineData.indexOf(match) >= 0) {
					raf.seek(pointer);
					String str = lineData;
					str = str.replace(match, replacingString);
					raf.writeBytes(str);
					// if the replacingString has less number of characters than
					// the matching string line then enter blank spaces.
					if (str.length() < lineData.length()) {
						int difference = (lineData.length() - str.length());// +1;
						for (int i = 0; i <= difference; i++) {
							raf.writeBytes(" ");
						}
					}
					raf.close();
					break;
				}
			}
		} catch (IOException e) {
			logger.error("Impossible access file: '"
					+ xmlFile + "'. " + e.getMessage());
			Exception ex = new IOException("Impossible access file: '"
					+ xmlFile + "'. " + e.getMessage());
			throw (ex);
		}
	}

	// --------------------------------------------------------------------
	/**
	 * Convert a string containing a date, from a specific format "fromFormat"
	 * in another format "toFormat".
	 * 
	 * @param date
	 * @param formFormat
	 * @param toFormat
	 * @return
	 */

	// From: "yyyy-MM-dd'T'HH:mm:ss" To: "yyyy/MM/dd'T'HH:mm:ss"
	// From: "dd/MM/yyyy HH:mm" To: "yyyy-MM-dd'T'HH:mm:ss"
	public static String formatDate(String date, String formFormat,
			String toFormat) {

		DateFormat sdf = new SimpleDateFormat(formFormat);
		sdf.setLenient(false);

		String s2 = null;
		Date d;
		try {
			d = sdf.parse(date);
			s2 = (new SimpleDateFormat(toFormat)).format(d);

		} catch (ParseException e) {

			e.printStackTrace();
		}
		return s2;
	}

	// --------------------------------------------------------------------
	/**
	 * Import a specific xml (must be in ICAT format) file in Icat
	 * 
	 * @param xmlFileName
	 * @throws Exception
	 */
	public static void importXmlFile(String xmlFileName) throws Exception {

		importXmlFile(xmlFileName, IcatSession.INGESTER_USER);

	}
	
	// --------------------------------------------------------------------
	/**
	 * Import a specific xml (must be in ICAT format) file in Icat
	 * 
	 * @param xmlFileName
	 * @throws Exception
	 */
	public static void importXmlFile(String xmlFileName, String user)
			throws Exception {

		String usr = null, psw = null;
		if (user.equals(IcatSession.ROOT_USER)) {
			usr = IcatSession.ROOT_USERNAME;
			psw = IcatSession.ROOT_PSW;
		} else if (user.equals(IcatSession.ADMIN_USER)) {
			usr = IcatSession.ADMIN_USERNAME;
			psw = IcatSession.ADMIN_PSW;
		} else if (user.equals(IcatSession.INGESTER_USER)) {
			usr = IcatSession.INGESTER_USERNAME;
			psw = IcatSession.INGESTER_PSW;
		}

		IcatXmlTestClient xmlIngesterclient = null;
		try {
			// Instanciate the client to import/extract date to/from Icat
			xmlIngesterclient = IcatXmlTestClient.createInstance(
					IcatSession.ICAT_HOSTNAME + "/icat",// /ICATService",
					usr, psw, IcatSession.ICAT_SECURITY_PLUGIN);

		} catch (Exception e) {
			Exception ex = new Exception(
					"Impossible instanciate IcatXmlTestClient. "
							+ e.getMessage());
			throw (ex);
		}
		try {
			// ingest xml file in ICAT
			xmlIngesterclient.read(xmlFileName);

		} catch (Exception e) {
			Exception ex = new Exception("Impossible import file: '"
					+ xmlFileName + "' in Icat. " + e.getMessage());
			throw (ex);
		}
	}

	// --------------------------------------------------------------------
	/**
	 * List all ".edf" files at the specified remoteDir and create for each file
	 * an XML datafile record. Append the generated xml to the end of the
	 * xmlFile.
	 * 
	 * @param remoteDir
	 * @param xmlFile
	 * @throws Exception
	 */
	public static void readRemoteFolder_FormatXML_AppendToFile(
			String remoteDir, File xmlFile) throws Exception {

		File dir = new File(remoteDir);
		File[] list = dir.listFiles();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.newDocument();

		try {
			// Root element, if remove the loop do not work
			Element e0 = doc.createElement("icatdata");
			doc.appendChild(e0);
			// - Ingest only 10 dataFiles ! !
			for (int i = 0; i < 10 && i < list.length; i++) {// <-- TEST ! !

				String filename = list[i].getName();
				long fileLenght = list[i].length();
				// Mon May 06 21:14:12 CES 2012
				Date time = new Date(list[i].lastModified());
				String fileDate = IcatPilotIngester.formatDate(time.toString(),
						"E MMM dd HH:mm:ss Z yyyy", "yyyy-MM-dd'T'HH:mm:ss");

				if (filename.contains(".edf")) { // "remove" header and trailer.

					Element dataFile = doc.createElement("datafile");

					Element element = doc.createElement("datafileCreateTime");
					element.setTextContent(fileDate);
					dataFile.appendChild(element);

					element = doc.createElement("fileSize");
					element.setTextContent(String.valueOf(fileLenght));
					dataFile.appendChild(element);

					element = doc.createElement("name");
					element.setTextContent(filename);
					dataFile.appendChild(element);

					element = doc.createElement("location");
					String pathLinux = remoteDir.replace("\\", "/");
					pathLinux = pathLinux.replaceAll("gy", "data").substring(1);
					element.setTextContent(pathLinux);
					dataFile.appendChild(element);

					element = doc.createElement("datafileFormat");
					String str = "<id>-3</id>"; // ID fix set in the XSLT file
					element.setTextContent(str);
					dataFile.appendChild(element);

					element = doc.createElement("dataset");
					// Use the XML importer internal ID of the dataset
					element.setTextContent("<id>-2</id>"); // ID fix set in the
					// XSLT file
					dataFile.appendChild(element);

					e0.appendChild(dataFile);
				}
			}

			String xmlStructure = printNode(doc);
			xmlStructure = xmlStructure.replaceFirst("<icatdata>", "");
			IcatPilotIngester.replaceStringInFile(xmlFile, "</icatdata>",
					xmlStructure);

		} catch (IOException e) {
			logger.error("Impossible append XML records to file: '" + xmlFile
					+ "'. " + e.getMessage());
			Exception ex = new IOException(
					"Impossible append XML records to file: '" + xmlFile
							+ "'. " + e.getMessage());
			throw (ex);
		}
	}

	// --------------------------------------------------------------------
	/**
	 * A DOM traversal program. Given a DOM Node, the one static method of this
	 * program prints the contents of the Node as XML.
	 */
	public static String printNode(Node node) {
		String xmlFormat = "";
		int type = node.getNodeType();
		switch (type) {
		// print the document element
		case Node.DOCUMENT_NODE: {
			xmlFormat = xmlFormat.concat(printNode(((Document) node)
					.getDocumentElement()));
			break;
		}

			// print element and any attributes
		case Node.ELEMENT_NODE: {
			xmlFormat = xmlFormat.concat("<");
			xmlFormat = xmlFormat.concat(node.getNodeName());

			if (node.hasAttributes()) {
				NamedNodeMap attrs = node.getAttributes();
				for (int i = 0; i < attrs.getLength(); i++)
					xmlFormat = xmlFormat.concat(printNode(attrs.item(i)));
			}
			xmlFormat = xmlFormat.concat(">");

			if (node.hasChildNodes()) {
				NodeList children = node.getChildNodes();
				for (int i = 0; i < children.getLength(); i++)
					xmlFormat = xmlFormat.concat(printNode(children.item(i)));
			}
			break;
		}

			// Print attribute nodes
		case Node.ATTRIBUTE_NODE: {
			xmlFormat = xmlFormat.concat(" " + node.getNodeName() + "=\"");
			if (node.hasChildNodes()) {
				NodeList children = node.getChildNodes();
				for (int i = 0; i < children.getLength(); i++)
					xmlFormat = xmlFormat.concat(printNode(children.item(i)));
			}
			xmlFormat = xmlFormat.concat("\"");
			break;
		}

			// handle entity reference nodes
		case Node.ENTITY_REFERENCE_NODE: {
			xmlFormat = xmlFormat.concat("&");
			xmlFormat = xmlFormat.concat(node.getNodeName());
			xmlFormat = xmlFormat.concat(";");
			break;
		}

			// print cdata sections
		case Node.CDATA_SECTION_NODE: {
			xmlFormat = xmlFormat.concat("<![CDATA[");
			xmlFormat = xmlFormat.concat(node.getNodeValue());
			xmlFormat = xmlFormat.concat("]]>");
			break;
		}

			// print text
		case Node.TEXT_NODE: {
			xmlFormat = xmlFormat.concat(node.getNodeValue());
			break;
		}

		case Node.COMMENT_NODE: {
			xmlFormat = xmlFormat.concat("<!--");
			xmlFormat = xmlFormat.concat(node.getNodeValue());
			xmlFormat = xmlFormat.concat("-->");
			break;
		}

			// print processing instruction
		case Node.PROCESSING_INSTRUCTION_NODE: {
			xmlFormat = xmlFormat.concat("<?");
			xmlFormat = xmlFormat.concat(node.getNodeName());
			String data = node.getNodeValue();
			{
				xmlFormat = xmlFormat.concat(" ");
				xmlFormat = xmlFormat.concat(data);
			}
			xmlFormat = xmlFormat.concat("?>");
			break;
		}
		}

		if (type == Node.ELEMENT_NODE) {
			xmlFormat = xmlFormat.concat("</");
			xmlFormat = xmlFormat.concat(node.getNodeName());
			xmlFormat = xmlFormat.concat(">");
		}
		return (xmlFormat);
	} // printNode(Node)

	
	// --------------------------------------------------------------------
	/**
	 * Copy a file from a location to another
	 * 
	 * @param remoteFileName
	 * @param localFileName
	 * @throws IOException
	 */
	public static void copyFileFromTo(String remoteFileName,
			String localFileName) throws IOException {

		File source = new File(remoteFileName);
		File target = new File(localFileName);

		InputStream in = new FileInputStream(source);
		OutputStream out = new FileOutputStream(target);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();

		logger.info("COPYED file '" + remoteFileName + "' to '" + localFileName
				+ "'");

	}
	
	/**
	 * 
	 * Before
	 * 	<test>blablabla</test>
	 * After
	 * 	<!--t>blablabla</te-->
	 * 
	 * @param xmlFile
	 * @param tag
	 * @throws Exception
	 */
	static void commentXmlNodeInFile(File xmlFile, String tag) throws Exception {
		String commentBegin = "!--"; // Characther '<' stays there
		String commentEnd = "-->";
		RandomAccessFile raf = new RandomAccessFile(xmlFile, "rw");
		long pointer;
		try {
			pointer = raf.getFilePointer();

			String lineData = "";
			while ((lineData = raf.readLine()) != null) {
				pointer = raf.getFilePointer() - lineData.length() - 2;
				if (lineData.indexOf(tag) >= 0) {
					if (tag.contains("/")){
						raf.seek(pointer);
						String str = lineData;
						str = str.replace(tag, commentEnd);
						raf.writeBytes(str);
						tag = tag.substring(1, tag.length()-2); // re-create original tag
						// if the replacingString has less number of characters than
						// the matching string line then enter blank spaces.
						if (str.length() < lineData.length()) {
							int difference = (lineData.length() - str.length());// +1;
							for (int i = 0; i <= difference; i++) {
								raf.writeBytes(" ");
							}
						}
						
					}else{
						raf.seek(pointer);
						String str = lineData;
						str = str.replace(tag, commentBegin);
						raf.writeBytes(str);
						tag = "/".concat(tag).concat(">"); // create closing tag
						// Now will search closing tag
					}
					
					
					// if the replacingString has less number of characters than
					// the matching string line then enter blank spaces.
					/*if (str.length() < lineData.length()) {
						int difference = (lineData.length() - str.length());// +1;
						for (int i = 0; i <= difference; i++) {
							raf.writeBytes(" ");
						}
					}*/
					
					
					
				}// else String not found
			}
			raf.close();
		} catch (IOException e) {
			logger.error("Impossible access file: '"
					+ xmlFile + "'. " + e.getMessage());
			Exception ex = new IOException("Impossible access file: '"
					+ xmlFile + "'. " + e.getMessage());
			throw (ex);
		}
	}
	
	

}