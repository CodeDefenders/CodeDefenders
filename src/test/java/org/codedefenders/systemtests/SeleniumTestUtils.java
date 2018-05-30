package org.codedefenders.systemtests;

import java.text.MessageFormat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.openqa.selenium.WebElement;

public class SeleniumTestUtils {

    /**
     * Checks if {@code actualURL} ends with {@code expectedURL}.
     * Fails the test if {@code acutalURL} doesn't end with {@code expectedURL}
     */
    public static void assertURLEndsWith(String actualURL, String expectedURL) {
        assertTrue(MessageFormat.format("Current url ({0}) should end with \"{1}\"", actualURL, expectedURL),
                actualURL.endsWith(expectedURL) || actualURL.endsWith(expectedURL + "/"));
    }

    /**
     * Checks if {@code actualURL} does not end with {@code expectedURL}.
     * Fails the test if {@code acutalURL} ends with {@code expectedURL}
     */
    public static void assertURLDoesntEndWith(String actualURL, String expectedURL) {
        assertFalse(MessageFormat.format("Current url ({0}) should end with \"{1}\"", actualURL, expectedURL),
                actualURL.endsWith(expectedURL) || actualURL.endsWith(expectedURL + "/"));
    }

    /**
     * Waits for an HTML element with the given {@code id} to be visible.
     * Fails the test if the element is not visible after 5 seconds.
     */
    public static void waitForVisible(WebElement element) throws InterruptedException {
        for (int millis = 0; millis < 5000 ; millis += 500) {
            if (element.isDisplayed())
                return;
            Thread.sleep(500);
        }
        fail("Timeout while waiting for element to be visible.");
    }

}
