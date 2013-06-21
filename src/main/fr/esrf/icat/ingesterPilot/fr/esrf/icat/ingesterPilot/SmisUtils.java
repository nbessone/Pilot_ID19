package fr.esrf.icat.ingesterPilot;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import jmis1.jboss_net.services.SMISWebService.SMISWebServiceSoapBindingStub;

import org.apache.log4j.Logger;
import org.icatproject.Facility;
import org.icatproject.FacilityCycle;
import org.icatproject.Group;
import org.icatproject.ICAT;
import org.icatproject.IcatException_Exception;
import org.icatproject.Instrument;
import org.icatproject.Investigation;
import org.icatproject.InvestigationUser;
import org.icatproject.Study;
import org.icatproject.StudyInvestigation;
import org.icatproject.StudyStatus;
import org.icatproject.User;
import org.icatproject.UserGroup;

import fr.esrf.smis.SMISServerService.ExpSessionInfoLightVO;
import fr.esrf.smis.SMISServerService.ProposalParticipantInfoLightVO;
import fr.esrf.utils.bean.MISBeanUtils;

public class SmisUtils {

	
	//public static final String[] PROPOSALS = new String[] { "CH", "ES", "EV",
	//		"HC", "HG", "LS", "MA", "ME", "MI", "MX", "SC", "MD" };
	//public static final Set<String> PROPOSAL_SET = new HashSet<String>(Arrays
	//		.asList(PROPOSALS));
	
