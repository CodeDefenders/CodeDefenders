package org.codedefenders.beans.creategames;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.MeleeGameRepository;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.CreateGamesService;

public class AdminCreateGamesBean extends CreateGamesBean {
    private final Map<Integer, UserInfo> userInfos;
    private final Set<Integer> availableMultiplayerGames;
    private final Set<Integer> availableMeleeGames;
    private final Set<Integer> assignedUsers;

    private final UserRepository userRepo;
    private final MeleeGameRepository meleeGameRepo;

    public AdminCreateGamesBean(StagedGameList stagedGames,
                                MessagesBean messages,
                                EventDAO eventDAO,
                                UserRepository userRepo,
                                MeleeGameRepository meleeGameRepo,
                                CreateGamesService createGamesService) {
        super(stagedGames, messages, eventDAO, userRepo, createGamesService);
        this.userRepo = userRepo;
        this.meleeGameRepo = meleeGameRepo;
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
        return MultiplayerGameDAO.getAvailableMultiplayerGames().stream()
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
