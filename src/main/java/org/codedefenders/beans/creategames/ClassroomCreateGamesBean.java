package org.codedefenders.beans.creategames;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.MeleeGameRepository;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.service.CreateGamesService;

import com.google.gson.annotations.Expose;

public class ClassroomCreateGamesBean extends CreateGamesBean {
    private final Map<Integer, UserInfo> userInfos;
    private final Set<Integer> availableMultiplayerGames;
    private final Set<Integer> availableMeleeGames;
    private final Set<Integer> assignedUsers;

    private final ClassroomService classroomService;
    private final UserRepository userRepo;
    private final MeleeGameRepository meleeGameRepo;
    private final int classroomId;

    public ClassroomCreateGamesBean(int classroomId,
                                    StagedGameList stagedGames,
                                    MessagesBean messages,
                                    EventDAO eventDAO,
                                    UserRepository userRepo,
                                    CreateGamesService createGamesService,
                                    ClassroomService classroomService,
                                    MeleeGameRepository meleeGameRepo) {
        super(stagedGames, messages, eventDAO, userRepo, createGamesService);
        this.classroomService = classroomService;
        this.userRepo = userRepo;
        this.meleeGameRepo = meleeGameRepo;
        this.classroomId = classroomId;
        userInfos = fetchUserInfos();
        availableMultiplayerGames = fetchAvailableMultiplayerGames();
        availableMeleeGames = fetchAvailableMeleeGames();
        assignedUsers = fetchAssignedUsers();
        synchronized (getSynchronizer()) {
            stagedGames.retainUsers(userInfos.keySet());
        }
    }

    protected Map<Integer, UserInfo> fetchUserInfos() {
        List<ClassroomMember> members = classroomService.getMembersForClassroom(classroomId);
        Map<Integer, ClassroomMember> membersMap = members.stream()
                .collect(Collectors.toMap(
                        ClassroomMember::getUserId,
                        member -> member
                ));
        return AdminDAO.getClassroomUsersInfo(classroomId).stream()
                .map(userInfo -> new UserInfo(
                        userInfo.getUser().getId(),
                        userInfo.getUser().getUsername(),
                        userInfo.getUser().getEmail(),
                        userInfo.getLastLogin(),
                        userInfo.getLastRole(),
                        userInfo.getTotalScore(),
                        membersMap.get(userInfo.getUser().getId()).getRole()))
                .collect(Collectors.toMap(
                        CreateGamesBean.UserInfo::getId,
                        userInfo -> userInfo
                ));
    }

    protected Set<Integer> fetchAvailableMultiplayerGames() {
        return MultiplayerGameDAO.getAvailableClassroomGames(classroomId).stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toSet());
    }

    protected Set<Integer> fetchAvailableMeleeGames() {
        return meleeGameRepo.getAvailableClassroomGames(classroomId).stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toSet());
    }

    protected Set<Integer> fetchAssignedUsers() {
        return userRepo.getAssignedUsersForClassroom(classroomId).stream()
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

    public static class UserInfo extends CreateGamesBean.UserInfo {
        @Expose
        private final ClassroomRole classroomRole;

        public UserInfo(int userId, String username, String email, Instant lastLogin, Role lastRole, int totalScore,
                        ClassroomRole classroomRole) {
            super(userId, username, email, lastLogin, lastRole, totalScore);
            this.classroomRole = classroomRole;
        }

        public ClassroomRole getClassroomRole() {
            return classroomRole;
        }
    }

    @Override
    public Kind getKind() {
        return Kind.CLASSROOM;
    }

    public int getClassroomId() {
        return classroomId;
    }
}
