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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.codedefenders.auth.PasswordEncoderProvider;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.itests.http.UnkillableMutantTest.WebClientFactory;
import org.codedefenders.itests.http.utils.AttackAction;
import org.codedefenders.itests.http.utils.CodeDefenderAction;
import org.codedefenders.itests.http.utils.DefendAction;
import org.codedefenders.itests.http.utils.HelperUser;
import org.codedefenders.model.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

/**
 * Given a trace file this class replay the game as close as possible...
 *
 * <p>This is an utility class for running tests
 *
 * @author gambi
 */
public class GameReplayer {

    private final static long INITIAL_DELAY_MILLIS = 5000;
    private final static int SPEED_UP = 10;

    private String cutFile;
    private String traceFile;
    private String testsFile;
    private String mutantsFile;

    // Delay, Actions
    private List<CodeDefenderAction> actions;
    //
    // private ScheduledExecutorService newScheduledThreadPool;
    //
    private Map<String, HelperUser> actors;
    private Map<String, String> tests;
    private Map<String, String> mutants;

    private final PasswordEncoder passwordEncoder = PasswordEncoderProvider.getPasswordEncoder();

    public GameReplayer(String cutFile, String traceFile, String testsFile, String mutantsFile) {
        this.cutFile = cutFile;
        this.traceFile = traceFile;
        this.testsFile = testsFile;
        this.mutantsFile = mutantsFile;

        actors = new HashMap<>();
        actions = new ArrayList<>();
        tests = new HashMap<>();
        mutants = new HashMap<>();
    }

    public List<UserEntity> getUsers() {
        List<UserEntity> users = new ArrayList<>();
        for (HelperUser hu : actors.values()) {
            users.add(hu.getUser());
        }
        return users;

    }

