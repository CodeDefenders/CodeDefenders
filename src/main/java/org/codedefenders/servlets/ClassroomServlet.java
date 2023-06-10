package org.codedefenders.servlets;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.beans.page.PageInfoBean;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.service.AuthService;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@WebServlet(Paths.CLASSROOM)
public class ClassroomServlet extends HttpServlet {

    @Inject
    private ClassroomService classroomService;

    @Inject
    private AuthService login; // for some reason it injects LoginBean if the type is CodeDefendersAuth

    @Inject
    private MessagesBean messages;

    @Inject
    private URLUtils url;

    @Inject
    private PageInfoBean pageInfo;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (!classroom.isPresent()) {
            messages.add("Classroom not found.");
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<ClassroomMember> member = getMember(classroom.get().getId());

        // Go to classroom page
        if (member.isPresent()) {
            pageInfo.setPageTitle(classroom.get().getName());
            request.setAttribute("classroom", classroom.get());
            request.setAttribute("member", member.get());
            request.getRequestDispatcher("/jsp/classroom_page.jsp").forward(request, response);
            return;
        }

        // Go to join classroom page
        if (classroom.get().isOpen()) {
            List<ClassroomMember> members = classroomService.getMembersForClassroom(classroom.get().getId());
            pageInfo.setPageTitle("Join " + classroom.get().getName());
            request.setAttribute("classroom", classroom.get());
            request.setAttribute("numMembers", members.size());
            request.getRequestDispatcher("/jsp/join_classroom_page.jsp").forward(request, response);
            return;
        }

        // Don't
        messages.add("You are not a member of this classroom.");
        Redirect.redirectBack(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (!classroom.isPresent()) {
            messages.add("Classroom not found.");
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<String> action = ServletUtils.getStringParameter(request, "action");
        if (!action.isPresent()) {
            messages.add("Missing required parameter: action.");
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<ClassroomMember> member = getMember(classroom.get().getId());
        if (!action.get().equals("join")) {
            if (!member.isPresent() || member.get().getRole() != ClassroomRole.OWNER) {
                messages.add("You must be the owner of this classroom.");
                Redirect.redirectBack(request, response);
                return;
            }
        }

        try {
            switch (action.get()) {
                case "enable-joining":
                    setOpen(request, response, classroom.get(), true);
                    break;
                case "disable-joining":
                    setOpen(request, response, classroom.get(), false);
                    break;
                case "change-name":
                    changeName(request, response, classroom.get());
                    break;
                case "set-password":
                    setPassword(request, response, classroom.get());
                    break;
                case "remove-password":
                    removePassword(request, response, classroom.get());
                    break;
                case "change-room-code":
                    changeRoomCode(request, response, classroom.get());
                    break;
                case "change-role":
                    changeRole(request, response, classroom.get());
                    break;
                case "change-owner":
                    changeOwner(request, response, classroom.get());
                    break;
                case "kick-member":
                    kickMember(request, response, classroom.get());
                    break;
                case "join":
                    join(request, response, classroom.get());
                    break;
            }
        } catch (ValidationException e) {
            messages.add("Validation failed: " + e.getMessage());
            Redirect.redirectBack(request, response);
        } catch (NoSuchElementException e) {
            messages.add("Missing or invalid parameter.");
            Redirect.redirectBack(request, response);
        }
    }

    private Optional<ClassroomMember> getMember(int classroomId) {
        if (login.isAdmin()) {
            return Optional.of(new ClassroomMember(login.getUserId(), classroomId, ClassroomRole.OWNER));
        }
        return classroomService.getMemberForClassroomAndUser(classroomId, login.getUserId());
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

    private void setOpen(HttpServletRequest request, HttpServletResponse response, Classroom classroom, boolean open)
            throws IOException {
        classroomService.setOpen(classroom.getId(), open);

        messages.add("Successfully set classroom to " + (open ? "open" : "closed"));
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }

    private void changeName(HttpServletRequest request, HttpServletResponse response, Classroom classroom)
            throws IOException {
        String name = ServletUtils.getStringParameter(request, "name").get();

        classroomService.changeName(classroom.getId(), name);

        messages.add("Successfully changed the name to: " + name);
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }

    private void setPassword(HttpServletRequest request, HttpServletResponse response, Classroom classroom)
            throws IOException {
        String password = ServletUtils.getStringParameter(request, "password").get();

        classroomService.setPassword(classroom.getId(), password);

        messages.add("Successfully set the password.");
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }

    private void removePassword(HttpServletRequest request, HttpServletResponse response, Classroom classroom)
            throws IOException {
        classroomService.removePassword(classroom.getId());

        messages.add("Successfully removed the password.");
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }

    private void changeRoomCode(HttpServletRequest request, HttpServletResponse response, Classroom classroom)
            throws IOException {
        String name = ServletUtils.getStringParameter(request, "room-code").get();

        classroomService.changeRoomCode(classroom.getId(), name);

        messages.add("Successfully changed the room code to: " + name);
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }

    private void changeRole(HttpServletRequest request, HttpServletResponse response, Classroom classroom)
            throws IOException {
        int userId = ServletUtils.getIntParameter(request, "userId").get();
        String roleStr = ServletUtils.getStringParameter(request, "role").get();

        ClassroomRole role;
        try {
             role = ClassroomRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            messages.add("Invalid enum value for parameter: role.");
            Redirect.redirectBack(request, response);
            return;
        }

        classroomService.changeRole(classroom.getId(), userId, role);

        messages.add("Successfully changed role");
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }

    private void changeOwner(HttpServletRequest request, HttpServletResponse response, Classroom classroom)
            throws IOException {
        int userId = ServletUtils.getIntParameter(request, "userId").get();

        classroomService.changeOwner(classroom.getId(), userId);

        messages.add("Successfully changed the owner");
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }

    private void kickMember(HttpServletRequest request, HttpServletResponse response, Classroom classroom)
            throws IOException {
        int userId = ServletUtils.getIntParameter(request, "userId").get();

        classroomService.removeMember(classroom.getId(), userId);

        messages.add("Successfully kicked member");
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }

    private void join(HttpServletRequest request, HttpServletResponse response, Classroom classroom)
            throws IOException {
        if (classroom.getPassword().isPresent()) {
            String password = ServletUtils.getStringParameter(request, "password").get();
            boolean matches = new BCryptPasswordEncoder().matches(password, classroom.getPassword().get());
            if (!matches) {
                messages.add("Wrong password");
                Redirect.redirectBack(request, response);
                return;
            }
        }

        ClassroomMember member = new ClassroomMember(login.getUserId(), classroom.getId(), ClassroomRole.STUDENT);
        classroomService.addMember(member);

        messages.add("Successfully joined classroom");
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomId=" + classroom.getId());
    }
}
