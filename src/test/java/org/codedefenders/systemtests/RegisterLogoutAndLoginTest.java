package org.codedefenders.systemtests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

@Category(SystemTest.class)
public class RegisterLogoutAndLoginTest extends AbstractEmptyDBSystemTest {

	/**
	 * This test contains and shall only contain the client logic
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUntitledTestCase() throws Exception {
		// Register user
		// Navigate to login page
		driver.findElement(By.linkText("Enter")).click();
		// Open registration form
		driver.findElement(By.linkText("Create an account")).click();
		// Fill out the form
		driver.findElement(By.id("inputUsernameCreate")).sendKeys("bot");
		driver.findElement(By.id("inputEmail")).sendKeys("bot@bot.bot");
		driver.findElement(By.id("inputPasswordCreate")).sendKeys("12345678");
		driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("12345678");
		// Submit form
		driver.findElement(By.id("submitCreateAccount")).click();
		// If registration was fine, we are on the home page
		// TODO Make an explicit assertion, otherwise, selenium waits 10 sec and if it cannot find the next element to click on, fails the test
		//
		// Click on the menu to visualize Logout button
		driver.findElement(By.xpath("//ul[@id='bs-example-navbar-collapse-1']/li[5]/a")).click();
		// Logout
		driver.findElement(By.linkText("Logout")).click();
		// At this point we should be on Home page and we can login with our credentials
		driver.findElement(By.linkText("Enter")).click();

		driver.findElement(By.id("inputUsername")).sendKeys("bot");
		driver.findElement(By.id("inputPassword")).sendKeys("12345678");

		driver.findElement(By.id("signInButton")).click();

		// If login was fine, we are on the home page
		// TODO Make an explicit assertion, otherwise, selenium waits 10 sec and if it cannot find the next element to click on, fails the test
		driver.findElement(By.linkText("Create Battleground"));
	}

}
