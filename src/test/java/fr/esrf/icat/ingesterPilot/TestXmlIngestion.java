package fr.esrf.icat.ingesterPilot;
import static org.junit.Assert.*;

import org.junit.Test;

import fr.esrf.icat.ingesterPilot.IcatPilotIngester;


public class TestXmlIngestion {

	@Test
	public void test() {
		fail("Not yet implemented");
	}

	
	@Test 
	public void importTest(){
		String filename = "C:\\Users\\bessone.ESRF\\git\\Pilot_ReplaceTomoDB\\ingesterPilot\\src\\test\\java\\resouces\\import-icat.xml";
		try {
			IcatPilotIngester.importXmlFile(filename);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
