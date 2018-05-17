package org.codedefenders.systemtests;

import static org.junit.Assert.fail;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthChecks;

@Category(SystemTest.class)
public class DockerSampleSystemTestUsingKatalon {

	@ClassRule
	public static DockerComposeRule docker = DockerComposeRule.builder()//
			.file("src/test/resources/systemtests/docker-compose.yml")//
			.waitingForService("selenium", HealthChecks.toHaveAllPortsOpen())//
			// .saveLogsTo("/tmp/test.log")// This is mostly for debugging
			.waitingForService("db", HealthChecks.toHaveAllPortsOpen()) //
			.waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080, new Function<DockerPort, String>() {
				@Override
				public String apply(DockerPort t) {
					return t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders");
				}
			})) //
			.build();

	private WebDriver driver;
	private StringBuffer verificationErrors = new StringBuffer();

	@Before
	public void setUp() throws Exception {
		ChromeOptions options = new ChromeOptions();
		// This is hardcoded but we cannot do otherwise. The alternative would be to run a docker grid
        driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		// Many suggests to define the size, but if you tests requires a different setting, change this
		driver.manage().window().setSize(new Dimension(1024,768));
		//
//		This is important: docker-compose randomly assign ports and names to
		// those logical entities so we get them from the rule
		DockerPort frontend = docker.containers().container("frontend").port(8080);
		// The docker container deploys codedefenders in this specific context.
		// Since we cannot connect to "localhost" from within the selenium-chrome, we rely on the
		// host.docker.internal (internal Docker DNS) to find out where codedefenders run
		String codeDefendersHome = frontend.inFormat("http://host.docker.internal:$EXTERNAL_PORT/codedefenders");
		// 
		//
		driver.get(codeDefendersHome);
	}

	/**
	 * This test contains and shall only contain the client logic
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUntitledTestCase() throws Exception {
		//
		driver.findElement(By.linkText("Enter")).click();
		//
		driver.findElement(By.linkText("Create an account")).click();
		driver.findElement(By.id("inputUsernameCreate")).sendKeys("bot");
		driver.findElement(By.id("inputEmail")).sendKeys("bot@bot.bot");
		driver.findElement(By.id("inputPasswordCreate")).sendKeys("12345678");
		driver.findElement(By.id("inputConfirmPasswordCreate")).sendKeys("12345678");
		driver.findElement(By.id("submitCreateAccount")).click();
		//
		driver.findElement(By.xpath("//div[2]/div/div[2]/div/div/div")).click();
		//
		driver.findElement(By.xpath("//ul[@id='bs-example-navbar-collapse-1']/li[5]/a")).click();
		driver.findElement(By.linkText("Logout")).click();
	}

	@After
	public void tearDown() throws Exception {
		driver.quit();
		String verificationErrorString = verificationErrors.toString();
		if (!"".equals(verificationErrorString)) {
			fail(verificationErrorString);
		}
	}

}
