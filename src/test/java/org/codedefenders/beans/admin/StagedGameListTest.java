package org.codedefenders.beans.admin;

import java.util.ArrayList;
import java.util.HashMap;

import org.codedefenders.beans.admin.StagedGameList.GameSettings;
import org.codedefenders.beans.admin.StagedGameList.StagedGame;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.model.User;
import org.codedefenders.model.UserInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AdminDAO.class})
public class StagedGameListTest {

    private StagedGameList stagedGameList;
    private HashMap<Integer, UserInfo> userInfos;

    @Before
    public void before() {
        userInfos = new HashMap<>();
        userInfos.put(0, new UserInfo(new User(0, "userA", "", ""), null, null, 0));
        userInfos.put(1, new UserInfo(new User(1, "userB", "", ""), null, null, 0));
        userInfos.put(2, new UserInfo(new User(2, "userC", "", ""), null, null, 0));
        userInfos.put(3, new UserInfo(new User(3, "userD", "", ""), null, null, 0));
        userInfos.put(4, new UserInfo(new User(4, "userE", "", ""), null, null, 0));
        userInfos.put(5, new UserInfo(new User(5, "userF", "", ""), null, null, 0));
        userInfos.put(6, new UserInfo(new User(6, "userG", "", ""), null, null, 0));
        userInfos.put(7, new UserInfo(new User(7, "userH", "", ""), null, null, 0));

        PowerMockito.mockStatic(AdminDAO.class);
        PowerMockito.when(AdminDAO.getAllUsersInfo()).thenReturn(new ArrayList<>(userInfos.values()));

        stagedGameList = new StagedGameList();
    }

    @Test
    public void testStagedGameIds() {
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());

        assertThat(stagedGame1.getId(), not(equalTo(stagedGame2.getId())));
    }

    @Test
    public void testGetStagedGame() {
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());

        assertThat(stagedGameList.getStagedGame(stagedGame1.getId()), equalTo(stagedGame1));
        assertThat(stagedGameList.getStagedGame(stagedGame2.getId()), equalTo(stagedGame2));
        assertThat(stagedGameList.getStagedGame(Integer.MAX_VALUE), nullValue());
    }

    @Test
    public void testGetStagedGames() {
        assertThat(stagedGameList.getStagedGames().values(), empty());

        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        assertThat(stagedGameList.getStagedGames().values(), containsInAnyOrder(stagedGame1));

        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());
        assertThat(stagedGameList.getStagedGames().values(), containsInAnyOrder(stagedGame1, stagedGame2));
    }

    @Test
    public void testRemoveStagedGame() {
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());

        assertThat(stagedGameList.removeStagedGame(stagedGame1.getId()), is(true));
        assertThat(stagedGameList.getStagedGames().values(), containsInAnyOrder(stagedGame2));
        assertThat(stagedGameList.removeStagedGame(stagedGame1.getId()), is(false));

        assertThat(stagedGameList.removeStagedGame(stagedGame2.getId()), is(true));
        assertThat(stagedGameList.getStagedGames().values(), empty());
        assertThat(stagedGameList.removeStagedGame(stagedGame2.getId()), is(false));
    }

    @Test
    public void testAddUsers() {
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());

        /* Existing unassigned user. */
        assertThat(stagedGame1.addAttacker(0), is(true));
        assertThat(stagedGame1.addDefender(1), is(true));

        /* Assigned user. */
        assertThat(stagedGame1.addAttacker(0), is(false));
        assertThat(stagedGame1.addDefender(0), is(false));

        /* Non-existing user. */
        assertThat(stagedGame1.addDefender(9), is(false));
        assertThat(stagedGame1.addDefender(9), is(false));

        /* Existing unassigned user. */
        assertThat(stagedGame2.addAttacker(2), is(true));
        assertThat(stagedGame2.addDefender(3), is(true));

        /* Assigned user to other game. */
        assertThat(stagedGame2.addAttacker(0), is(false));
        assertThat(stagedGame2.addDefender(0), is(false));
    }

    @Test
    public void testGetPlayers() {
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());

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
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());

        stagedGame1.addAttacker(0);
        stagedGame1.addDefender(1);
        stagedGame2.addAttacker(2);
        stagedGame2.addDefender(3);

        /* Unassigned user. */
        assertThat(stagedGame1.removePlayer(7), is(false));

        /* Non-existing user. */
        assertThat(stagedGame1.removePlayer(9), is(false));

        /* User assigned to other game. */
        assertThat(stagedGame1.removePlayer(2), is(false));

        /* Existing and assigned user. */
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
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());

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

    @Test
    public void testDetachedGame() {
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        stagedGameList.removeStagedGame(stagedGame1.getId());

        try {
            stagedGame1.addAttacker(0);
            fail("Expected assertion.");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalStateException.class));
        }

        try {
            stagedGame1.addDefender(0);
            fail("Expected assertion.");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalStateException.class));
        }

        try {
            stagedGame1.removePlayer(0);
            fail("Expected assertion.");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalStateException.class));
        }
    }
}
