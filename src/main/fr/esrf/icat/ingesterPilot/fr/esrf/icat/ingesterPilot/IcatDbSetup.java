package fr.esrf.icat.ingesterPilot;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.icatproject.Facility;
import org.icatproject.Group;
import org.icatproject.ICAT;
import org.icatproject.IcatException_Exception;
import org.icatproject.Instrument;
import org.icatproject.InvestigationType;
import org.icatproject.Rule;
import org.icatproject.User;
import org.icatproject.UserGroup;

public class IcatDbSetup {

	private static ICAT icat = null;
	private static String sessionId = null;
	private static final Logger logger = Logger.getLogger(IcatPilotIngester.class.getName());
	private static final String FACILITY_ADMIN_GROUP = "FacilityAdmins";
	private static final String DATA_INGESTOR_GROUP = "DataIngestors";
	
	/**
	 * Re initialize the local ICAT DB
	 * @throws Exception 
	 */
	public static void SetupDB() throws Exception{
		DeleteAllDB();
		SetupEsrfSpecificData();
		SetupBeamlineSpecificData();
	}

	/**
	 * Delete ALL entries existing in the ICAT DB
	 * @throws Exception 
	 */
	public static void DeleteAllDB() throws Exception{
		
		String currentDir = System.getProperty("user.dir");
		// Clean ALL data in the DB and create ROOT user and group
		String filename = currentDir+File.separatorChar+"resources"+File.separatorChar+"clean-all.xml";
		// to Delete needs to be ROOT.
		try {
			IcatPilotIngester.importXmlFile(filename,IcatSession.ROOT_USER);
		} catch (Exception e) {
			logger.error("Unable to import XML file: '"+filename+"' to remove ALL data from the ICAT DB.");
			throw e;
		}
	}
	
