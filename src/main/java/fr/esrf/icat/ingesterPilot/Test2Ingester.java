package fr.esrf.icat.ingesterPilot;

import static org.junit.Assert.*;

import org.junit.Test;

public class Test2Ingester {

	@Test
	public void testImportTest() {
		String filename = "C:\\Users\\bessone.ESRF\\git\\Pilot_ReplaceTomoDB\\ingesterPilot\\src\\test\\java\\resouces\\import-icat.xml";
		try {
			IcatPilotIngester.importXmlFile(filename);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fail("Not yet implemented");
	}

}
