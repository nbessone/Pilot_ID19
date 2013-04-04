package fr.esrf.icat.ingesterPilot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
	static String proposalName;
	static Map<String, String> map = new HashMap<String, String>();
	static Long icatInvestigationID = (long) 0;
	private static final Logger logger = Logger
			.getLogger(IcatPilotIngester.class.getName());

	public static void main(String[] args) {
		String filepath = "\\\\gy\\visitor\\ma1145\\id19\\freeDM_A_\\freeDM_A_.xml";
		// \\gy\visitor\ma1145\id19\freeDM_A_\freeDM_A_.xml
		try {
			// run(filepath);
			testAllRun();
			// testRun();
		} catch (Exception e) {
			logger.error("Impossible ingest metadata relative to :'" + filepath
					+ "'. \n" + e.getMessage());
		}
	}

	// =======================================================================================================================
	// =======================================================================================================================
	public static void testRun() throws Exception {

		String rootDirectory = "\\\\gy\\visitor\\";

		// - get proposalName 'XXXXX'
		String proposalName = "ma1281";

		// - get folders (experiments) in \\gy\visitor\XXXXX\id19\
		String studyRootDir = rootDirectory + proposalName + "\\id19\\";

		File dir = new File(studyRootDir);
		File[] folderList = dir.listFiles();

		// - Loop over folders
		for (int i = 6; i < folderList.length; i++) {

			// - - Folder contains TomoDB XML file?
			String investigationDir = studyRootDir + folderList[i].getName()
					+ "\\";
			String fileName = folderList[i].getName() + ".xml";

			File f = new File(investigationDir + fileName);
			if (f.exists()) {
				System.out.println("File: " + investigationDir + fileName
						+ " EXISTS.");

				try {

					run(investigationDir + fileName);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				System.out.println("File: " + investigationDir
						+ " NO TmoDB file..");
			}

		}

	}

	/**
	 * Scan all folders in //gy/visitor/ find all containing a folder "id19" and
	 * search if it contain datasets with TomoDB xml file.
	 * 
	 * @throws Exception
	 */
	public static void testAllRun() throws Exception {

		// Search for all directory in \\gy\visitor\ containing ID19
		// experiments.
		// On each ID19 subFolder (dataSet) search if exist a TomoDB xml file.
		// if so, run the importation on that dataSet.
		Process p = null;
		String remoteDir = "\\\\gy\\visitor\\";
		String command = "cmd  /c dir " + remoteDir + " /AD /ON /B";

		try {
			p = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			logger.error("Impossible run command: '" + command
					+ "' at remote location: '" + remoteDir + "'. "
					+ e.getMessage());
		}

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		try {
			String s1;
			while ((s1 = stdInput.readLine()) != null) { // proposal level

				if (!s1.contains("in")) { // exclude all INDUSTRIAL proposals

					String path1 = remoteDir + s1;
					File dir = new File(path1);
					FilenameFilter textFilter = new FilenameFilter() {
						public boolean accept(File dir, String name) {
							String lowercaseName = name.toLowerCase();
							if (lowercaseName.startsWith("id19")
									|| lowercaseName.startsWith("id11")
									|| lowercaseName.startsWith("id22")
									//|| lowercaseName.startsWith("id17-")
									) {
								return true;
							} else {
								return false;
							}
						}
					};
					String[] list = dir.list(textFilter);

					for (int n = 0; n < list.length; n++) {
						String s2 = list[n];

						String path2 = path1 + File.separatorChar + s2;
						// System.out.println(path2);
						String command2 = "cmd  /c dir " + path2 + " /AD /B";
						Process p2 = null;

						p2 = Runtime.getRuntime().exec(command2);
						BufferedReader stdInput2 = new BufferedReader(
								new InputStreamReader(p2.getInputStream()));

						String s3 = null;
						int count = 0;
						// TEST - Ingest only 'X' dataset per investigation
						while ((s3 = stdInput2.readLine()) != null && count < 5) {// DataSet level

							String path3 = path2 + File.separatorChar + s3;
							String command3 = "cmd  /c dir " + path3
									+ " /A-D /B *.xml";

							Process p3 = Runtime.getRuntime().exec(command3);
							BufferedReader stdInput3 = new BufferedReader(
									new InputStreamReader(p3.getInputStream()));

							String s4 = null;
							while ((s4 = stdInput3.readLine()) != null
									&& count < 5) {

								if (s4.contains(s3 + ".xml")) {
									try {
										count++;

										run(path3 + File.separatorChar + s4);

									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
										logger.error(e.getMessage());
									}
								}

							}

							// }

						}
					}
				}
			}
			logger.info("FINISH - All ID19 proposal with investigations has been inported.... Maybe.....");
		} catch (IOException e1) { // TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	// =======================================================================================================================
	// =======================================================================================================================

	public static void run(String remoteFileName) throws Exception {

		logger.info("BEGIN ingestion of TomoDB file: '" + remoteFileName + "'.");

		String currentDir = System.getProperty("user.dir");

		String path = currentDir.concat(File.separatorChar + "resources"
				+ File.separatorChar);
		String inXML = path + "imput.xml"; // tomoDB_Catalogue.xml";
		String inXSL = path + "tomoDB_to_Icat.xsl";
		String outTXT = path + "tomoTest.xml";

		// Retrieve TomoDB XML file and copy it locally
		// ---------------------------------------------------------------------
		// TODO
		try {
			copyFileFromTo(remoteFileName, inXML);
		} catch (IOException e1) {
			logger.error("Impossible access file: '" + remoteFileName + "'. "
					+ e1.getMessage());
			Exception ex = new IOException("Impossible access file: '"
					+ remoteFileName + "'. " + e1.getMessage());
			throw (ex);
		}

		// Transform the TomoDB file
		// ---------------------------------------------------------------------
		TomoDBtoICAT.trasformTomo_in_ICAT(inXSL, inXML, outTXT);

		// Extract from the converted file StudyName InstrumentName and date
		// ---------------------------------------------------------------------
		proposalName = extractProposalFromPath(remoteFileName);
		String dataset = extractDataSetFromPath(remoteFileName);

		// If the DataSet already exist in Icat, return
		if (!IcatUtils.existDataset(dataset)) {

			map = getParametersFromXmlFile(outTXT);
			String beamline = map.get("instrument");
			String date = formatDate(map.get("date"), "yyyy-MM-dd'T'HH:mm:ss",
					"yyyy/MM/dd'T'HH:mm:ss");

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

			// Remove support variable and replace placeholder
			// ---------------------------------------------------------------------
			File xmlFile = new File(outTXT);
			replaceStringInFile(xmlFile, "placeholder",
					icatInvestigationID.toString());
			removeLineContainigStringFromFile(xmlFile, "<ExperimentName>");
			removeLineContainigStringFromFile(xmlFile, "<Instrument>");

			// Do an 'ls -al *.edf' on the folder containing the TomoDB XML file
			// and format the filename and sizes as Datafiles xml record
			// ---------------------------------------------------------------------
			readRemoteFolder_FormatXML_AppendToFile(
					remoteFileName.substring(0,
							remoteFileName.lastIndexOf(File.separatorChar)),
					xmlFile);

			// Import XML with Stefan client
			// ---------------------------------------------------------------------
			importXmlFile(outTXT);

			// Remember to Always LogOut from Icat
			// ---------------------------------------------------------------------
			// IcatSession.logOutIcat();
		}
		logger.info("END ingestion.\n");
	}

	// =======================================================================================================================
	// =======================================================================================================================
	/**
	 * From a specific path structure
	 * (\\gy\visitor\<PROPOSALNAME>\<DATASETNAME>... extract the dataSet name
	 * 
	 * @param remoteFileName
	 * @return
	 * @throws IOException
	 */
	private static String extractDataSetFromPath(String remoteFileName)
			throws IOException {
		// "\\gy\visitor\ma1145\id19\freeDM_A_\freeDM_A_.xml"
		
		String datsetName = null;
		String headerPath = File.separatorChar + "" + File.separatorChar + "gy"
				+ File.separatorChar + "visitor" + File.separatorChar; // "\\gy\visitor\"
		if (remoteFileName.indexOf(headerPath) == 0) {
			
			int i = remoteFileName.indexOf(File.separatorChar, headerPath.length() )+ 1;
			int j = remoteFileName.indexOf(File.separatorChar, i)+1;
			i = remoteFileName.indexOf(File.separatorChar, j+2);
			datsetName = remoteFileName.substring(j, i);
			
			
			/*String partialPath = File.separatorChar + "id19"
					+ File.separatorChar;
			datsetName = remoteFileName.substring(remoteFileName
					.indexOf(partialPath) + partialPath.length());
			datsetName = datsetName.substring(0,
					datsetName.indexOf(File.separatorChar));*/
		} else {
			throw (new IOException("Unknown file path format. Expected: '"
					+ headerPath + "' Received: " + remoteFileName));
		}

		return datsetName;
	}

	// =======================================================================================================================
	// =======================================================================================================================

	/**
	 * From a specific path structure (\\gy\visitor\<PROPOSALNAME>\... extract
	 * the proposal name
	 * 
	 * @param remoteFileName
	 * @return
	 * @throws IOException
	 */
	private static String extractProposalFromPath(String remoteFileName)
			throws IOException {

		String proposalName = null;
		String headerPath = File.separatorChar + "" + File.separatorChar + "gy"
				+ File.separatorChar + "visitor" + File.separatorChar; // "\\\\gy\\visitor\\"
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
	 * Read from an xml file the content of several xml tags.
	 * 
	 * @param fileName
	 * @return Map
	 * @throws Exception
	 */
	private static Map getParametersFromXmlFile(String fileName)
			throws Exception {

		String expName = null;
		String instrument = null;
		String date = null;
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

		return mp;
	}

	// =======================================================================================================================
	// =======================================================================================================================

	/**
	 * Search the FIRST occurrence of the specified String in a file, and
	 * delete the entire line.
	 * 
	 * @param xmlFile
	 * @param match
	 * @throws Exception
	 */
	static void removeLineContainigStringFromFile(File xmlFile, String match)
			throws Exception {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(xmlFile, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		RandomAccessFile raf = new RandomAccessFile(xmlFile, "rw");
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

		IcatXmlTestClient xmlIngesterclient = null;
		try {
			xmlIngesterclient = IcatXmlTestClient.createInstance(IcatSession.ICAT_HOSTNAME+"/icat", 
					IcatSession.ICAT_USERNAME, IcatSession.ICAT_PSW,  IcatSession.ICAT_SECURITY_PLUGIN);


		} catch (Exception e) {
			Exception ex = new Exception(
					"Impossible instanciate IcatXmlTestClient. "
							+ e.getMessage());
			throw (ex);
		}
		try {
			xmlIngesterclient.read(xmlFileName);

		} catch (Exception e) {
			Exception ex = new Exception("Impossible import file: '"
					+ xmlFileName + "' in Icat. " + e.getMessage());
			throw (ex);
		}
	}

	// --------------------------------------------------------------------
	/**
	 * list all ".edf" files at the specified remoteDir and create for each file
	 * an XML datafile record. Append the generated xml to the end of the
	 * xmlFile.
	 * 
	 * @param remoteDir
	 * @param xmlFile
	 * @throws Exception
	 */
	public static void readRemoteFolder_FormatXML_AppendToFile(String remoteDir, File xmlFile)
			throws Exception {

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
			for (int i = 0; i < 10 && i <list.length; i++) {// <-- TEST ! !
											
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
					pathLinux= pathLinux.replaceAll("gy", "data").substring(1);
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

}