    // Parse the log file and create tons of Callable/Runnable with delays
    public void parse() throws IOException, ParseException {

        // We parse mutants first because attackers CAN submit tests for
        // equivalence duel
        //
        // mutant_id javaFile
        // 13022 /scratch/defender/mutants/mp/232/41/00000006/IntHashMap.java
        mutants = new HashMap<>();
        for (String line : Files.readAllLines((new File(mutantsFile)).toPath())) {
            String[] tokens = line.split("\t");
            // Skip first line
            if (!tokens[1].contains("/"))
                continue;

            String mutantID = tokens[0];
            String userID = tokens[1].split("/")[6];

            if (!actors.containsKey(userID)) {
                System.out.println("GameReplayer.parse() FOUND ATTACKER " + userID);
                actors.put(userID,
                        new HelperUser(
                                new UserEntity("Attacker" + userID, passwordEncoder.encode("test"), "Attacker" + userID + "@test.com"),
                                org.codedefenders.itests.http.utils.WebClientFactory.getNewWebClient(), "localhost", "test"));
            }
            System.out.println("GameReplayer.parse() Attack " + userID);
            mutants.put(mutantID, userID);
        }

        // parseTests
        // test_id javaFile
        // 7851 /scratch/defender/tests/mp/232/56/00000001/TestIntHashMap.java
        tests = new HashMap<>();
        for (String line : Files.readAllLines((new File(testsFile)).toPath())) {
            String[] tokens = line.split("\t");
            // Skip first line
            if (!tokens[1].contains("/"))
                continue;
            String testID = tokens[0];
            String userID = tokens[1].split("/")[6];

            // In testing mode we do not consider Equivalence duels
            if (actors.containsKey(userID) && actors.get(userID).getUser().getUsername().contains("Attacker")) {
                System.out.println("GameReplayer.parse() Skip test for resolving equivalence duel");
            } else {
                tests.put(testID, userID);

                if (!actors.containsKey(userID)) {
                    System.out.println("GameReplayer.parse() FOUND DEFENDER " + userID);
                    actors.put(userID,
                            new HelperUser(new UserEntity("Defender" + userID, passwordEncoder.encode("test"), "Defender" + userID + "@test.com"),
                                    org.codedefenders.itests.http.utils.WebClientFactory.getNewWebClient(), "localhost", "test"));
                }
            }
        }

        // Fill up the actors table

        // TargetExecution_ID Test_ID Mutant_ID Target Status Message Timestamp
        // 83661 7851 NULL COMPILE_TEST SUCCESS 2017-12-14 12:37:12
        // 83678 7853 NULL COMPILE_TEST SUCCESS 2017-12-14 12:37:57
        // 83679 NULL 12924 COMPILE_MUTANT SUCCESS 2017-12-14 12:37:58

        // The file is already ordered, so we need only to compute the relative
        // delays
        actions = new ArrayList<>();

        long startingTime = -1;

        for (String line : Files.readAllLines((new File(traceFile)).toPath())) {
            CodeDefenderAction action = null;
            String[] tokens = line.split("\t");

            if ("TargetExecution_ID".equals(tokens[0]))
                continue;

            // Identify Action
            if (TargetExecution.Target.COMPILE_TEST.name().equals(tokens[3])) {
                // Defend
                String testID = tokens[1];
                // Retrieve the file
                // src/test/resources/replay/game232/test.<testID>
                String testFile = "src/test/resources/replay/game232/test." + testID;
                String userID = tests.get(testID);

                if (userID == null) {
                    // Those are the tests which correspond to equivalence duels
                    continue;
                    // throw new NullPointerException("Cannot find user for TEST
                    // " + testID );
                }

                // Create corresponding action
                action = new DefendAction(userID, testFile);

            } else if (TargetExecution.Target.COMPILE_MUTANT.name().equals(tokens[3])) {
                // Attack
                String mutantID = tokens[2];
                //
                // src/test/resources/replay/game232/mutant.<mutantID>
                //
                String mutantFile = "src/test/resources/replay/game232/mutant." + mutantID;
                String userID = mutants.get(mutantID);

                // Create corresponding action
                action = new AttackAction(userID, mutantFile);
            } else {
                System.out.println("GameReplayer.parse() ACTION NOT FOUND " + tokens[3]);
                continue;
            }
            // 2017-12-14 12:37:12
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long date = fmt.parse(tokens[6]).getTime();

            // Compute delay
            if (actions.isEmpty()) {
                action.setDelay(0);
                // parse date
                startingTime = date;
            } else {
                action.setDelay(date - startingTime);
            }
            System.out.println(action.getUserId() + " - " + action.getPayload() + " - " + action.getDelay());
            actions.add(action);
        }
    }

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    // Raise assertions ?
    public void replay(int speedUp) throws FailingHttpStatusCodeException, IOException, InterruptedException {
        // Create a game -> we need the game ID
        UserEntity creatorUser = new UserEntity("creator", passwordEncoder.encode("test"), "creator@test.com");
        HelperUser creator = new HelperUser(creatorUser, WebClientFactory.getNewWebClient(), "localhost", "test");

        // This fails if the user exists ?
        creator.doRegister();
        System.out.println("GameReplayer.replay() Creator Register");
        creator.doLogin();
        System.out.println("Creator Login");

        int classID = creator.uploadClass(new File(cutFile));

        System.out.println("Class ID = " + classID);

        int mpGameId = creator.createNewGame(classID);
        System.out.println("Creator Create new Game: " + mpGameId);

        //
        creator.startGame(mpGameId);

        // Login and Join all the users
        for (HelperUser user : actors.values()) {
            // TODO
            user.doRegister();
            //
            user.doLogin();
            System.out.println(user.getUser().getUsername() + " Login");
            //
            user.joinOpenGame(mpGameId, user.getUser().getUsername().startsWith("Attacker"));

            System.out.println(user.getUser().getUsername() + "Join game " + mpGameId);
        }

        // Starting the execution

        ScheduledExecutorService scheduledExecutors = Executors.newScheduledThreadPool(actors.size());

        // Schedule all the actions passing the gameID in
        final int total = actions.size();
        for (int i = 0; i < total; i++) {
            // for (CodeDefenderAction action : actions) {
            final int index = i;
            CodeDefenderAction action = actions.get(i);
            System.out.println("GameReplayer.replay() Scheduling action for " + action.getUserId() + " after "
                    + ((action.getDelay() + INITIAL_DELAY_MILLIS) / speedUp));
            scheduledExecutors.schedule(() -> {
                HelperUser user = actors.get(action.getUserId());

                if (action instanceof DefendAction) {
                    try {
                        System.out.println((index + 1) + "/" + total + ") Defend " + user.getUser().getUsername()
                                + " using " + action.getPayload());
                        user.defend(mpGameId, readFile(action.getPayload(), Charset.defaultCharset()));
                    } catch (FailingHttpStatusCodeException | IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    try {
                        System.out.println((index + 1) + "/" + total + ") Attack " + user.getUser().getUsername()
                                + " using " + action.getPayload());
                        user.attack(mpGameId, readFile(action.getPayload(), Charset.defaultCharset()));
                    } catch (FailingHttpStatusCodeException | IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }, (action.getDelay() + INITIAL_DELAY_MILLIS) / speedUp, TimeUnit.MILLISECONDS);
        }
        // Wait for completion
        scheduledExecutors.shutdown();
        scheduledExecutors.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public static void main(String[] args) {
        GameReplayer gr = new GameReplayer(
                "/Users/gambi/CodeDefenders/src/test/resources/replay/game232/IntHashMap.java",
                "/Users/gambi/CodeDefenders/src/test/resources/replay/game232/game232.trace", //.short", // .short",
                "/Users/gambi/CodeDefenders/src/test/resources/replay/game232/game232.tests",
                "/Users/gambi/CodeDefenders/src/test/resources/replay/game232/game232.mutants");

        try {
            System.out.println("GameReplayer.main() Parsing");
            gr.parse();
            System.out.println("GameReplayer.main() Parsed");
            System.out.println("GameReplayer.main() Replaying");
            gr.replay(20);
            System.out.println("GameReplayer.main() Replayed");

        } catch (IOException | InterruptedException | FailingHttpStatusCodeException | ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
