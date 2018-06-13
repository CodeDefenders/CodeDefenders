package org.codedefenders.systemtests;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

@Category(SystemTest.class)
public class CheckCodeDefendersVersionTest extends AbstractEmptyDBSystemTest {

	private static String versionNumber = null;

	@Test
	public void testCheckVersion() throws Exception {
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream(new File("target/maven-archiver/pom.properties")));
			versionNumber = prop.getProperty("version");
		} catch (Throwable t) {
		}
		Assume.assumeNotNull(versionNumber);

		// Navigate to About page - Sometimes if the element is not visible in
		// the page, we need to scroll to it otherwise selenium will complain:
		// Taken from: https://stackoverflow.com/questions/12035023/selenium-webdriver-cant-click-on-a-link-outside-the-page
//		WebElement target = driver.findElement(By.id("footerAbout"));
//		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", target);
//		Thread.sleep(500); //not sure why the sleep was needed, but it was needed or it wouldnt work :(
//		target.click();
		
		driver.get(codeDefendersHome+"/about");
		
		// // Retrieve the element
		String versionString = driver.findElement(By.xpath("/html/body/div[2]/div[2]/div/div/p[1]")).getText();

		Assert.assertNotNull("Cannot find version string on About Page!", versionString);
		versionString = versionString.trim();

		// This can be also String.contains(versionNumber)
		Assert.assertTrue("Cannot find expected version number " + versionNumber,
				versionString.endsWith(versionNumber + "."));

	}

}
