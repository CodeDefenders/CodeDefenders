package org.codedefenders.beans.creategames;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.service.CreateGamesService;

import com.google.gson.annotations.Expose;

public class ClassroomCreateGamesBean extends CreateGamesBean<ClassroomCreateGamesBean.UserInfo> {
    private final ClassroomService classroomService;
    private final int classroomId;

    public ClassroomCreateGamesBean(int classroomId,
                                    StagedGameList stagedGames,
                                    MessagesBean messages,
                                    EventDAO eventDAO,
                                    UserRepository userRepo,
                                    CreateGamesService createGamesService,
                                    ClassroomService classroomService) {
        super(stagedGames, messages, eventDAO, userRepo, createGamesService);
        this.classroomService = classroomService;
        this.classroomId = classroomId;
    }

    @Override
    protected Set<UserInfo> fetchUserInfos() {
        List<ClassroomMember> members = classroomService.getMembersForClassroom(classroomId);
        Map<Integer, ClassroomMember> membersMap = members.stream()
                .collect(Collectors.toMap(
                        ClassroomMember::getUserId,
                        member -> member
                ));
        return AdminDAO.getUsersInfo(membersMap.keySet()).stream()
                .map(userInfo -> new UserInfo(
                        userInfo.getUser().getId(),
                        userInfo.getUser().getUsername(),
                        userInfo.getUser().getEmail(),
                        userInfo.getLastLogin(),
                        userInfo.getLastRole(),
                        userInfo.getTotalScore(),
                        membersMap.get(userInfo.getUser().getId()).getRole()))
                .collect(Collectors.toSet());
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
}
