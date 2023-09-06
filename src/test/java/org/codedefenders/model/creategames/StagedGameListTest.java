package org.codedefenders.model.creategames;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class StagedGameListTest {
    private final GameSettings defaultSettings = new GameSettings(GameType.MULTIPLAYER, -1, false, false, 3,
            CodeValidatorLevel.MODERATE, true, false, 0, GameLevel.HARD, Role.OBSERVER, 60, false, null);

    private StagedGameList stagedGameList;

    @BeforeEach
    public void before() {
        stagedGameList = new StagedGameList();
    }

    @Test
    public void testStagedGameIds() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);

        assertThat(stagedGame1.getId(), not(equalTo(stagedGame2.getId())));
    }

    @Test
    public void testGetStagedGame() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);

        assertThat(stagedGameList.getGame(stagedGame1.getId()), equalTo(stagedGame1));
        assertThat(stagedGameList.getGame(stagedGame2.getId()), equalTo(stagedGame2));
        assertThat(stagedGameList.getGame(Integer.MAX_VALUE), nullValue());
    }

    @Test
    public void testGetStagedGames() {
        assertThat(stagedGameList.getMap().values(), empty());

        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        assertThat(stagedGameList.getMap().values(), containsInAnyOrder(stagedGame1));

        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);
        assertThat(stagedGameList.getMap().values(), containsInAnyOrder(stagedGame1, stagedGame2));
    }

    @Test
    public void testRemoveStagedGame() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);

        assertThat(stagedGameList.removeStagedGame(stagedGame1.getId()), is(true));
        assertThat(stagedGameList.getMap().values(), containsInAnyOrder(stagedGame2));
        assertThat(stagedGameList.removeStagedGame(stagedGame1.getId()), is(false));

        assertThat(stagedGameList.removeStagedGame(stagedGame2.getId()), is(true));
        assertThat(stagedGameList.getMap().values(), empty());
        assertThat(stagedGameList.removeStagedGame(stagedGame2.getId()), is(false));
    }

    @Test
    public void testAddUsers() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);

        /* Unassigned user. */
        assertThat(stagedGame1.addAttacker(0), is(true));
        assertThat(stagedGame1.addDefender(1), is(true));

        /* Assigned user. */
        assertThat(stagedGame1.addAttacker(0), is(false));
        assertThat(stagedGame1.addDefender(0), is(false));

        /* Unassigned user. */
        assertThat(stagedGame2.addAttacker(2), is(true));
        assertThat(stagedGame2.addDefender(3), is(true));

        /* Assigned user to other game. */
        assertThat(stagedGame2.addAttacker(0), is(false));
        assertThat(stagedGame2.addDefender(0), is(false));
    }

    @Test
    public void testGetPlayers() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);

        stagedGame1.addAttacker(0);
        stagedGame1.addDefender(1);

        stagedGame2.addAttacker(2);
        stagedGame2.addDefender(3);

        assertThat(stagedGame1.getAttackers(), containsInAnyOrder(0));
        assertThat(stagedGame1.getDefenders(), containsInAnyOrder(1));
        assertThat(stagedGame1.getPlayers(), containsInAnyOrder(0, 1));

        assertThat(stagedGame2.getAttackers(), containsInAnyOrder(2));
        assertThat(stagedGame2.getDefenders(), containsInAnyOrder(3));
        assertThat(stagedGame2.getPlayers(), containsInAnyOrder(2, 3));
    }

    @Test
    public void testRemovePlayers() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);

        stagedGame1.addAttacker(0);
        stagedGame1.addDefender(1);
        stagedGame2.addAttacker(2);
        stagedGame2.addDefender(3);

        /* Unassigned user. */
        assertThat(stagedGame1.removePlayer(7), is(false));

        /* User assigned to other game. */
        assertThat(stagedGame1.removePlayer(2), is(false));

        /* Assigned user. */
        assertThat(stagedGame1.removePlayer(0), is(true));

        /* No longer assigned user. */
        assertThat(stagedGame1.removePlayer(0), is(false));

        assertThat(stagedGame1.getAttackers(), empty());
        assertThat(stagedGame1.getDefenders(), containsInAnyOrder(1));
        assertThat(stagedGame1.getPlayers(), containsInAnyOrder(1));

        assertThat(stagedGame1.removePlayer(1), is(true));
        assertThat(stagedGame1.getAttackers(), empty());
        assertThat(stagedGame1.getDefenders(), empty());
        assertThat(stagedGame1.getPlayers(), empty());
    }

    @Test
    public void testGetAssignedUsers() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);

        stagedGame1.addAttacker(0);
        stagedGame1.addDefender(1);
        assertThat(stagedGameList.getAssignedUsers(), containsInAnyOrder(0, 1));

        stagedGame2.addAttacker(2);
        stagedGame2.addDefender(3);
        assertThat(stagedGameList.getAssignedUsers(), containsInAnyOrder(0, 1, 2, 3));

        stagedGame1.removePlayer(0);
        stagedGame1.removePlayer(1);
        assertThat(stagedGameList.getAssignedUsers(), containsInAnyOrder(2, 3));

        stagedGame2.removePlayer(2);
        stagedGame2.removePlayer(3);
        assertThat(stagedGameList.getAssignedUsers(), empty());
    }
}
