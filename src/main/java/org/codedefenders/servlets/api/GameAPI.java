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
package org.codedefenders.servlets.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.game.MeleeScoreboardBean;
import org.codedefenders.beans.game.ScoreItem;
import org.codedefenders.beans.game.ScoreboardBean;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.dto.api.AttackerScore;
import org.codedefenders.dto.api.DefenderScore;
import org.codedefenders.dto.api.DuelsCount;
import org.codedefenders.dto.api.MeleeScore;
import org.codedefenders.dto.api.MeleeScoreboard;
import org.codedefenders.dto.api.MultiplayerScoreboard;
import org.codedefenders.dto.api.MutantsCount;
import org.codedefenders.dto.api.TestsCount;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.admin.api.GetUserTokenAPI;
import org.codedefenders.servlets.util.APIUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MissingRequiredPropertiesException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This {@link HttpServlet} offers an API for {@link Test tests}.
 *
 * <p>A {@code GET} request with the {@code testId} parameter results in a JSON string containing
 * test information, including the source code.
 *
 * <p>Serves on path: {@code /api/test}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet("/api/game")
public class GameAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GetUserTokenAPI.class);
    final Map<String, Class<?>> parameterTypes = new HashMap<String, Class<?>>() {
        {
            put("gameId", Integer.class);
        }
    };
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameService gameService;
    @Inject
    SettingsRepository settingsRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserService userService;
    @Inject
    ScoreboardBean scoreboardBean;
    @Inject
    MeleeScoreboardBean meleeScoreboardBean;
    @Inject
    private ScoreCalculator scoreCalculator;

    private static int[] slashStringToArray(String s) {
        return Arrays.stream(s.split(" / ")).mapToInt(Integer::valueOf).toArray();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Map<String, Object> params;
        try {
            params = APIUtils.getParametersOrRespondJsonError(request, response, parameterTypes);
        } catch (MissingRequiredPropertiesException e) {
            return;
        }
        final Integer gameId = (Integer) params.get("gameId");
        AbstractGame abstractGame = GameDAO.getGame(gameId);
        if (abstractGame == null) {
            APIUtils.respondJsonError(response, "Game with ID " + gameId + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else {
            List<Object> outTemp = new ArrayList<>();
            Gson gson = new Gson();
            JsonElement scoreboardJson;
            if (abstractGame instanceof MultiplayerGame) {
                MultiplayerScoreboard scoreboard = new MultiplayerScoreboard();
                MultiplayerGame game = (MultiplayerGame) abstractGame;
                scoreboardBean.setGameId(game.getId());
                scoreboardBean.setScores(game.getMutantScores(), game.getTestScores());
                scoreboardBean.setPlayers(game.getAttackerPlayers(), game.getDefenderPlayers());
                Map<Integer, PlayerScore> mutantScores = scoreboardBean.getMutantsScores();
                Map<Integer, PlayerScore> testScores = scoreboardBean.getTestScores();
                final List<Player> attackers = scoreboardBean.getAttackers();
                final List<Player> defenders = scoreboardBean.getDefenders();

                int[] mki;
                int[] mdi;
                //battleground attackers
                PlayerScore zeroDummyScore = new PlayerScore(-1);
                zeroDummyScore.setMutantKillInformation("0 / 0 / 0");
                zeroDummyScore.setDuelInformation("0 / 0 / 0");
                for (Player attacker : attackers) {
                    int playerId = attacker.getId();
                    UserEntity attackerUser = attacker.getUser();
                    if (attackerUser.getId() == Constants.DUMMY_ATTACKER_USER_ID && MutantDAO.getMutantsByGameAndUser(scoreboardBean.getGameId(), attackerUser.getId()).isEmpty()) {
                        continue;
                    }

                    PlayerScore mutantsScore = mutantScores.getOrDefault(playerId, zeroDummyScore);
                    PlayerScore testsScore = testScores.getOrDefault(playerId, zeroDummyScore);
                    mki = slashStringToArray(mutantsScore.getMutantKillInformation());
                    mdi = slashStringToArray(mutantsScore.getDuelInformation());
                    scoreboard.addAttacker(new AttackerScore(attackerUser.getUsername(), attackerUser.getId(), playerId, mutantsScore.getTotalScore() + testsScore.getTotalScore(),
                            new DuelsCount(mdi[0], mdi[1], mdi[2]), new MutantsCount(mki[0], mki[1], mki[2])));
                }

                //battleground attacker total
                mki = slashStringToArray(mutantScores.getOrDefault(-1, zeroDummyScore).getMutantKillInformation());
                mdi = slashStringToArray(mutantScores.getOrDefault(-1, zeroDummyScore).getDuelInformation());
                scoreboard.setAttackersTotal(new AttackerScore("Total", -1, -1,
                        mutantScores.getOrDefault(-1, zeroDummyScore).getTotalScore() + testScores.getOrDefault(-2, zeroDummyScore).getTotalScore(),
                        new DuelsCount(mdi[0], mdi[1], mdi[2]), new MutantsCount(mki[0], mki[1], mki[2])));

                //battleground defenders
                for (Player defender : defenders) {
                    int playerId = defender.getId();
                    UserEntity defenderUser = defender.getUser();

                    if (defenderUser.getId() == Constants.DUMMY_DEFENDER_USER_ID && TestDAO.getTestsForGameAndUser(scoreboardBean.getGameId(), defenderUser.getId()).isEmpty()) {
                        continue;
                    }

                    PlayerScore testsScore = testScores.getOrDefault(playerId, zeroDummyScore);
                    int killing = Integer.parseInt(testsScore.getMutantKillInformation());
                    mdi = slashStringToArray(testsScore.getDuelInformation());
                    scoreboard.addDefender(
                            new DefenderScore(defenderUser.getUsername(), defenderUser.getId(), playerId, testsScore.getTotalScore(), new DuelsCount(mdi[0], mdi[1], mdi[2]),
                                    new TestsCount(killing, testsScore.getQuantity() - killing)));
                }

                //battleground defenders total
                int killing = Integer.parseInt(testScores.getOrDefault(-1, zeroDummyScore).getMutantKillInformation());
                mdi = slashStringToArray(testScores.getOrDefault(-1, zeroDummyScore).getDuelInformation());
                scoreboard.setDefendersTotal(new DefenderScore("Total", -1, -1, testScores.getOrDefault(-1, zeroDummyScore).getTotalScore(), new DuelsCount(mdi[0], mdi[1], mdi[2]),
                        new TestsCount(killing, testScores.getOrDefault(-1, zeroDummyScore).getQuantity() - killing)));
                scoreboardJson = gson.toJsonTree(scoreboard);
            } else if (abstractGame instanceof MeleeGame) {
                MeleeGame game = (MeleeGame) abstractGame;
                MeleeScoreboard scoreboard = new MeleeScoreboard();
                meleeScoreboardBean.setGameId(game.getId());
                meleeScoreboardBean.setScores(scoreCalculator.getMutantScores(game.getId()), scoreCalculator.getTestScores(game.getId()),
                        scoreCalculator.getDuelScores(game.getId()));
                meleeScoreboardBean.setPlayers(game.getPlayers());
                for (ScoreItem scoreItem : meleeScoreboardBean.getSortedScoreItems()) {
                    scoreboard.addPlayer(new MeleeScore(scoreItem.getUser().getName(), scoreItem.getUser().getId(), scoreItem.getAttackScore().getPlayerId(),
                            scoreItem.getAttackScore().getTotalScore() + scoreItem.getDefenseScore().getTotalScore() + scoreItem.getDuelScore().getTotalScore(),
                            scoreItem.getAttackScore().getTotalScore(), scoreItem.getDefenseScore().getTotalScore(), scoreItem.getDuelScore().getTotalScore()));
                }
                scoreboardJson = gson.toJsonTree(scoreboard);
            } else {
                APIUtils.respondJsonError(response, "Specified game is neither battleground nor melee");
                return;
            }
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            JsonObject root = new JsonObject();
            root.add("classId", gson.toJsonTree(abstractGame.getClassId(),Integer.class));
            root.add("state", gson.toJsonTree(abstractGame.getState()));
            root.add("mutants", gson.toJsonTree("TODO",String.class)); //TODO
            root.add("tests", gson.toJsonTree("TODO",String.class)); //TODO
            root.add("scoreboard", scoreboardJson);
            out.print(new Gson().toJson(root));
            out.flush();
        }
    }
}
