package org.codedefenders.game.tcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.execution.KillMap.KillMapEntry.Status;
import org.codedefenders.game.tcs.impl.KillCountTestCaseSelector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KillmapDAO.class, GameDAO.class})
public class KillCountTestCaseSelectorTest {

    /*
     * Default values for unused variables
     */
    int classId = 0;
    int gameId = 0;
    String javaFile = null;
    String classFile = null;
    int roundCreated = 0;
    int mutantsKilled = 0;
    int playerId = 0;
    List<Integer> linesCovered = Arrays.asList(1);
    List<Integer> linesUncovered = Arrays.asList(1);
    int score = 0;

    @Test
    public void testSorting() throws Exception {
        PowerMockito.mockStatic(GameDAO.class);
        PowerMockito.mockStatic(KillmapDAO.class);


        PowerMockito.when(GameDAO.class, "getCurrentRound", gameId).thenReturn(0);


        List<KillMapEntry> killMapEntriesForClass = new ArrayList<>();


        // Test calss is UNTESTABLE ! it requires the Database !
        org.codedefenders.game.Test t1 = new org.codedefenders.game.Test(1, classId, gameId, javaFile, classFile,
                roundCreated, mutantsKilled, playerId, linesCovered, linesUncovered, score);
        org.codedefenders.game.Test t2 = new org.codedefenders.game.Test(2, classId, gameId, javaFile, classFile,
                roundCreated, mutantsKilled, playerId, linesCovered, linesUncovered, score);
        org.codedefenders.game.Test t3 = new org.codedefenders.game.Test(3, classId, gameId, javaFile, classFile,
                roundCreated, mutantsKilled, playerId, linesCovered, linesUncovered, score);

        // What happens if we get different instances of the same test?
        org.codedefenders.game.Test t31 = new org.codedefenders.game.Test(3, classId, gameId, javaFile, classFile,
                roundCreated, mutantsKilled, playerId, linesCovered, linesUncovered, score);

        // t1 covers 3 killed one mutant
        killMapEntriesForClass.add(new KillMapEntry(t1, null, Status.KILL));
        killMapEntriesForClass.add(new KillMapEntry(t1, null, Status.KILL));
        killMapEntriesForClass.add(new KillMapEntry(t1, null, Status.NO_KILL));
        // t2 covers 1 killed no mutants
        killMapEntriesForClass.add(new KillMapEntry(t2, null, Status.NO_KILL));
        // t3 covers 5 kills 3 mutants
        killMapEntriesForClass.add(new KillMapEntry(t3, null, Status.KILL));
        killMapEntriesForClass.add(new KillMapEntry(t31, null, Status.KILL));
        killMapEntriesForClass.add(new KillMapEntry(t31, null, Status.KILL));
        killMapEntriesForClass.add(new KillMapEntry(t3, null, Status.NO_KILL));
        killMapEntriesForClass.add(new KillMapEntry(t3, null, Status.NO_KILL));


        // Return the configured list
        PowerMockito.when(KillmapDAO.class, "getKillMapEntriesForClass", classId).thenReturn(killMapEntriesForClass);

        KillCountTestCaseSelector cut = new KillCountTestCaseSelector();

        List<org.codedefenders.game.Test> allTests = Arrays.asList(t1, t2, t3);

        int maxTests = 10;

        List<org.codedefenders.game.Test> sortedAndselectedTests = cut.select(allTests, maxTests);

        Assert.assertEquals(3, sortedAndselectedTests.size());
    }
}
