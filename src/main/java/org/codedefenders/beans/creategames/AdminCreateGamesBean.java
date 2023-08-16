package org.codedefenders.beans.creategames;

import java.util.Set;
import java.util.stream.Collectors;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.CreateGamesService;

public class AdminCreateGamesBean extends CreateGamesBean<CreateGamesBean.UserInfo> {
    public AdminCreateGamesBean(StagedGameList stagedGames,
                                MessagesBean messages,
                                EventDAO eventDAO,
                                UserRepository userRepo,
                                CreateGamesService createGamesService) {
        super(stagedGames, messages, eventDAO, userRepo, createGamesService);
    }

    @Override
    protected Set<UserInfo> fetchUserInfos() {
        return AdminDAO.getAllUsersInfo().stream()
                .map(userInfo -> new UserInfo(
                        userInfo.getUser().getId(),
                        userInfo.getUser().getUsername(),
                        userInfo.getUser().getEmail(),
                        userInfo.getLastLogin(),
                        userInfo.getLastRole(),
                        userInfo.getTotalScore()))
                .collect(Collectors.toSet());
    }

    @Override
    protected Set<Integer> fetchAvailableMultiplayerGames() {
        return MultiplayerGameDAO.getAvailableMultiplayerGames().stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toSet());
    }

    @Override
    protected Set<Integer> fetchAvailableMeleeGames() {
        return MeleeGameDAO.getAvailableMeleeGames().stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toSet());
    }
}