	/**
	 * Enter in ICAT general data about the ESRF installation:
	 * Facility
	 * Instruments
	 * InvestigationTypes
	 * Group (admin, ingester, etc...)
	 * User (admin, ingester, etc...) 
	 * Rules...
	 * @throws IcatException_Exception 
	 */
	public static void SetupEsrfSpecificData() throws IcatException_Exception{
		
		
		try {
			IcatSession.connect(IcatSession.ROOT_USER);
			icat = IcatSession.getIcat();
			sessionId = IcatSession.getSession();
			
		}catch (MalformedURLException e) {
			logger.error("Impossible connnect to Icat: " + e.getMessage());
			//throw e;
		}catch (IcatException_Exception e) {
			logger.error("Impossible connnect to Icat: " + e.getMessage());
			//throw e;
		}
		
		//-------------------------------------------------------------------------
		// Login as ROOT to first create the FacilityAdmins GROUP/User then to use
		
		
		//Give basic access rules to ROOT
        Group rootGroup = (Group)icat.search(sessionId, "Group[name='AdminGrp']" ).get(0);
        List<String> rootTables = new ArrayList<String>();
        rootTables.add("Application");
      	rootTables.add("Datafile");
      	rootTables.add("DatafileFormat");
      	rootTables.add("DatafileParameter");
      	rootTables.add("Dataset");
      	rootTables.add("DatasetParameter");
      	rootTables.add("DatasetType");
      	rootTables.add("Facility");
      	rootTables.add("FacilityCycle");
        rootTables.add("Group");
        rootTables.add("InputDatafile");
      	rootTables.add("InputDataset");
      	rootTables.add("Instrument");
      	rootTables.add("InstrumentScientist");
      	rootTables.add("Investigation");
      	rootTables.add("InvestigationParameter");
      	rootTables.add("InvestigationType");
      	rootTables.add("InvestigationUser");
      	rootTables.add("Job");
      	rootTables.add("Keyword");
      	rootTables.add("NotificationRequest");
      	rootTables.add("OutputDatafile");
      	rootTables.add("OutputDataset");
      	rootTables.add("ParameterType");
      	rootTables.add("PermissibleStringValue");
      	rootTables.add("Publication");
      	rootTables.add("RelatedDatafile");
      	rootTables.add("Rule");
      	rootTables.add("Sample");
      	rootTables.add("SampleParameter");
      	rootTables.add("SampleType");
      	rootTables.add("Shift");
      	rootTables.add("Study");
      	rootTables.add("StudyInvestigation");
        rootTables.add("User");
        rootTables.add("UserGroup");

        
        for(String what : rootTables)
        {
            Rule rule = new  Rule();
            rule.setGroup(rootGroup);
            rule.setCrudFlags("CRUD");
            rule.setWhat(what);
            icat.create(sessionId, rule);
            logger.info("Created Root user rule: '" + what +"'");
        }
		
		
		
		//-------------------------------------------------------------------------
		Group adminGroup = EnterGroup(FACILITY_ADMIN_GROUP);
		
		User ingesterUserAdm = EnterUser("db/admin","Admin");
		EnterUserGroup(ingesterUserAdm, adminGroup);
		
		//--------------------------------------------------------------------------------------------
		//Let the FacilityAdmins group do everything
        Group facilityAdmins = (Group)icat.search(sessionId, "Group[name='"+FACILITY_ADMIN_GROUP+"']" ).get(0);
        List<String> adminTables = new ArrayList<String>();
        adminTables.add("Application");
        adminTables.add("Datafile");
        adminTables.add("DatafileFormat");
        adminTables.add("DatafileParameter");
        adminTables.add("Dataset");
        adminTables.add("DatasetParameter");
        adminTables.add("DatasetType");
        adminTables.add("Facility");
        adminTables.add("FacilityCycle");
        adminTables.add("Group");
        adminTables.add("InputDatafile");
        adminTables.add("InputDataset");
        adminTables.add("Instrument");
        adminTables.add("InstrumentScientist");
        adminTables.add("Investigation");
        adminTables.add("InvestigationParameter");
        adminTables.add("InvestigationType");
        adminTables.add("InvestigationUser");
        adminTables.add("Job");
        adminTables.add("Keyword");
        adminTables.add("NotificationRequest");
        adminTables.add("OutputDatafile");
        adminTables.add("OutputDataset");
        adminTables.add("ParameterType");
        adminTables.add("PermissibleStringValue");
        adminTables.add("Publication");
        adminTables.add("RelatedDatafile");
        adminTables.add("Rule");
        adminTables.add("Sample");
        adminTables.add("SampleParameter");
        adminTables.add("SampleType");
        adminTables.add("Shift");
        adminTables.add("Study");
        adminTables.add("StudyInvestigation");
        adminTables.add("User");
        adminTables.add("UserGroup");

        
        for(String table : adminTables)
        {
            Rule rule = new  Rule();
            rule.setGroup(facilityAdmins);
            rule.setCrudFlags("CRUD");
            rule.setWhat(table);
            icat.create(sessionId, rule);
            logger.info("Created " + table + " admin rule");
        }
        
        IcatSession.logOutIcat();
        
			
		//-------------------------------------------------------------------------
        // Log-in as ADMINISTRATOR to enter ESRF specific data.
		//-------------------------------------------------------------------------
		
        try {
			IcatSession.connect(IcatSession.ADMIN_USER);
			icat = IcatSession.getIcat();
			sessionId = IcatSession.getSession();
			
		}catch (MalformedURLException e) {
			logger.error("Impossible connnect to Icat: " + e.getMessage());
			//throw e;
		}catch (IcatException_Exception e) {
			logger.error("Impossible connnect to Icat: " + e.getMessage());
			//throw e;
		}
        
        
		
		Facility facility =
		EnterFacility("ESRF", "European Synchrotron Radiation Facility", 
				"The ESRF is a joint research facility supported by 19 countries situated in Grenoble, France.");
		
		EnterInstrument("ID11","ID11 - Materials Science Beamline",
				"Materials Science Beamline","Beamline", facility);
		EnterInstrument("ID17","ID17 - Bio-medical Beamline",
				"Bio-medical Beamline","Beamline", facility);
		EnterInstrument("ID19","ID19 - High-resolution Diffraction Topography Beamline",
				"Topography - Microtomography beamline","Beamline", facility);
		EnterInstrument("ID21","ID21 - X-ray Microscopy Beamline",
				"ID21 houses two microscopes: a Scanning X-ray Microscope and an infra-red microscope","Beamline", facility);
		EnterInstrument("ID22","ID22 - Micro-Fluorescence, Imaging and Diffraction",
				"ID22 is a versatile X-ray instrument in hard X-ray microscopy science.","Beamline", facility);
		// EnterInstrument("","","","Beamline", facility);
		
		
		EnterInvestigationType("MX", "Macromolecular christallography", facility);
		EnterInvestigationType("MA", "Applied material science", facility);
		EnterInvestigationType("CH", "Chemistry", facility);
		EnterInvestigationType("ES", "Earth Sciences", facility);
		EnterInvestigationType("EV", "Environment", facility);
		EnterInvestigationType("HC", "Hard Condensed Matter Science", facility);
		EnterInvestigationType("HG", "Cultural Heritage", facility);
		EnterInvestigationType("LS", "Life Sciences", facility);
		EnterInvestigationType("ME", "Engineering", facility);
		EnterInvestigationType("MI", "Method and Instrunebtation", facility);
		EnterInvestigationType("SC", "Soft Condensed Matter Science", facility);
		EnterInvestigationType("MD", "Medicine", facility);
		
		
		Group ingesterGroup = EnterGroup(DATA_INGESTOR_GROUP);
		
		User ingesterUser11 = EnterUser("db/ingester_id11","Ingester_ID11");
		EnterUserGroup(ingesterUser11, ingesterGroup);
		
		User ingesterUser19 = EnterUser("db/ingester_id19","Ingester_ID19");
		EnterUserGroup(ingesterUser19, ingesterGroup);
		
		User ingesterUser22 = EnterUser("db/ingester_id22","Ingester_ID22");
		EnterUserGroup(ingesterUser22, ingesterGroup);
		
		
		
		
		
		//=========================================================================================
		// RULEs
		//=========================================================================================
		
		
		//public read access to common tables 
        List<String> publicTables = new ArrayList<String>();
        publicTables.add("Application");
        publicTables.add("DatafileFormat");
        publicTables.add("DatasetType");
        publicTables.add("Facility");
        publicTables.add("FacilityCycle");
        publicTables.add("Instrument");
        publicTables.add("InstrumentScientist");
        publicTables.add("InvestigationType");
        publicTables.add("ParameterType");
        publicTables.add("PermissibleStringValue");
        publicTables.add("Publication");
        publicTables.add("Shift");

        for(String table : publicTables)
        {
            Rule rule = new  Rule();
            rule.setCrudFlags("R");
            rule.setWhat(table);
            rule.setGroup(null);
            icat.create(sessionId, rule);
        }
        
      //--------------------------------------------------------------------------------------------
      //Let the DataIngestors group do enough to ingest data/metadata
        Group dataIngestors = (Group)icat.search(sessionId, "Group[name='"+DATA_INGESTOR_GROUP+"']" ).get(0);
        List<String> ingestorTables = new ArrayList<String>();
        
        ingestorTables.add("Application");
        ingestorTables.add("Datafile");
        ingestorTables.add("DatafileFormat");
        ingestorTables.add("DatafileParameter");
        ingestorTables.add("Dataset");
        ingestorTables.add("DatasetParameter");
        ingestorTables.add("DatasetType");
        ingestorTables.add("FacilityCycle");
        ingestorTables.add("Group");
        ingestorTables.add("InputDatafile");
        ingestorTables.add("InputDataset");
        ingestorTables.add("Investigation");
        ingestorTables.add("InvestigationParameter");
        ingestorTables.add("InvestigationUser");
        ingestorTables.add("Job");
        ingestorTables.add("Keyword");
        ingestorTables.add("OutputDatafile");
        ingestorTables.add("OutputDataset");
        ingestorTables.add("ParameterType");
        ingestorTables.add("Publication");
        ingestorTables.add("RelatedDatafile");
        ingestorTables.add("Sample");
        ingestorTables.add("SampleParameter");
        ingestorTables.add("SampleType");
        ingestorTables.add("Study");
        ingestorTables.add("StudyInvestigation");
        ingestorTables.add("User");
        ingestorTables.add("UserGroup");
        

        for(String table : ingestorTables)
        {
            Rule rule = new  Rule();
            rule.setGroup(dataIngestors);
            rule.setCrudFlags("CRU"); //no delete permission for ingestors
            rule.setWhat(table);
            icat.create(sessionId, rule);
            logger.info("Created " + table + " ingestor rule");
        }

        
        //----------------------------------------------------------------------------------------------------------------------------
        //Investigatio-User rules 
        List<String> inv_userTable = new ArrayList<String>();
        
        inv_userTable.add(												 "Investigation <-> InvestigationUser <-> User [name = :user]");
        inv_userTable.add(					  "InvestigationParameter <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        inv_userTable.add(				 					 "Dataset <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        inv_userTable.add(			    "DatasetParameter <-> Dataset <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        inv_userTable.add(					    "Datafile <-> Dataset <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        inv_userTable.add("DatafileParameter <-> Datafile <-> Dataset <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        inv_userTable.add(									  "Sample <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        
        Rule rule = new  Rule();
        for(String what : inv_userTable)
        {
            rule.setGroup(null);
            rule.setCrudFlags("CRU"); //no delete permission for instrument scientist
            rule.setWhat(what);
            icat.create(sessionId, rule);
            System.out.println("Created Instrument Scientist rule: '" + what +"'");
        }
        
    /*    TODO
        
        //----------------------------------------------------------------------------------------------------------------------------
        //instrument scientist rules - these cause problems - see ICAT issue 83 https://code.google.com/p/icatproject/issues/detail?id=83 
        //investigation and investigation parameter
        List<String> scientistTables = new ArrayList<String>();
        
        scientistTables.add(												"Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(					 "InvestigationParameter <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(									"Dataset <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(			   "DatasetParameter <-> Dataset <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(					   "Datafile <-> Dataset <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add( "DatafileParameter <-> Datafile <-> Dataset <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(									 "Sample <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        
        Rule rule = new  Rule();
        for(String what : scientistTables)
        {
            rule.setGroup(null);
            rule.setCrudFlags("CRU"); //no delete permission for instrument scientist
            rule.setWhat(what);
            icat.create(sessionId, rule);
            System.out.println("Created Instrument Scientist rule: '" + what +"'");
        }
        
                
        
        //----------------------------------------------------------------------------------------------------------------------------
        //Principal Investigator - for now they can read and update everything they own - Investigation + P ; DS + P ; DF + P; Sample + P
        scientistTables.clear();
        
        scientistTables.add(											   "Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(					"InvestigationParameter <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(								   "Dataset <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(			  "DatasetParameter <-> Dataset <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(					  "Datafile <-> Dataset <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add("DatafileParameter <-> Datafile <-> Dataset <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(									"Sample <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(				"SampleParameter <-> Sample <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        
        for(String what : scientistTables)
        {
            rule.setGroup(null);
            rule.setCrudFlags("RU"); //ONLY Read/Update permission for Principal scientist
            rule.setWhat(what);
            icat.create(sessionId, rule);
            System.out.println("Created Principal Investigator rule: '" + what +"'");
        }
        
                
        
        //----------------------------------------------------------------------------------------------------------------------------
        //Co-Investigator - for now they can read everything they are associated with - Investigation + P ; DS + P ; DF + P; Sample + P
        scientistTables.clear();
        
        scientistTables.add(											   "Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add("					 InvestigationParameter <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(								   "Dataset <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(			  "DatasetParameter <-> Dataset <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(					  "Datafile <-> Dataset <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add("DatafileParameter <-> Datafile <-> Dataset <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(									"Sample <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(				"SampleParameter <-> Sample <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
                
        for(String what : scientistTables)
        {
            rule.setGroup(null);
            rule.setCrudFlags("R"); //ONLY Read permission for Co-Investigator 
            rule.setWhat(what);
            icat.create(sessionId, rule);
            System.out.println("Created Co-Investigator rule: '" + what +"'");
        }  
        
        */
        
		
      //=========================================================================================
      //=========================================================================================
		
		
	}
	
	//----------------------------------------------------------------------------------------------
	/**
	 * Create a new facility
	 */
	private static Facility EnterFacility(String name, String fullName, String description) 
	throws IcatException_Exception{
		Facility facility = new Facility();
		facility.setName(name);
		facility.setFullName(fullName);
		facility.setDescription(description);
		try {
			
			facility.setId(icat.create(sessionId, facility));
			
		} catch (IcatException_Exception e) {
			logger.error("Impossible create Facility: '"+name+ "'. " + e.getMessage());
			throw e;
		}
		return facility;
	}
	
	/**
	 * Create and add to ICAT a new instrument 
	 * @param name
	 * @param fullName
	 * @param description
	 * @param type
	 * @param facility
	 * @throws IcatException_Exception
	 */
	private static void EnterInstrument(String name, String fullName, String description, String type, Facility facility) 
	throws IcatException_Exception {
		Instrument instrument = new Instrument();
		instrument.setName(name);
		instrument.setFullName(fullName);
		instrument.setDescription(description);
		instrument.setType(type);
		instrument.setFacility(facility);
		try {
			
			icat.create(sessionId, instrument);
			
		} catch (IcatException_Exception e) {
			logger.error("Impossible create Instrument: '"+name+ "'. " + e.getMessage());
			throw e;
		}
	}
	/**
	 * Create and add to ICAT a new InvestigationType
	 * @param name
	 * @param description
	 * @param facility
	 * @throws IcatException_Exception
	 */
	private static void EnterInvestigationType(String name, String description, Facility facility) 
	throws IcatException_Exception {
		InvestigationType investigationType = new InvestigationType();
		investigationType.setName(name);
		investigationType.setDescription(description);
		investigationType.setFacility(facility);
		try {
			
			icat.create(sessionId, investigationType);
			
		} catch (IcatException_Exception e) {
			logger.error("Impossible create InvestigationType: '"+investigationType+ "'. " + e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Create and add to ICAT a new Group
	 * @param name
	 * @return
	 * @throws IcatException_Exception
	 */
	private static Group EnterGroup(String name) 
	throws IcatException_Exception{
		Group group = new Group();
		group.setName(name);

		try {
			
			group.setId( icat.create(sessionId, group) );
			System.out.println("Created  Group: '"+name + "'.");
			
		} catch (IcatException_Exception e) {
			logger.error("Impossible create Group: '"+group+ "'. " + e.getMessage());
			throw e;
		}
		return group;
	}
	
	/**
	 * Create and add to ICAT a new User
	 * @param name
	 * @param fullName
	 * @return
	 * @throws IcatException_Exception
	 */
	private static User EnterUser(String name, String fullName) 
	throws IcatException_Exception{
		User user = new User();
		user.setName(name);
		user.setFullName(fullName);
		try {
			
			user.setId( icat.create(sessionId, user) );
			System.out.println("Created  User: '"+fullName + "'.");
			
		} catch (IcatException_Exception e) {
			logger.error("Impossible create User: '"+user+ "'. " + e.getMessage());
			throw e;
		}
		return user;
	}
	
	/**
	 * Create and add to ICAT a new UserGroup
	 * @param user
	 * @param group
	 * @throws IcatException_Exception
	 */
	private static void EnterUserGroup(User user, Group group) 
	throws IcatException_Exception{
		UserGroup userGroup = new UserGroup();
		userGroup.setUser(user);
		userGroup.setGroup(group);
		try {
			
			icat.create(sessionId, userGroup);
			System.out.println("Created  UserGroup: '"+user.getFullName()+"' - '"+group.getName() + "'.");
			
		} catch (IcatException_Exception e) {
			logger.error("Impossible create UserGroup: '"+userGroup+ "'. " + e.getMessage());
			throw e;
		}
	}
	//----------------------------------------------------------------------------------------------
	
	public static void SetupBeamlineSpecificData() throws Exception{
		
		//TODO
		SetupID19SpecificData();
	}
	
	/**
	 * Enter all ID19 parameters name and types
	 * @throws Exception 
	 */
	private static void SetupID19SpecificData() throws Exception{
		
		String currentDir = System.getProperty("user.dir");
		
		// Import all ParameterType relative to ID19
		String filename = currentDir+File.separatorChar+"resources"+File.separatorChar+"ParamTypeID19.xml";
		try {
			IcatPilotIngester.importXmlFile(filename);
		} catch (Exception e) {
			logger.error("Unable to import XML file: '"+filename+"' to restore ID19 specific data");
			throw e;
		}
	}
	
	//----------------------------------------------------------------------------------------------
	
	
	
	public static void addRulesToIcat() throws IcatException_Exception{
		
	
	 try {
			IcatSession.connect(IcatSession.ADMIN_USER);
			icat = IcatSession.getIcat();
			sessionId = IcatSession.getSession();
			
		}catch (MalformedURLException e) {
			logger.error("Impossible connnect to Icat: " + e.getMessage());
			//throw e;
		}catch (IcatException_Exception e) {
			logger.error("Impossible connnect to Icat: " + e.getMessage());
			//throw e;
		}
		
        
        //----------------------------------------------------------------------------------------------------------------------------
        //instrument scientist rules - these cause problems - see ICAT issue 83 https://code.google.com/p/icatproject/issues/detail?id=83 
        //investigation and investigation parameter
        List<String> scientistTables = new ArrayList<String>();
        
        scientistTables.add(												"Investigation <-> InvestigationUser <-> User [name = :user]");
        scientistTables.add(					 "InvestigationParameter <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        scientistTables.add(									"Dataset <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        scientistTables.add(			   "DatasetParameter <-> Dataset <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        scientistTables.add(					   "Datafile <-> Dataset <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        scientistTables.add( "DatafileParameter <-> Datafile <-> Dataset <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        scientistTables.add(									 "Sample <-> Investigation <-> InvestigationUser <-> User [name = :user]");
        
      /*  scientistTables.add(												"Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(					 "InvestigationParameter <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(									"Dataset <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(			   "DatasetParameter <-> Dataset <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(					   "Datafile <-> Dataset <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add( "DatafileParameter <-> Datafile <-> Dataset <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");
        scientistTables.add(									 "Sample <-> Investigation <-> Instrument <-> InstrumentScientist <-> User [name = :user]");  */
        
        
        Rule rule = new  Rule();
        for(String what : scientistTables)
        {
            rule.setGroup(null);
            rule.setCrudFlags("CRU"); //no delete permission for instrument scientist
            rule.setWhat(what);
            icat.create(sessionId, rule);
            System.out.println("Created Instrument Scientist rule: '" + what +"'");
        }
        
               
        
		
		/*
		 
		List<String> scientistTables = new ArrayList<String>();
		Rule rule = new  Rule();
   
		 
        //----------------------------------------------------------------------------------------------------------------------------
        //Principal Investigator - for now they can read and update everything they own - Investigation + P ; DS + P ; DF + P; Sample + P
        scientistTables.clear();
        
        scientistTables.add(											   "Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(					"InvestigationParameter <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(								   "Dataset <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(			  "DatasetParameter <-> Dataset <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(					  "Datafile <-> Dataset <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add("DatafileParameter <-> Datafile <-> Dataset <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(									"Sample <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        scientistTables.add(				"SampleParameter <-> Sample <-> Investigation <-> InvestigationUser [role = 'Principal Investigator'] <-> User [name = :user]");
        
        for(String what : scientistTables)
        {
			rule.setGroup(null);
            rule.setCrudFlags("RU"); //ONLY Read/Update permission for Principal scientist
            rule.setWhat(what);
            icat.create(sessionId, rule);
            System.out.println("Created Principal Investigator rule: '" + what +"'");
        }
        */
        
      /*          
        
        //----------------------------------------------------------------------------------------------------------------------------
        //Co-Investigator - for now they can read everything they are associated with - Investigation + P ; DS + P ; DF + P; Sample + P
        scientistTables.clear();
        
        scientistTables.add(											   "Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add("					 InvestigationParameter <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(								   "Dataset <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(			  "DatasetParameter <-> Dataset <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(					  "Datafile <-> Dataset <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add("DatafileParameter <-> Datafile <-> Dataset <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(									"Sample <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
        scientistTables.add(				"SampleParameter <-> Sample <-> Investigation <-> InvestigationUser <-> User <-> User [name = :user]");
                
        for(String what : scientistTables)
        {
            rule.setGroup(null);
            rule.setCrudFlags("R"); //ONLY Read permission for Co-Investigator 
            rule.setWhat(what);
            icat.create(sessionId, rule);
            System.out.println("Created Co-Investigator rule: '" + what +"'");
        }  
        
        */
		
		
	}
     
	
	
}
