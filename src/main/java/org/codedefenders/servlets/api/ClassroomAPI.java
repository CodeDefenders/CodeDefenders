package org.codedefenders.servlets.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
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

        if (!login.isAdmin()) {
            Optional<ClassroomMember> member = classroomService.getMemberForClassroomAndUser(
                    classroom.get().getId(), login.getUserId());
            if (!member.isPresent()) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                return;
            }
        }

        List<ClassroomMemberDTO> members = getMembersForClassroom(classroom.get());
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
        gson.toJson(members, response.getWriter());
    }

    private void handleClassrooms(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String which = ServletUtils.getStringParameter(request, "which").orElse("all");

        List<Classroom> classrooms;
        switch (which) {
            case "all":
                if (!login.isAdmin()) {
                    response.setStatus(HttpStatus.SC_BAD_REQUEST);
                    return;
                }
                classrooms = classroomService.getAllClassrooms();
                break;
            case "visible":
                classrooms = classroomService.getVisibleClassrooms();
                break;
            case "user":
                classrooms = classroomService.getAllClassroomsByMember(login.getUserId());
                break;
            default:
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                return;
        }

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
        gson.toJson(classrooms, response.getWriter());
    }

    private List<ClassroomMemberDTO> getMembersForClassroom(Classroom classroom) {
        List<ClassroomMember> members = classroomService.getMembersForClassroom(classroom.getId());

        List<ClassroomMemberDTO> memberDTOs = new ArrayList<>();
        for (ClassroomMember member : members) {
            SimpleUser user = userService.getSimpleUserById(member.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Non-existing user is part of this classroom."));
            memberDTOs.add(new ClassroomMemberDTO(user, member.getRole()));
        }

        return memberDTOs;
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
}
