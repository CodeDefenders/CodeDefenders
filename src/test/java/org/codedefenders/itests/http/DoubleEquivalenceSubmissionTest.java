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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.codedefenders.auth.PasswordEncoderProvider;
import org.codedefenders.model.UserEntity;
import org.codedefenders.util.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This test assumes that the Web app is deployed at localhost:8080/ it's just
 * the client side
 *
 * @author gambi
 */
//@SystemTest
public class DoubleEquivalenceSubmissionTest {

    // Use directly form submission, do not care about UI interactions, cannot
    // do it with pre-loaded db
    //
    // Login as creator
    // Create a new game as creator, use Lift class
    // Start the game
    // Logout => Store Game ID
    //
    // Login as attacker
    // Join the game --> GameID
    // Attack with MutantLift -> mutant 1
    // Logout

    // Login as defender1
    // Join the game --> GameID
    // Submit Test Covering but not killing
    // Wait

    // Repeat the same for defender2

    // At this point both defenders are ready to claim equivalence
    // Do it in parallel

    // private WebClient webClient;
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
            //
            // }
            // });
            webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

                @Override
                public void scriptException(InteractivePage page, ScriptException scriptException) {

                }

                @Override
                public void timeoutError(InteractivePage page, long allowedTime, long executionTime) {

                }

                @Override
                public void malformedScriptURL(InteractivePage page, String url,
                        MalformedURLException malformedURLException) {

                }

                @Override
                public void loadScriptError(InteractivePage page, URL scriptUrl, Exception exception) {

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

    class HelperUser {
        private UserEntity user;
        private String password;
        private WebClient browser;

        public HelperUser(UserEntity user, String password) {
            this.user = user;
            this.password = password;
            this.browser = WebClientFactory.getNewWebClient();
        }

        public void doLogin() throws FailingHttpStatusCodeException, IOException {
            WebRequest loginRequest = new WebRequest(new URL("http://localhost:8080" + Paths.LOGIN), HttpMethod.POST);
            // // Then we set the request parameters
            loginRequest.setRequestParameters(List.of(
                    new NameValuePair("formType", "login"),
                    new NameValuePair("username", user.getUsername()),
                    new NameValuePair("password", password)));
            // Finally, we can get the page
            HtmlPage retunToGamePage = browser.getPage(loginRequest);
        }

        public int createNewGame() throws FailingHttpStatusCodeException, IOException {
            // List the games already there
            Set<String> myGames = new HashSet<>();

            HtmlPage gameUsers = browser.getPage("http://localhost:8080" + Paths.GAMES_OVERVIEW);
            for (HtmlAnchor a : gameUsers.getAnchors()) {
                if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?gameId=")) {
                    myGames.add(a.getHrefAttribute());
                }
            }

            WebRequest createGameRequest = new WebRequest(new URL("http://localhost:8080/multiplayer" + Paths.GAMES_OVERVIEW),
                    HttpMethod.POST);
            createGameRequest.setRequestParameters(List.of(
                    new NameValuePair("formType", "createGame"),
                    new NameValuePair("class", "221") // This is hardcoded, it's Lift.class
                    // new NameValuePair("level", "true"),
            ));

            gameUsers = browser.getPage(createGameRequest);
            String newGameLink = null;
            // TODO Check that we get there ?
            for (HtmlAnchor a : gameUsers.getAnchors()) {
                if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?gameId=")) {
                    if (!myGames.contains(a.getHrefAttribute())) {
                        newGameLink = a.getHrefAttribute();
                        break;
                    }
                }
            }
            // There's should be only one


            return Integer.parseInt(newGameLink.replaceAll("multiplayer/games\\?gameId=", ""));

        }

        public void startGame(int gameId) throws FailingHttpStatusCodeException, IOException {

            WebRequest startGameRequest = new WebRequest(new URL("http://localhost:8080" + Paths.BATTLEGROUND_GAME),
                    HttpMethod.POST);
            // // Then we set the request parameters
            startGameRequest.setRequestParameters(List.of(
                    new NameValuePair("formType", "startGame"),
                    new NameValuePair("gameId", "" + gameId)
            ));
            // Finally, we can get the page
            // Not sure why this returns TextPage and not HtmlPage
            browser.getPage(startGameRequest);

        }

