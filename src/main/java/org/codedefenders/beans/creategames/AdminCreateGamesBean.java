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
package org.codedefenders.beans.creategames;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.MeleeGameRepository;
import org.codedefenders.persistence.database.MultiplayerGameRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.CreateGamesService;

public class AdminCreateGamesBean extends CreateGamesBean {
    private final Map<Integer, UserInfo> userInfos;
    private final Set<Integer> availableMultiplayerGames;
    private final Set<Integer> availableMeleeGames;
    private final Set<Integer> assignedUsers;

    private final UserRepository userRepo;
    private final MeleeGameRepository meleeGameRepo;
    private final MultiplayerGameRepository multiplayerGameRepo;

    public AdminCreateGamesBean(StagedGameList stagedGames,
                                MessagesBean messages,
                                EventDAO eventDAO,
                                UserRepository userRepo,
                                MeleeGameRepository meleeGameRepo,
                                MultiplayerGameRepository multiplayerGameRepo,
                                CreateGamesService createGamesService,
                                GameClassRepository gameClassRepo,
                                CodeDefendersAuth login) {
        super(stagedGames, messages, eventDAO, userRepo, createGamesService, gameClassRepo, login);
        this.userRepo = userRepo;
        this.meleeGameRepo = meleeGameRepo;
        this.multiplayerGameRepo = multiplayerGameRepo;
        userInfos = fetchUserInfos();
        availableMultiplayerGames = fetchAvailableMultiplayerGames();
        availableMeleeGames = fetchAvailableMeleeGames();
        assignedUsers = fetchAssignedUsers();
        synchronized (getSynchronizer()) {
            stagedGames.retainUsers(userInfos.keySet());
        }
    }

    protected Map<Integer, UserInfo> fetchUserInfos() {
        return AdminDAO.getAllUsersInfo().stream()
                .map(userInfo -> new UserInfo(
                        userInfo.getUser().getId(),
                        userInfo.getUser().getUsername(),
                        userInfo.getUser().getEmail(),
                        userInfo.getLastLogin(),
                        userInfo.getLastRole(),
                        userInfo.getTotalScore()))
                .collect(Collectors.toMap(
                        UserInfo::getId,
                        userInfo -> userInfo
                ));
    }

    protected Set<Integer> fetchAvailableMultiplayerGames() {
        return multiplayerGameRepo.getAvailableMultiplayerGames().stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toSet());
    }

    protected Set<Integer> fetchAvailableMeleeGames() {
        return meleeGameRepo.getAvailableMeleeGames().stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toSet());
    }

    protected Set<Integer> fetchAssignedUsers() {
        return userRepo.getAssignedUsers().stream()
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<Integer, UserInfo> getUserInfos() {
        return userInfos;
    }

    @Override
    public Set<Integer> getAvailableMultiplayerGames() {
        return availableMultiplayerGames;
    }

    @Override
    public Set<Integer> getAvailableMeleeGames() {
        return availableMeleeGames;
    }

    @Override
    public Set<Integer> getAssignedUsers() {
        return assignedUsers;
    }

    @Override
    public UserInfo getUserInfo(int userId) {
        return userInfos.get(userId);
    }

    @Override
    public Kind getKind() {
        return Kind.ADMIN;
    }
}
