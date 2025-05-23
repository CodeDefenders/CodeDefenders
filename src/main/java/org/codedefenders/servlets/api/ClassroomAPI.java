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
package org.codedefenders.servlets.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
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
import org.codedefenders.persistence.database.MeleeGameRepository;
import org.codedefenders.persistence.database.MultiplayerGameRepository;
import org.codedefenders.persistence.database.PlayerRepository;
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
    @Inject
    private MeleeGameRepository meleeGameRepo;
    @Inject
    private MultiplayerGameRepository multiplayerGameRepo;
    @Inject
    private PlayerRepository playerRepo;

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
        games.addAll(multiplayerGameRepo.getClassroomGames(classroom.getId()));
        games.addAll(meleeGameRepo.getClassroomGames(classroom.getId()));

        return games.stream()
                .map(game -> {
                    Player player = playerRepo.getPlayerForUserAndGame(login.getUserId(), game.getId());
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
