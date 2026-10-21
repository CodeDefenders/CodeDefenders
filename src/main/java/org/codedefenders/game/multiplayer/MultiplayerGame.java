/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.game.multiplayer;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.WhitelistType;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MultiplayerGameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.persistence.database.WhitelistRepository;
import org.codedefenders.util.CDIUtil;

import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.game.Mutant.Equivalence.DECLARED_YES;
import static org.codedefenders.game.Mutant.Equivalence.PENDING_TEST;
import static org.codedefenders.game.Mutant.Equivalence.PROVEN_NO;

public class MultiplayerGame extends AbstractMultiplayerGame {

    /*
     * Inherited from AbstractGame
     *
     * protected GameClass cut; protected int id; protected int classId; protected
     * int creatorId; protected GameState state; protected GameLevel level;
     * protected GameMode mode; protected ArrayList<Event> events; protected
     * List<Mutant> mutants; protected List<Test> tests;
     *
     *
     * Inherited from AbstractMultiplayerGame
     *
     * protected float lineCoverage;
     * protected float mutantCoverage;
     * protected float prize;
     * protected boolean chatEnabled;
     * protected int gameDurationMinutes;
     * protected long startTimeUnixSeconds;
     * protected long finishTimeUnixSeconds;
     * protected int automaticMutantEquivalenceThreshold = 0;
     * protected Integer classroomId;
     */

    private List<Player> attackers;
    private List<Player> defenders;
    private int defenderValue;
    private int attackerValue;

    private boolean mayChooseRoles = true;


    public static class Builder extends AbstractMultiplayerGame.Builder<Builder, MultiplayerGame> {
        private List<Player> attackers = null;
        private List<Player> defenders = null;
        private boolean mayChooseRoles = true;
        private int defenderValue = 100;
        private int attackerValue = 100;

        public Builder(int classId, int creatorId, int maxAssertionsPerTest) {
            super(classId, creatorId, maxAssertionsPerTest);
        }

        public Builder defenderValue(int defenderValue) {
            this.defenderValue = defenderValue;
            return this;
        }

        public Builder attackerValue(int attackerValue) {
            this.attackerValue = attackerValue;
            return this;
        }

        public Builder attackers(List<Player> attackers) {
            this.attackers = attackers;
            return this;
        }

        public Builder defenders(List<Player> defenders) {
            this.defenders = defenders;
            return this;
        }

        public Builder mayChooseRoles(boolean mayChooseRoles) {
            this.mayChooseRoles = mayChooseRoles;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public MultiplayerGame build() {
            return new MultiplayerGame(this);
        }
    }

    private MultiplayerGame(Builder builder) {
        super(builder);
        this.mode = GameMode.PARTY;
        this.attackers = builder.attackers;
        this.defenders = builder.defenders;
        this.defenderValue = builder.defenderValue;
        this.attackerValue = builder.attackerValue;
        this.mayChooseRoles = builder.mayChooseRoles;
    }

    public int getDefenderValue() {
        return defenderValue;
    }

    public int getAttackerValue() {
        return attackerValue;
    }

    public int getAutomaticMutantEquivalenceThreshold() {
        return automaticMutantEquivalenceThreshold;
    }

    public Optional<Integer> getClassroomId() {
        return Optional.ofNullable(classroomId);
    }

    public Role getRole(int userId) {
        if (getDefenderPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.DEFENDER;
        } else if (getAttackerPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.ATTACKER;
        } else if (getObserverPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.OBSERVER;
        } else {
            return Role.NONE;
        }
    }

    @Override
    public boolean isUserAuthorized(int userId) {
        return getRole(userId) != Role.NONE;
    }

    public boolean isMayChooseRoles() {
        return mayChooseRoles;
    }

    public List<Player> getDefenderPlayers() {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        if (defenders == null) {
            defenders = gameRepo.getPlayersForGame(getId(), Role.DEFENDER);
        }
        return defenders;
    }

    public List<Player> getAttackerPlayers() {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        if (attackers == null) {
            attackers = gameRepo.getPlayersForGame(getId(), Role.ATTACKER);
        }
        return attackers;
    }

    /**
     * Adds a player to the game.
     * @param userId The id of the player to add.
     * @param role The requested role. If {@code null},
     *            the player will be assigned to the side with fewer players.
     *             If the user is the creator, the role will never be changed.
     * @return {@code true} if the player was added successfully, {@code false} otherwise.
     */
    @Override
    public boolean addPlayer(int userId, Role role) {
        if (userId != creatorId && !mayChooseRoles || role == null) {
            WhitelistRepository whitelistRepo = CDIUtil.getBeanFromCDI(WhitelistRepository.class);
            WhitelistType whitelistType = whitelistRepo.getWhitelistType(id, userId);
            if (whitelistType == null
                    || whitelistType == WhitelistType.FLEX
                    || (whitelistType == WhitelistType.CHOICE) && role == null) {
                role = getAttackerPlayers().size() > getDefenderPlayers().size() ? Role.DEFENDER : Role.ATTACKER;
            } else if (whitelistType == WhitelistType.ATTACKER) {
                role = Role.ATTACKER;
            } else if (whitelistType == WhitelistType.DEFENDER) {
                role = Role.DEFENDER;
            }
            //If whitelist type is choice and role is set, keep role untouched.
        }
        return canJoinGame(userId) && addPlayerForce(userId, role);
    }

    public boolean addPlayerForce(int userId, Role role) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);
        EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);
        UserRepository userRepo = CDIUtil.getBeanFromCDI(UserRepository.class);

        if (state == GameState.FINISHED && role != Role.OBSERVER) {
            return false;
        }

        if (!gameRepo.addPlayerToGame(id, userId, role)) {
            return false;
        }

        // Do not add events for observers joining
        if (role == Role.OBSERVER) {
            return true;
        }

        Optional<UserEntity> u = userRepo.getUserById(userId);
        EventType et = role == Role.ATTACKER ? EventType.ATTACKER_JOINED : EventType.DEFENDER_JOINED;
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        Event e = new Event(-1, id, userId, u.map(UserEntity::getUsername).orElse("") + " joined the game as " + role, et, EventStatus.GAME,
                timestamp);
        eventDAO.insert(e);

        Event notif = new Event(-1, id, userId, "You joined a game as " + role, et, EventStatus.NEW, timestamp);
        eventDAO.insert(notif);

        return true;
    }

    @Override
    public boolean insert() {
        var multiplayerGameRepo = CDIUtil.getBeanFromCDI(MultiplayerGameRepository.class);
        try {
            this.id = multiplayerGameRepo.storeMultiplayerGame(this);
            return true;
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store multiplayer game to database.", e);
            return false;
        }
    }

    @Override
    public boolean update() {
        var multiplayerGameRepo = CDIUtil.getBeanFromCDI(MultiplayerGameRepository.class);
        return multiplayerGameRepo.updateMultiplayerGame(this);
    }

    public WhitelistType getWhitelistTypeOfUser(int userId) {
        WhitelistRepository whitelistRepo = CDIUtil.getBeanFromCDI(WhitelistRepository.class);
        return whitelistRepo.getWhitelistType(id, userId);
    }

}
