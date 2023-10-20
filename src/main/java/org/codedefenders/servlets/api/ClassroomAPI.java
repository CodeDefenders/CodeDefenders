package org.codedefenders.servlets.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.model.Player;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@WebServlet(Paths.API_CLASSROOM)
public class ClassroomAPI extends HttpServlet {

    @Inject
    private ClassroomService classroomService;
    @Inject
    private UserService userService;
    @Inject
    private CodeDefendersAuth login;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        Optional<String> type = ServletUtils.getStringParameter(request, "type");
        if (type.isEmpty()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        switch (type.get()) {
            case "classrooms":
                handleClassrooms(request, response);
                return;
            case "members":
                handleMembers(request, response);
                return;
            case "games":
                handleGames(request, response);
                return;
            default:
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
        }
    }

    private Optional<Classroom> getClassroomFromRequest(HttpServletRequest request) {
        Optional<Classroom> classroomById = ServletUtils.getIntParameter(request, "classroomId")
                .flatMap(classroomService::getClassroomById);
        if (classroomById.isPresent()) {
            return classroomById;
        }

        return ServletUtils.getUUIDParameter(request, "classroomUid")
                .flatMap(classroomService::getClassroomByUUID);
    }

    private void handleClassrooms(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String which = ServletUtils.getStringParameter(request, "which").orElse("all");

        Optional<List<ClassroomDTO>> classrooms = getClassroomsData(which);
        if (classrooms.isEmpty()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
        gson.toJson(classrooms.get(), response.getWriter());
    }

    private void handleMembers(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (classroom.isEmpty()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        List<ClassroomMemberDTO> members = getMembersData(classroom.get());

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
        gson.toJson(members, response.getWriter());
    }

    private void handleGames(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (classroom.isEmpty()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        List<ClassroomGameDTO> games = getGamesData(classroom.get());

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
        gson.toJson(games, response.getWriter());
    }

    private Optional<List<ClassroomDTO>> getClassroomsData(String which) {
        return getClassrooms(which).map(this::addClassroomMemberCounts);
    }

    private Optional<List<Classroom>> getClassrooms(String which) {
        switch (which) {
            case "all":
                if (!login.isAdmin()) {
                    return Optional.empty();
                }
                return Optional.of(classroomService.getAllClassrooms());
            case "visible":
                return Optional.of(classroomService.getVisibleClassrooms());
            case "user":
                return Optional.of(classroomService.getAllClassroomsByMember(login.getUserId()));
            default:
                return Optional.empty();
        }
    }

    private List<ClassroomDTO> addClassroomMemberCounts(List<Classroom> classrooms) {
        return classrooms.stream()
                .map(classroom -> {
                    List<ClassroomMember> members = classroomService.getMembersForClassroom(classroom.getId());
                    return new ClassroomDTO(classroom, members.size());
                })
                .collect(Collectors.toList());
    }

    private List<ClassroomMemberDTO> getMembersData(Classroom classroom) {
        List<ClassroomMember> members = classroomService.getMembersForClassroom(classroom.getId());

        return members.stream()
                .map(member -> {
                    SimpleUser user = userService.getSimpleUserById(member.getUserId())
                            .orElseThrow(() -> new IllegalStateException("Non-existing user is part of this classroom."));
                    return new ClassroomMemberDTO(user, member.getRole());
                })
                .collect(Collectors.toList());
    }

    private List<ClassroomGameDTO> getGamesData(Classroom classroom) {
        List<AbstractGame> games = new ArrayList<>();
        games.addAll(MultiplayerGameDAO.getClassroomGames(classroom.getId()));
        games.addAll(MeleeGameDAO.getClassroomGames(classroom.getId()));

        return games.stream()
                .map(game -> {
                    Player player = PlayerDAO.getPlayerForUserAndGame(login.getUserId(), game.getId());
                    Role role;
                    if (player != null) {
                        role = player.getRole();
                    } else if (game.getCreatorId() == login.getUserId()) {
                        role = Role.OBSERVER;
                    } else {
                        role = Role.NONE;
                    }
                    return new ClassroomGameDTO(game, role);
                })
                .collect(Collectors.toList());
    }

    private static class ClassroomDTO extends Classroom {
        @Expose
        private final int memberCount;

        public ClassroomDTO(Classroom classroom, int memberCount) {
            super(classroom);
            this.memberCount = memberCount;
        }
    }

    private static class ClassroomMemberDTO {
        @Expose
        private final SimpleUser user;

        @Expose
        private final ClassroomRole role;

        public ClassroomMemberDTO(SimpleUser user, ClassroomRole role) {
            this.user = user;
            this.role = role;
        }
    }

    private static class ClassroomGameDTO {
        @Expose
        private final int gameId;

        @Expose
        private final GameMode mode;

        @Expose
        private final GameState state;

        @Expose
        private final Role role;

        @Expose
        private final Long startTime;

        @Expose
        private final Integer duration;

        public ClassroomGameDTO(AbstractGame game, Role role) {
            this.gameId = game.getId();
            this.mode = game.getMode();
            this.state = game.getState();
            this.role = role;

            if (game instanceof MeleeGame) {
                this.startTime = ((MeleeGame) game).getStartTimeUnixSeconds();
                this.duration = ((MeleeGame) game).getGameDurationMinutes();
            } else if (game instanceof MultiplayerGame) {
                this.startTime = ((MultiplayerGame) game).getStartTimeUnixSeconds();
                this.duration = ((MultiplayerGame) game).getGameDurationMinutes();
            } else {
                this.startTime = null;
                this.duration = null;
            }
        }
    }
}
