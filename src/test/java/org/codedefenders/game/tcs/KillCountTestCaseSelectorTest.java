package org.codedefenders.game.tcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.execution.KillMap.KillMapEntry.Status;
import org.codedefenders.game.Test;
import org.codedefenders.game.tcs.impl.KillCountTestCaseSelector;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
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

    @org.junit.jupiter.api.Test
    public void testSorting() throws Exception {
        List<org.codedefenders.game.Test> allTests;
        org.codedefenders.game.Test t1, t2, t3, t31;

        // Test calss is UNTESTABLE ! it requires the Database !
        t1 = new Test(1, classId, gameId, javaFile, classFile,
                roundCreated, mutantsKilled, playerId, linesCovered, linesUncovered, score);
        t2 = new Test(2, classId, gameId, javaFile, classFile,
                roundCreated, mutantsKilled, playerId, linesCovered, linesUncovered, score);
        t3 = new Test(3, classId, gameId, javaFile, classFile,
                roundCreated, mutantsKilled, playerId, linesCovered, linesUncovered, score);

        // What happens if we get different instances of the same test?
        t31 = new Test(3, classId, gameId, javaFile, classFile,
                roundCreated, mutantsKilled, playerId, linesCovered, linesUncovered, score);

        allTests = Arrays.asList(t1, t2, t3);

        List<KillMapEntry> killMapEntriesForClass = new ArrayList<>();
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


        KillCountTestCaseSelector cut = new KillCountTestCaseSelector();

        try (var mockedKillmapDAO = mockStatic(KillmapDAO.class)) {
            mockedKillmapDAO.when(() -> KillmapDAO.getKillMapEntriesForClass(classId))
                    .thenReturn(killMapEntriesForClass);

            int maxTests = 10;
            List<org.codedefenders.game.Test> sortedAndSelectedTests = cut.select(allTests, maxTests);
            assertEquals(3, sortedAndSelectedTests.size());
        }
    }
}