        public void joinOpenGame(int gameId, boolean isAttacker) throws FailingHttpStatusCodeException, IOException {
            HtmlPage openGames = browser.getPage("http://localhost:8080" + Paths.GAMES_OVERVIEW);

            // Really we can simply click on that link once we know the gameId,
            // no need to go to openGame page
            HtmlAnchor joinLink = null;
            for (HtmlAnchor a : openGames.getAnchors()) {
                if (a.getHrefAttribute().contains(
                        Paths.BATTLEGROUND_GAME + "?" + ((isAttacker) ? "attacker" : "defender") + "=1&gameId=" + gameId)) {
                    joinLink = a;
                    break;
                }
            }
            if (!joinLink.getHrefAttribute().startsWith("http://localhost:8080/")) {
                joinLink.setAttribute("href", "http://localhost:8080/" + joinLink.getHrefAttribute());
            }
            HtmlPage page = joinLink.click();
        }

        public void attack(int gameId, String mutant) throws FailingHttpStatusCodeException, IOException {
            WebRequest attackRequest = new WebRequest(new URL("http://localhost:8080" + Paths.BATTLEGROUND_GAME),
                    HttpMethod.POST);
            // // Then we set the request parameters
            attackRequest.setRequestParameters(List.of(
                    new NameValuePair("formType", "createMutant"),
                    new NameValuePair("gameId", "" + gameId),
                    // TODO Encoded somehow ?
                    new NameValuePair("mutant", "" + mutant)
            ));
            // curl -X POST \
            // --data "formType=createMutant&gameId=${gameId}" \
            // --data-urlencode mutant@${mutant} \
            // --cookie "${cookie}" --cookie-jar "${cookie}" \
            // -w @curl-format.txt \
            // -s ${CODE_DEFENDER_URL}/multiplayergame
            browser.getPage(attackRequest);

        }

        public void defend(int gameId, String test) throws FailingHttpStatusCodeException, IOException {
            WebRequest defendRequest = new WebRequest(new URL("http://localhost:8080" + Paths.BATTLEGROUND_GAME),
                    HttpMethod.POST);
            // curl -X POST \
            // --data "formType=createTest&gameId=${gameId}" \
            // --data-urlencode test@${test} \
            // --cookie "${cookie}" --cookie-jar "${cookie}" \
            // -w @curl-format.txt \
            // -s ${CODE_DEFENDER_URL}/multiplayergame
            defendRequest.setRequestParameters(List.of(
                    new NameValuePair("formType", "createTest"),
                    new NameValuePair("gameId", "" + gameId),
                    // TODO Encoded somehow ?
                    new NameValuePair("test", "" + test)
            ));
            browser.getPage(defendRequest);
        }

        // public void register() {
        // // command="curl -s --data
        // \"formType=create&username=${username}&email=${email}&password=${password}&confirm=${password}\"
        // ${CODE_DEFENDER_URL}/login && echo \"SUCCESS!\" || echo \"FAILED TO
        // REGISTER ${username} !\""
        // }

        public void claimEquivalenceOnLine(int gameId, int line)
                throws FailingHttpStatusCodeException, MalformedURLException, IOException {
            HtmlPage playPage = browser.getPage("http://localhost:8080" + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            HtmlAnchor claimEquivalenceLink = null;
            for (HtmlAnchor a : playPage.getAnchors()) {
                if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?equivLines=" + line)) {
                    claimEquivalenceLink = a;
                    break;
                }
            }

            if (!claimEquivalenceLink.getHrefAttribute().startsWith("http://localhost:8080/")) {
                claimEquivalenceLink.setAttribute("href",
                        "http://localhost:8080/" + claimEquivalenceLink.getHrefAttribute());
            }
            claimEquivalenceLink.click();
        }

        public void acceptEquivalence(int gameId)
                throws FailingHttpStatusCodeException, MalformedURLException, IOException {
            // http://localhost:8080/multiplayer/
            HtmlPage playPage = browser.getPage("http://localhost:8080" + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            HtmlAnchor acceptEquivalenceLink = null;
            for (HtmlAnchor a : playPage.getAnchors()) {
                if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?acceptEquivalent=")) {
                    acceptEquivalenceLink = a;
                    break;
                }
            }

            if (!acceptEquivalenceLink.getHrefAttribute().startsWith("http://localhost:8080/")) {
                acceptEquivalenceLink.setAttribute("href",
                        "http://localhost:8080/" + acceptEquivalenceLink.getHrefAttribute());
            }

            System.out.println(
                    "DoubleEquivalenceSubmissionTest.HelperUser.acceptEquivalence() Accepting equivalence on game "
                            + gameId);
            acceptEquivalenceLink.click();
        }

