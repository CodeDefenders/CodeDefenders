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
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.service.AuthService;
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
    private AuthService login; // for some reason it injects LoginBean if the type is CodeDefendersAuth

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        Optional<String> type = ServletUtils.getStringParameter(request, "type");
        if (!type.isPresent()) {
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
            case "exists":
                handleExists(request, response);
                return;
            default:
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
        }
    }

    private void handleMembers(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (!classroom.isPresent()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        Optional<ClassroomMember> member = classroomService.getMemberForClassroomAndUser(
                classroom.get().getId(), login.getUserId());
        if (!member.isPresent()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        gson.toJson(getMembers(classroom.get()), response.getWriter());
    }

    private void handleClassrooms(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!login.isAdmin()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        List<Classroom> classrooms = classroomService.getAllClassrooms();
        List<ClassroomDTO> classroomDTOs = classrooms.stream()
                .map(c -> new ClassroomDTO(c.getId(), c.getName(), c.getRoomCode(), c.isOpen()))
                .collect(Collectors.toList());
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        gson.toJson(classroomDTOs, response.getWriter());
    }

    private List<ClassroomMemberDTO> getMembers(Classroom classroom) {
        List<ClassroomMember> members = classroomService.getMembersForClassroom(classroom.getId());

        List<ClassroomMemberDTO> memberDTOs = new ArrayList<>();
        for (ClassroomMember member : members) {
            SimpleUser user = userService.getSimpleUserById(member.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Non-existing user is part of this classroom."));
            memberDTOs.add(new ClassroomMemberDTO(user, member.getRole()));
        }

        return memberDTOs;
    }

    private void handleExists(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        String json = classroom.isPresent() ? "true" : "false";
        response.getWriter().print(json);
    }

    private Optional<Classroom> getClassroomFromRequest(HttpServletRequest request) {
        Optional<Classroom> classroom = ServletUtils.getIntParameter(request, "classroomId")
                .flatMap(classroomService::getClassroomById);
        if (classroom.isPresent()) {
            return classroom;
        }

        return ServletUtils.getStringParameter(request, "room")
                .flatMap(classroomService::getClassroomByRoomCode);
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

    private static class ClassroomDTO {
        @Expose
        private final int id;

        @Expose
        private final String name;

        @Expose
        private final String roomCode;

        @Expose
        private final boolean open;

        public ClassroomDTO(int id, String name, String roomCode, boolean open) {
            this.id = id;
            this.name = name;
            this.roomCode = roomCode;
            this.open = open;
        }
    }
}
