package fr.esrf.icat.ingesterPilot;

import java.awt.List;
import java.io.IOException;
import java.net.MalformedURLException;
import org.icatproject.Facility;
import org.icatproject.FacilityCycle;
import org.icatproject.Group;
import org.icatproject.ICAT;
import org.icatproject.IcatException_Exception;
import org.icatproject.Instrument;
import org.icatproject.Investigation;
import org.icatproject.InvestigationType;
import org.icatproject.Study;
import org.icatproject.User;

public class IcatUtils {

	static String sessionId = null;
	static ICAT icat = null;

	//-------------------------------------------------------------------------------------------
	//  EXISTS methods
	//-------------------------------------------------------------------------------------------
	/**
	 * Return TRUE if the investigation alredy exist in ICAT
	 * @param expName2
	 * @param beamline
	 * @param date
	 * @return
	 * @throws MalformedURLException
	 * @throws IcatException_Exception
	 */
	public static boolean existInvestigationsInICAT(String expName2,
			String beamline, String date) throws MalformedURLException,
			IcatException_Exception {

		// Connect to ICAT
		if (icat == null || sessionId == null) {
			try {
				icat = IcatSession.getIcat();
				sessionId = IcatSession.getSession();
			} catch (IcatException_Exception | MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Search for the Investigation name
		java.util.List<Object> ids = null;
		try {
			ids = icat.search(sessionId, "Study [" + "name = '" + expName2
					+ "']");
			if (ids == null || ids.isEmpty()) {
				return false;// Should not happen
			}
			Study st = (Study) ids.get(0);
			long stId = st.getId();

			ids = icat.search(sessionId, "Instrument [" + "name = '" + beamline
					+ "']");
			Instrument inv = (Instrument) ids.get(0);
			long invId = inv.getId();

			ids = icat.search(sessionId, "Investigation [" + "study_id = '"
					+ stId + "' AND " + "instrument_id = '" + invId + "' AND "
					+ "startdate <= '" + date + "' AND " + "enddate >= '"
					+ date + "']");
		} catch (IcatException_Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return(ids != null && !ids.isEmpty()) ;

	}

	/**
	 * Return TRUE if the Study alredy exist in ICAT
	 * @param studyName2
	 * @return
	 * @throws IcatException_Exception
	 * @throws IOException 
	 */
	public static boolean existStudy(String studyName)
			throws IcatException_Exception, IOException {

		String query = "Study[name = '" + studyName + "']";
		
		try{

			return perfomIcatExistQuery(query);
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to check if Study '"+studyName+"' exists in ICAT "+ e.getMessage());
			throw ex;
		}

	}

	/**
	 * Return TRUE if the User alredy exist in ICAT
	 * @param userName
	 * @param userSurname
	 * @return
	 * @throws IOException 
	 */
	public static boolean userExistInICAT(String userName, String userSurname) 
			throws IOException {
		
		String query = "User[name = '" + userSurname.toUpperCase() + "']";
		
		try{

			return perfomIcatExistQuery(query);
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to check if User '"+userName+" - "+ userSurname+"' exists in ICAT "+ e.getMessage());
			throw ex;
		}
	}
	
	/**
	 * Return the request  if exist in Icat
	 * @param userId
	 * @param invId
	 * @return
	 * @throws IOException 
	 */
	public static boolean existInvestigationUser(Long userId, Long invId) 
			throws IOException {

		String query = "InvestigationUser[user.id = "
					+ userId + " AND investigation.id = " + invId + "]";
		
		try{

			return perfomIcatExistQuery(query);
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to check if InvestigationUser '"+userId+" - "+ invId+"' exists in ICAT "+ e.getMessage());
			throw ex;
		}
	}

	/**
	 * 
	 * @param scientistName
	 * @param groupName
	 * @return
	 * @throws IOException 
	 */
	public static boolean existUserGroup(String scientistName,String groupName) 
			throws IOException {
		String query = "UserGroup <-> User [name = '"
					+ scientistName.toUpperCase() + "'] <-> Group [ name = '"
					+ groupName + "']";
		try{

			return perfomIcatExistQuery(query);
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to check if UserGroup '"+scientistName+" - "+ groupName+"' exists in ICAT. "+ e.getMessage());
			throw ex;
		}
	}

	/**
	 * 
	 * @param datasetName
	 * @return
	 * @throws IOException 
	 */
	public static boolean existDataset(String datasetName)
			throws IOException {

		String query = "Dataset[name = '" + datasetName + "']";
		try{

			return perfomIcatExistQuery(query);
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to check if Dataset '"+datasetName+"' exists in ICAT. "+ e.getMessage());
			throw ex;
		}

	}
	
	//-------------------------------------------------------------------------------------------
	//  CORE private methods
	//-------------------------------------------------------------------------------------------
	/**
	 * Submit a specific Exist query to Icat and return True/False
	 * @param icatQuery
	 * @return
	 * @throws IcatException_Exception
	 * @throws MalformedURLException
	 */
	private static boolean perfomIcatExistQuery(String icatQuery) 
			throws IcatException_Exception, MalformedURLException{ 
		
		
		java.util.List<Object> ids = perfomIcatQuery(icatQuery);

		return(ids != null && !ids.isEmpty());

	}
	
	/**
	 * Submit a specific query to Icat and return the objects
	 * @param icatQuery
	 * @return
	 * @throws IcatException_Exception
	 * @throws MalformedURLException
	 */
	private static java.util.List<Object> perfomIcatQuery(String icatQuery) 
			throws IcatException_Exception, MalformedURLException{ 
		
		if (icat == null || sessionId == null) {
				icat = IcatSession.getIcat();
				sessionId = IcatSession.getSession();
		}
		
		return icat.search(sessionId, icatQuery);

	}

	
	//-------------------------------------------------------------------------------------------
	//  GET methods
	//-------------------------------------------------------------------------------------------
	
	/**
	 * Return the request users if exist in Icat
	 * @param userName
	 * @param userSurname
	 * @return
	 * @throws IOException 
	 */
	public static User getUser(String userName, String userSurname) throws IOException {
		
		String query = "User[name = '" + userSurname.toUpperCase() + "']";
		
		try{

			java.util.List<Object> list = perfomIcatQuery(query);
			if (list.isEmpty()){
				return null;
			}else{
				return (User)list.get(0);
			} 
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to retrive User '"+userName+" - "+userSurname+"' from ICAT. "+ e.getMessage());
			throw ex;
		}

	}

	/**
	 * Return the request FacilityCycle if exist in Icat
	 * @param runCode
	 * @return
	 * @throws IOException 
	 */
	public static FacilityCycle getFacilityCycle(String runCode) throws IOException {
		
		String query = "FacilityCycle[name = '" + runCode+ "']";
		
		try{
			java.util.List<Object> list = perfomIcatQuery(query);
			if (list.isEmpty()){
				return null;
			}else{
				return (FacilityCycle)list.get(0);
			}
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to retrive FacilityCycle '"+runCode+"' from ICAT. "+ e.getMessage());
			throw ex;
		}
		
	}

	/**
	 * Return the request Facility if exist in Icat
	 * @param facilityName
	 * @return
	 * @throws IOException 
	 */
	public static Facility getFacility(String facilityName) throws IOException {
		
		String query = "Facility[name = '" + facilityName+ "']";
		
		try{
			java.util.List<Object> list = perfomIcatQuery(query);
			if (list.isEmpty()){
				return null;
			}else{
				return (Facility)list.get(0);
			}
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to retrive Facility '"+facilityName+"' from ICAT. "+ e.getMessage());
			throw ex;
		}
	}

	/**
	 * Return the request Instrument if exist in Icat
	 * @param beamlineName
	 * @return
	 * @throws IOException 
	 */
	public static Instrument getInstrument(String beamlineName) throws IOException {

		String query =  "Instrument[name = '" + beamlineName
					+ "']";
		
		try{
			java.util.List<Object> list = perfomIcatQuery(query);
			if (list.isEmpty()){
				return null;
			}else{
				return (Instrument)list.get(0);
			}
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to retrive Facility '"+beamlineName+"' from ICAT. "+ e.getMessage());
			throw ex;
		}
	}

	/**
	 * Return the request InvestigationType if exist in Icat
	 * @param proposalCode
	 * @return
	 * @throws IOException 
	 */
	public static InvestigationType getInvestigationType(String proposalCode) throws IOException {

		String query = "InvestigationType[name = '"+ proposalCode + "']";
		
		try{
			java.util.List<Object> list = perfomIcatQuery(query);
			if (list.isEmpty()){
				return null;
			}else{
				return (InvestigationType)list.get(0);
			}
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to retrive InvestigationType '"+proposalCode+"' from ICAT. "+ e.getMessage());
			throw ex;
		}
	}

	
	/**
	 * Return the request Investigation if exist in Icat
	 * @param name
	 * @return
	 * @throws IOException 
	 */
	public static Investigation getInvestigation(String name) throws IOException {

		String query = "Investigation[name = '" + name + "']";
		
		try{
			java.util.List<Object> list = perfomIcatQuery(query);
			if (list.isEmpty()){
				return null;
			}else{
				return (Investigation)list.get(0);
			}
			
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to retrive Investigation '"+name+"' from ICAT. "+ e.getMessage());
			throw ex;
		}
		
	}

	/**
	 * Return the request Study if exist in Icat
	 * @param proposalName
	 * @return
	 * @throws IOException 
	 */
	public static Study getStudy(String proposalName) throws IOException {
		
		String query = "Study[name = '" + proposalName + "']";
		
		try{

			java.util.List<Object> list = perfomIcatQuery(query);
			if (list.isEmpty()){
				return null;
			}else{
				return (Study)list.get(0);
			}
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to retrive Study '"+proposalName+"' from ICAT. "+ e.getMessage());
			throw ex;
		}
	}

	/**
	 * Return the request Group if exist in Icat
	 * @param proposalName
	 * @return
	 * @throws IOException 
	 */
	public static Group getGroup(String proposalName) throws IOException {
		
		String query =  "Group[name = '" + proposalName + "']";
		
		try{

			java.util.List<Object> list = perfomIcatQuery(query);
			if (list.isEmpty()){
				return null;
			}else{
				return (Group)list.get(0);
			}
			
		} catch (IcatException_Exception | MalformedURLException e) {
			IOException ex = new IOException("Unable to retrive Study '"+proposalName+"' from ICAT. "+ e.getMessage());
			throw ex;
		}
	}
	

}