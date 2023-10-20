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
package org.codedefenders.itests.http;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.codedefenders.auth.PasswordEncoderProvider;
import org.codedefenders.itests.http.utils.HelperUser;
import org.codedefenders.model.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

/**
 * This test assumes that the Web app is deployed at localhost:8080/ it's just
 * the client side
 *
 * @author gambi
 */
//@SystemTest
public class UnkillableMutantTest {

    private static final int TIMEOUT = 10000;
    private static final String EMPTY_PW = PasswordEncoderProvider.getPasswordEncoder().encode("");

    static class WebClientFactory {
        private static Collection<WebClient> clients = new ArrayList<>();

        public static WebClient getNewWebClient() {
            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

            // webClient = new WebClient(BrowserVersion.CHROME);
            WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);

            webClient.getOptions().setCssEnabled(true);
            webClient.setCssErrorHandler(new SilentCssErrorHandler());

            // Do not fail on status code ?
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            // Disable test failing because of JS exceptions
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            webClient.getOptions().setRedirectEnabled(true);
            webClient.getOptions().setAppletEnabled(false);

            webClient.getOptions().setJavaScriptEnabled(true);

            webClient.getOptions().setPopupBlockerEnabled(true);
            webClient.getOptions().setTimeout(TIMEOUT);
            webClient.getOptions().setPrintContentOnFailingStatusCode(false);
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.setAlertHandler((page, message) -> System.err.println("[alert] " + message));
            // Shut down HtmlUnit
            // webClient.setIncorrectnessListener(new IncorrectnessListener() {
            //
            // @Override
            // public void notify(String arg0, Object arg1) {
            // // TODO Auto-generated method stub
            //
            // }
            // });
            webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

                @Override
                public void scriptException(InteractivePage page, ScriptException scriptException) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void timeoutError(InteractivePage page, long allowedTime, long executionTime) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void malformedScriptURL(InteractivePage page, String url,
                        MalformedURLException malformedURLException) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void loadScriptError(InteractivePage page, URL scriptUrl, Exception exception) {
                    // TODO Auto-generated method stub

                }
            });
            webClient.waitForBackgroundJavaScript(TIMEOUT);

            webClient.setRefreshHandler(new WaitingRefreshHandler());
            clients.add(webClient);

            return webClient;
        }

        public static void closeAllClients() {
            for (WebClient webClient : clients) {
                if (webClient != null) {
                    webClient.close();
                }
            }
        }
    }


    @Disabled
    @Test
    public void testUnkillableMutant() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        // // This test assumes an empty db !
        UserEntity creatorUser = new UserEntity("creator", EMPTY_PW);
        HelperUser creator = new HelperUser(creatorUser, WebClientFactory.getNewWebClient(), "localhost", "test");
        creator.doLogin();
        System.out.println("Creator Login");

        // Upload the class
        int classId = creator.uploadClass(new File("src/test/resources/itests/sources/XmlElement/XmlElement.java"));

        System.out.println("UnkillableMutant.testUnkillableMutant() Class ID = " + classId);

        int newGameId = creator.createNewGame(classId);
        System.out.println("Creator Create new Game: " + newGameId);

        creator.startGame(newGameId);

        UserEntity attackerUser = new UserEntity("demoattacker", EMPTY_PW);
        HelperUser attacker = new HelperUser(attackerUser, WebClientFactory.getNewWebClient(), "localhost", "test");
        attacker.doLogin();
        System.out.println("Attacker Login");

        attacker.joinOpenGame(newGameId, true);
        System.out.println("Attacker Join game " + newGameId);
        // Submit the unkillable mutant
        attacker.attack(newGameId,
                Files.readString(
                        new File("src/test/resources/itests/mutants/XmlElement/Mutant9559.java").toPath(),
                        Charset.defaultCharset()));
        System.out.println("Attacker attack in game " + newGameId);

        UserEntity defenderUser = new UserEntity("demodefender", EMPTY_PW);
        HelperUser defender = new HelperUser(defenderUser, WebClientFactory.getNewWebClient(), "localhost", "test");
        defender.doLogin();

        System.out.println("Defender Login");

        defender.joinOpenGame(newGameId, false);

        System.out.println("Defender Join game " + newGameId);


        // Submit all the compilable tests from the original game (182)
        Collection<File> testFiles = FileUtils.listFiles(
                new File("src/test/resources/itests/tests/XmlElement/182"),
                new RegexFileFilter("^.*.java"),
                DirectoryFileFilter.DIRECTORY
        );

        for (File testFile : testFiles) {
            System.out.println("UnkillableMutant.testUnkillableMutant() Defending with " + testFile);
            defender.defend(newGameId, Files.readString(testFile.toPath(), Charset.defaultCharset()));
        }
    }

    @AfterEach
    public void afterEachTest() throws Exception {
        WebClientFactory.closeAllClients();
    }
}