	private static final Logger logger = Logger
			.getLogger(IcatPilotIngester.class.getName());
	
	
	/**
	 * 
	 * 
	 * @param proposalName
	 * @param beamline
	 * @param date
	 * @return
	 * @throws Exception
	 */
	static Long pushSmisMetadataToIcat(String proposalName, String beamline,
			String date) throws Exception {

		String sessionId = null;
		ICAT icat = null;
		Long invId = null;

		// Split the Proposal name in ProposalCode and ProposalNumber
		String proposalCode = extractProposalCode(proposalName);
		Integer proposalNumber = extractProposalNumber(proposalName);
		// To remove eventual white spaces, or "/" or "-"
		proposalName = proposalCode + proposalNumber.toString();

		SMISWebServiceSoapBindingStub stub = SmisSession.getStub();

		// get Smis Proposal Id (Icat Study)
		long proposalPk = 0;
		try {
			proposalPk = stub.getProposalPK(proposalCode, proposalNumber);
		} catch (org.jboss.axis.AxisFault e) {
			logger.error("Impossible retrieve proposal <" + proposalCode + ", "
					+ proposalNumber + "> from SMIS." + e.getMessage());
			throw e;
		}
		try {
			// If does not exist it rise an exceprion.
			stub.findSessionByProposalBeamlineAndDate(proposalCode,
					proposalNumber, beamline, toCalendar(date));
		} catch (org.jboss.axis.AxisFault e) {
			logger.error("Impossible retrieve Session <" + proposalCode + ", "
					+ proposalNumber + "> from SMIS." + e.getMessage());
			throw e;
		}

		if (proposalPk > 0) {// Proposal exist in SMIS?
			logger.info("Proposal '" + proposalName + "' exists in SMIS. ");

			// Connect to ICAT
			// -----------------------------------------------------------
			if (icat == null || sessionId == null) {
				try {

					icat = IcatSession.getIcat();
					sessionId = IcatSession.getSession();
				} catch (IcatException_Exception e) {
					logger.error("Impossible connnect to Icat: "
							+ e.getMessage());
					throw e;
				}
			}

			if (!IcatUtils.existStudy(proposalName)) {
				logger.info("Study '" + proposalName
						+ "' do NOT exists in ICAT: Creating.... ");

				// if Proposal do NOT exist in ICAT --- create it.
				ProposalParticipantInfoLightVO[] proposers = stub
						.findMainProposersForProposal(proposalPk);

				// N.B. Should NOT exist multiple main proposers!
				ProposalParticipantInfoLightVO mainProposer = proposers[0];
				// System.out.println(MISBeanUtils.beanToString(mainProposer));
				// // DEBUG

				// Crate proposal GROUP in Icat
				// -----------------------------------------------------------
				Group group = new Group();
				group.setName(proposalName);
				group.setCreateTime(toXmlGregorian(getCurrentTimeCal()));
				long groupId = icat.create(sessionId, group);
				group.setId(groupId);

				logger.info("Created GROUP.. "
						+ MISBeanUtils.beanToString(group));

				// Create proposal USER in Icat
				// -----------------------------------------------------------
				createIcatUser(icat, sessionId, group, IcatSession.ICAT_SECURITY_PLUGIN+proposalName,
						proposalName);
				/* 
				 * This kind of user is the only both present in LDAP and SMIS
				 * Since the authentication plugin in use is LDAP we have to prefix
				 * the username with ICAT_SECURITY_PLUGIN
				 */

				// Create main proposal USER in Icat
				// -----------------------------------------------------------
				createIcatUser(icat, sessionId, group, mainProposer
						.getScientistName(), mainProposer
						.getScientistFirstName());

				// Create STUDY in Icat
				// -----------------------------------------------------------
				Study study = new Study();
				study.setName(proposalName);

				// study.setStartDate(value) //missing
				study.setStatus(StudyStatus.IN_PROGRESS); // how to set it to
				// FINISH?

				study.setCreateTime(toXmlGregorian(getCurrentTime())); // should
				// be
				// automatic
				study.setDescription(mainProposer.getProposalTitle());

				User mainUser = IcatUtils.getUser(mainProposer
						.getScientistFirstName(), mainProposer
						.getScientistName());

				study.setUser(mainUser);// Main Study User

				Long studyId = icat.create(sessionId, study);
				study.setId(studyId);

				logger.info("Created STUDY.. "
						+ MISBeanUtils.beanToString(study));

			}// --Created new STUDY in Icat--
			else {
				logger.info("Study '" + proposalCode
						+ "' already exists in ICAT. ");
			}

			// Experiment exist in SMIS?
			// -----------------------------------------------------------
			Calendar cal = toCalendar(date);

			ExpSessionInfoLightVO expSession = null;
			try {
				expSession = stub.findSessionByProposalBeamlineAndDate(
						proposalCode, proposalNumber, beamline, cal);
				// expSession = expSessions[0];
			} catch (Exception e) {

				Exception ex = new IOException(
						"Impossible push SMIS data into ICAT for <Proposal, beamline, date>: <"
								+ proposalName + ", " + beamline + ", " + date
								+ "> \n" + e.getMessage());
				throw (ex);
				// e.printStackTrace();
			}

			// System.out.println(MISBeanUtils.beanToString(expSession)); //
			// DEBUG

			if (expSession != null) { // Investigation exist in SMIS?
				// logger.info("Investigation <"+ proposalName + ", " + beamline
				// + ", " + date+ "> exists in SMIS. ");

				String invName = formatInvestigationName(expSession.getName(),
						proposalCode, proposalNumber, beamline);

				// Investigation exist in Icat?
				// -----------------------------------------------------------
				Investigation tmp = IcatUtils.getInvestigation(invName);
				if (tmp == null) {
					// logger.info("Investigation '"+ invName+
					// "' do NOT exists in ICAT: Creating.... ");

					ProposalParticipantInfoLightVO[] users = stub
							.findUsersByExpSession(expSession.getPk());
					for (ProposalParticipantInfoLightVO usr : users) {

						// Create investigation USER in Icat
						// -----------------------------------------------------------
						createIcatUser(icat, sessionId, proposalName, usr
								.getScientistName(), usr
								.getScientistFirstName());
					}

					expSession.getRunCode();
					FacilityCycle fc = IcatUtils.getFacilityCycle(expSession
							.getRunCode());
					if (fc == null) {
						// Create FacilityCycle in ICAT (new Run is needed)
						// -----------------------------------------------------------
						fc = new FacilityCycle();
						fc.setName(expSession.getRunCode());

						fc.setStartDate(toXmlGregorian(expSession
								.getRunEndDate()));
						fc.setEndDate(toXmlGregorian(expSession
								.getRunStartDate()));

						// fc.setDescription();
						Facility f = IcatUtils.getFacility("ESRF");
						fc.setFacility(f);

						Long facId = icat.create(sessionId, fc);
						fc.setId(facId);

						logger.info("Created FACILITYCYCLE.. "
								+ MISBeanUtils.beanToString(fc));
					}

					// Create INVESTIGATION in Icat
					// -----------------------------------------------------------
					Investigation inv = new Investigation();
					inv.setName(invName);
					inv.setSummary(expSession.getComment());
					inv.setTitle(invName);

					Instrument ins = IcatUtils.getInstrument(expSession
							.getBeamlineName());
					inv.setInstrument(ins);

					inv.setFacility(IcatUtils.getFacility("ESRF"));
					inv.setFacilityCycle(IcatUtils.getFacilityCycle(expSession
							.getRunCode()));
					inv.setType(IcatUtils.getInvestigationType(proposalCode));

					inv.setStartDate(toXmlGregorian(expSession.getStartDate()));
					inv.setEndDate(toXmlGregorian(expSession.getEndDate()));

					invId = icat.create(sessionId, inv);
					inv.setId(invId);

					logger.info("Created INVESTIGATION.. "
							+ MISBeanUtils.beanToString(inv));

					// Create STUDYINVESTIGATION in Icat
					// -----------------------------------------------------------
					StudyInvestigation stInv = new StudyInvestigation();
					stInv.setStudy(IcatUtils.getStudy(proposalName));
					stInv.setInvestigation(inv);

					Long stInvId = icat.create(sessionId, stInv);
					stInv.setId(stInvId);

					logger.info("Created STUDYINVESTIGATION.. "
							+ MISBeanUtils.beanToString(stInv));

					ProposalParticipantInfoLightVO[] sessionUsers = stub
							.findUsersByExpSession(expSession.getPk());
					for (ProposalParticipantInfoLightVO usr : sessionUsers) {
						// System.out.println(MISBeanUtils.beanToString(usr));
						// // DEBUG

						// Create USER in Icat
						// -----------------------------------------------------------
						User icatUser = null;
						if (!IcatUtils.userExistInICAT(usr
								.getScientistFirstName(), usr
								.getScientistName())) {

							icatUser = createIcatUser(icat, sessionId,
									proposalName, usr.getScientistName(), usr
											.getScientistFirstName());

						}
						InvestigationUser invUser = new InvestigationUser();

						if (icatUser == null) {
							icatUser = IcatUtils.getUser(usr
									.getScientistFirstName(), usr
									.getScientistName());
						}
						if (inv.getId() == null) {
							inv.setId(IcatUtils.getInvestigation(inv.getName())
									.getId());
						}

						// Create INVESTIGATIONUSER in Icat
						// -----------------------------------------------------------
						if (!IcatUtils.existInvestigationUser(icatUser.getId(),
								inv.getId())) {

							invUser.setUser(icatUser);
							// invUser.setRole(value) // Missing
							invUser.setInvestigation(inv);
							invUser.setRole("Scientist"); // 4.2.2 has a bug ->
							// need this value!!
							// icatObjects.add(invUser);
							Long invUserID = icat.create(sessionId, invUser);
							invUser.setId(invUserID);

							logger.info("Created INVESTIGATIONUSER.. "
									+ MISBeanUtils.beanToString(invUser));
						}
					}

				} else {// if Investigation already exist in Icat
					logger.info("Investigation '" + invName
							+ "' already exists in ICAT. ");
					invId = tmp.getId();
				}

			}else{
				//remove Investigation parameters from xml file
			}

			// SAMPLE
			/*
			 * SampleSheetInfoLightVO[] sampleSheet = stub
			 * .findSamplesheetInfoLightForProposalPk(proposalPk); System.out
			 * .println
			 * ("----------------------------------------------------------------"
			 * );
			 * System.out.println("findSamplesheetInfoLightForProposalPk count: "
			 * + sampleSheet.length); if(sampleSheet.length>0){
			 * 
			 * // Enter all study Samples for (SampleSheetInfoLightVO vo :
			 * sampleSheet) { System.out.println(MISBeanUtils.beanToString(vo));
			 * 
			 * TypeDesc tmp = vo.getTypeDesc(); FieldDesc[] tmp2 =
			 * tmp.getFields(); for(FieldDesc t: tmp2){
			 * System.out.println("-- "+
			 * t.getFieldName()+" : "+MISBeanUtils.beanToString
			 * (tmp.getFieldByName(t.getFieldName())) ); } } }
			 */

		} else {
			logger.info("Proposal '" + proposalCode
					+ "' do NOT exists in SMIS. ");
		}

		return invId;

	}

	
	/**
	 * Extract from SMIS all experiments performed in the specified time frame.
	 * Identify if the relative TomoDB exist. Return list of TomoDB fileNames.
	 * 
	 * @param instrument
	 * @param from
	 * @param to
	 * @throws Exception 
	 */
	public static List<String> getInvestigationsByDates(Calendar from,
			Calendar to) throws Exception {

		List<String> TomoDBFileList = new ArrayList<String>();
		SMISWebServiceSoapBindingStub stub = null;
		String path = IcatPilotIngester.ROOT_DIRECTORY;

		try {
			stub = SmisSession.getStub();
		} catch (Exception e) {
			logger.error("Error in the SMIS connection. " + e.getMessage());
			throw e;
		}

		ExpSessionInfoLightVO[] resultList = null;

		// loop over various Instruments
		for (String instrument : IcatPilotIngester.instruments) {

			SimpleDateFormat df = new SimpleDateFormat();
			df.applyPattern("dd/MM/yyyy");
			System.out.println("Retrieving experiments performed on '"
					+ instrument + "' in the period from "
					+ df.format(from.getTime()) + " to "
					+ df.format(to.getTime()) + " ... Can take a while.");

			try {
				resultList = stub.findSessionsByBeamlineAndDates(instrument,
						from, to);
			} catch (RemoteException e) {
				logger.error("Error in retrievibg information from SMIS:"
						+ " (findSessionsByBeamlineAndDates). "
						+ e.getMessage());
			}

			// System.out.println("Trovati  " + resultList.length
			// + "  TomoDB file in Specified time frame");

			for (ExpSessionInfoLightVO res : resultList) {

				// exclude all INDUSTRIAL proposals
				if (!res.getCategCode().toLowerCase().equals("in")) {

					path = IcatPilotIngester.ROOT_DIRECTORY.concat(res.getCategCode().toLowerCase()
							+ res.getCategCounter() + File.separatorChar
							+ instrument);

					File file = new File(path); // === List DataSets
															// ===
					String[] directory = file.list(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return new File(dir, name).exists();
						}
					});

					// if (directory != null)
					// System.out.println("Checking: " + rootDirectory +
					// " contain: "
					// + directory.length + " directories."); // DEBUG

					// Loop over all DataSets
					for (int n = 0; directory != null && n < directory.length; n++) {

						final String s2 = directory[n];

						String tomoDB_filePath = path
								+ File.separatorChar + s2; // DataSet directory

						// === Search for TomoDB_fileName ===
						File file3 = new File(tomoDB_filePath);

						FilenameFilter filter = new FilenameFilter() {
							// select only file with a specific name
							public boolean accept(File dir, String name) {
								if (name.contains(s2 + ".xml")) {
									return true;
								} else {
									return false;
								}
							}
						};

						File[] directories3 = file3.listFiles(filter);

						if (directories3 != null && directories3.length > 0) {

							// FOUND TomoDB file
							String tomoDBFileName = tomoDB_filePath
									+ File.separatorChar + s2 + ".xml";
							TomoDBFileList.add(tomoDBFileName);
							// System.out.println(tomoDBFileName);

						}
					}
				}
			}
		}
		return TomoDBFileList;

	}

	// ===========================================================================================
	// Utility functions
	// ===========================================================================================

	/**
	 * 
	 * @param icat
	 * @param sessionId
	 * @param groupName
	 * @param scientistName
	 * @param scientistFirstName
	 * @return
	 * @throws IcatException_Exception
	 * @throws IOException
	 */
	private static User createIcatUser(ICAT icat, String sessionId,
			String groupName, String scientistName, String scientistFirstName)
			throws IcatException_Exception, IOException {

		Group group = IcatUtils.getGroup(groupName);
		return createIcatUser(icat, sessionId, group, scientistName,
				scientistFirstName);

	}

	/**
	 * NOTE:
	 * The username MUST be prefix with the authenticaton method used: 'db/'or 'ldap/'
	 * or it won't match the information coming from the Icat authentication plugin.
	 * 
	 * @param icat
	 * @param sessionId
	 * @param group
	 * @param scientistName
	 * @param scientistFirstName
	 * @throws IcatException_Exception
	 * @throws IOException
	 */
	private static User createIcatUser(ICAT icat, String sessionId,
			Group group, String scientistName, String scientistFirstName)
			throws IcatException_Exception, IOException {

		// Create main proposal USER in Icat
		// -----------------------------------------------------------
		User icatUser = null;
		if (!IcatUtils.userExistInICAT(scientistFirstName, scientistName)) {
			icatUser = new User();
			icatUser.setName(scientistName.toLowerCase());
			icatUser.setFullName(scientistName.toUpperCase() + " "
					+ scientistFirstName);
			Long userId = icat.create(sessionId, icatUser);
			icatUser.setId(userId);
			logger
					.info("Created USER.. "
							+ MISBeanUtils.beanToString(icatUser));
		}

		// Crate proposal USERGROUP in Icat
		// -----------------------------------------------------------
		if (!IcatUtils.existUserGroup(scientistName, group.getName())) {
			UserGroup userGroup = new UserGroup();
			userGroup.setGroup(group);
			if (icatUser == null) {
				icatUser = IcatUtils.getUser(scientistFirstName, scientistName);
			}
			userGroup.setUser(icatUser);
			long userGroupId = icat.create(sessionId, userGroup);
			userGroup.setId(userGroupId);

			logger.info("Created USERGROUP.. "
					+ MISBeanUtils.beanToString(userGroup));
		}
		return icatUser;
	}

	/**
	 * Format the Investigation name.
	 * 
	 * @param proposalCode
	 * @param proposalNumber
	 * @param beamline
	 * @return
	 */
	public static String formatInvestigationName(String proposalName,
			String proposalCode, Integer proposalNumber, String beamline) {

		String dates = proposalName.substring(
				proposalName.trim().lastIndexOf(" ")).trim();

		String prefix = proposalCode + proposalNumber.toString() + " "
				+ beamline + " " + dates;

		return prefix;
	}

	/**
	 * Extract the Proposal number from the proposal name and check if it is a
	 * number. Remove eventual leading "/" or "-" characters.
	 * 
	 * @param proposalName2
	 * @return
	 */
	private static Integer extractProposalNumber(String proposalName) {

		String tmp = proposalName.substring(2).trim();

		if (((String) tmp).contains("/") || ((String) tmp).contains("-")) {
			tmp = tmp.substring(1);
		}
		return Integer.parseInt(tmp);
	}

	/**
	 * Extract the Proposal code from the proposal name and check if exist in
	 * the list of allowed values.
	 * 
	 * @param proposalName
	 * @return
	 * @throws Exception
	 */
	private static String extractProposalCode(String proposalName)
			throws Exception {
		// should probably check if the code exist in the Icat INVESTIGATIONTYPE
		// table
		String code = proposalName.substring(0, 2).trim().toUpperCase();
		/*
		 * if(!PROPOSAL_SET.contains(code)) throw new
		 * Exception("Proposal code not recognized. Received: '"+
		 * code+"'. Expected: "+PROPOSAL_SET);
		 */
		return code;
	}

	/**
	 * 
	 * @param cal
	 * @return
	 */
	private static XMLGregorianCalendar toXmlGregorian(Calendar cal) {

		DatatypeFactory dtf = null;
		try {
			dtf = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XMLGregorianCalendar xgc = dtf.newXMLGregorianCalendar();

		xgc.setYear(cal.get(Calendar.YEAR));
		xgc.setDay(cal.get(Calendar.DAY_OF_MONTH));
		xgc.setMonth(cal.get(Calendar.MONTH) + 1);
		xgc.setHour(cal.get(Calendar.HOUR_OF_DAY));
		xgc.setMinute(cal.get(Calendar.MINUTE));
		xgc.setSecond(cal.get(Calendar.SECOND));
		xgc.setMillisecond(cal.get(Calendar.MILLISECOND));
		// Calendar ZONE_OFFSET and DST_OFFSET fields are in milliseconds.
		int offsetInMinutes = (cal.get(Calendar.ZONE_OFFSET) + cal
				.get(Calendar.DST_OFFSET))
				/ (60 * 1000);
		xgc.setTimezone(offsetInMinutes);

		return xgc;
	}
	

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static XMLGregorianCalendar toXmlGregorian(String str) {
		return toXmlGregorian(toCalendar(str));
	}

	/**
	 * From a string of the form "2013/02/07T17:44:10" create a Calendar object.
	 * 
	 * @param val
	 * @return
	 */
	private static Calendar toCalendar(String val) {
		// 2012-11-19T23:38:12
		final SimpleDateFormat format = new SimpleDateFormat(
				"yyyy/MM/dd'T'HH:mm:ss");
		Date dDate = null;

		try {
			dDate = format.parse(val);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(dDate);

		return cal;
	}

	/**
	 * 
	 * @return
	 */
	private static String getCurrentTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());
	}

	private static Calendar getCurrentTimeCal() {
		return Calendar.getInstance();
	}
}
