/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.systemtests;

import java.io.FileInputStream;
import java.util.Properties;

import org.codedefenders.util.Paths;
import org.codedefenders.util.tags.SystemTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

//FIXME
@Disabled
@SystemTest
public class CheckCodeDefendersVersionTest extends AbstractEmptyDBSystemTest {

    private static String versionNumber = null;

    @Test
    public void testCheckVersion() throws Exception {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("target/maven-archiver/pom.properties"));
            versionNumber = prop.getProperty("version");
        } catch (Throwable t) {
        }
        assumeTrue(versionNumber != null);

        // Navigate to About page - Sometimes if the element is not visible in
        // the page, we need to scroll to it otherwise selenium will complain:
        // Taken from: https://stackoverflow.com/questions/12035023/selenium-webdriver-cant-click-on-a-link-outside-the-page
        //        WebElement target = driver.findElement(By.id("footerAbout"));
        //        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", target);
        //        Thread.sleep(500); //not sure why the sleep was needed, but it was needed or it wouldnt work :(
        //        target.click();

        driver.get(codeDefendersHome + Paths.ABOUT_PAGE);

        // // Retrieve the element
        String versionString = driver.findElement(By.xpath("/html/body/div[2]/div[2]/div/div/p[1]")).getText();

        assertNotNull(versionString, "Cannot find version string on About Page!");
        versionString = versionString.trim();

        // This can be also String.contains(versionNumber)
        assertTrue(versionString.endsWith(versionNumber + "."),
                "Cannot find expected version number " + versionNumber);

    }

}
