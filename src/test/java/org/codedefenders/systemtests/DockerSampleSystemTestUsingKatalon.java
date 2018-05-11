package org.codedefenders.systemtests;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthChecks;

import io.github.bonigarcia.wdm.WebDriverManager;

@Category(SystemTest.class)
public class DockerSampleSystemTestUsingKatalon {

	@ClassRule
	public static DockerComposeRule docker = DockerComposeRule.builder()//
			.file("src/test/resources/systemtests/docker-compose.yml")//
			// .saveLogsTo("/tmp/test.log")// This is mostly for debugging
			.waitingForService("db", HealthChecks.toHaveAllPortsOpen()) //
			.waitingForService("frontend", HealthChecks.toRespond2xxOverHttp(8080, new Function<DockerPort, String>() {
				@Override
				public String apply(DockerPort t) {
					return t.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders");
				}
			}))//
			.build();

	private WebDriver driver;
	private StringBuffer verificationErrors = new StringBuffer();

	@Before
	public void setUp() throws Exception {
		// This automagically download and install the right driver for you
		// environment
		// https://stackoverflow.com/questions/7450416/selenium-2-chrome-driver?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
		WebDriverManager.chromedriver().setup();
		ChromeOptions options = new ChromeOptions();
		// This 
		options.setHeadless( true );
		driver = new ChromeDriver( options );
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	@Test
	public void testUntitledTestCase() throws Exception {
		// This is important: docker-compose randomly assign ports and names to
		// those logical entities so we get them from the rule
		DockerPort frontend = docker.containers().container("frontend").port(8080);
		// The docker container deploys codedefenders in this specific context
		String codeDefendersHome = frontend.inFormat("http://$HOST:$EXTERNAL_PORT/codedefenders");

		System.out.println("Connecting to " + codeDefendersHome);
		//
		driver.get(codeDefendersHome);
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
