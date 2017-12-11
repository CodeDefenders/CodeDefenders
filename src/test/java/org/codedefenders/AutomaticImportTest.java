package org.codedefenders;

import org.junit.Ignore;

public class AutomaticImportTest {

	@Ignore
	@org.junit.Test
	public void testAutomaticImportOnlyPrimitive(){
		GameClass gc = new GameClass("Lift", "Lift", 
				"src/test/resources/itests/sources/Lift/Lift.java",
				"src/test/resources/itests/sources/Lift/Lift.class");
		
	}
	@org.junit.Test
	public void testAutomaticImport(){
		GameClass gc = new GameClass("XmlElement", "XmlElement", 
				"src/test/resources/itests/sources/XmlElement/XmlElement.java",
				"src/test/resources/itests/sources/XmlElement/XmlElement.class");

		System.out.println("AutomaticImportTest.testAutomaticImport()" + gc.getTestTemplate());
	}
}