        public void assertNoMoreEquivalenceDuels(int gameId)
                throws FailingHttpStatusCodeException, MalformedURLException, IOException {
            HtmlPage playPage = browser.getPage("http://localhost:8080" + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            for (HtmlAnchor a : playPage.getAnchors()) {
                if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?acceptEquivalent=")) {
                    fail("On game " + gameId + " there is still an equivalence duel open");
                }
            }
        }

        public void assertThereIsAnEquivalenceDuel(int gameId)
                throws FailingHttpStatusCodeException, MalformedURLException, IOException {
            HtmlPage playPage = browser.getPage("http://localhost:8080" + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            for (HtmlAnchor a : playPage.getAnchors()) {
                if (a.getHrefAttribute().contains(Paths.BATTLEGROUND_GAME + "?acceptEquivalent=")) {
                    return;
                }
            }
            fail("On game " + gameId + " there is no equivalence duels open");

        }

    }

    @Disabled
    @Test
    public void doubleSubmissionTest() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        UserEntity creatorUser = new UserEntity("creator", EMPTY_PW);
        HelperUser creator = new HelperUser(creatorUser, "test");
        creator.doLogin();
        System.out.println("Creator Login");
        //
        int newGameId = creator.createNewGame();
        System.out.println("Creator Create new Game: " + newGameId);
        //
        creator.startGame(newGameId);
        //
        UserEntity attackerUser = new UserEntity("demoattacker", EMPTY_PW);
        HelperUser attacker = new HelperUser(attackerUser, "test");
        attacker.doLogin();
        System.out.println("Attacker Login");
        //
        attacker.joinOpenGame(newGameId, true);
        System.out.println("Attacker Join game " + newGameId);
        //
        UserEntity defenderUser = new UserEntity("demodefender", EMPTY_PW);
        HelperUser defender = new HelperUser(defenderUser, "test");
        defender.doLogin();
        //
        System.out.println("Defender Login");
        //
        defender.joinOpenGame(newGameId, false);
        //
        System.out.println("Defender Join game " + newGameId);
        //
        UserEntity defender2User = new UserEntity("demodefender", EMPTY_PW);
        HelperUser defender2 = new HelperUser(defender2User, "test");
        defender2.doLogin();
        //
        System.out.println("Defender 2 Login");
        //
        defender2.joinOpenGame(newGameId, false);
        //
        System.out.println("Defender 2 Join game " + newGameId);
        //
        // Create the mutant
        attacker.attack(newGameId,
                Files.readString(
                        new File("src/test/resources/itests/mutants/Lift/MutantLift1.java").toPath(),
                        Charset.defaultCharset()));
        System.out.println("Attacker attack in game " + newGameId);

        // // Cover the mutant with a non-mutant-killing test
        String coveringButNotKillingTest = Files.readString(
                new File("src/test/resources/itests/tests/Lift/CoveringButNotKillingTest.java").toPath(),
                Charset.defaultCharset());
        defender.defend(newGameId, coveringButNotKillingTest);
        System.out.println("Defender defend in game " + newGameId);
        defender2.defend(newGameId, coveringButNotKillingTest);
        System.out.println("Defender2 defend in game " + newGameId);
        //
        // Claim the equivalence "at the same time" using threads/runnables
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try {
                defender.claimEquivalenceOnLine(newGameId, 9);
                System.out.println("Defender claim equivalence in game " + newGameId);
            } catch (FailingHttpStatusCodeException | IOException e) {
                e.printStackTrace();
            }
        });
        executorService.submit(() -> {
            try {
                defender2.claimEquivalenceOnLine(newGameId, 9);
                System.out.println("Defender 2 claim equivalence in game " + newGameId);
            } catch (FailingHttpStatusCodeException | IOException e) {
                e.printStackTrace();
            }
        });
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        attacker.assertThereIsAnEquivalenceDuel(newGameId);
        //
        attacker.acceptEquivalence(newGameId);
        //
        attacker.assertNoMoreEquivalenceDuels(newGameId);
    }

    @AfterEach
    public void afterEachTest() throws Exception {
        WebClientFactory.closeAllClients();
    }
}
