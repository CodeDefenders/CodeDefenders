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
package org.codedefenders.servlets;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.beans.page.PageInfoBean;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@WebServlet(Paths.CLASSROOM)
public class ClassroomServlet extends HttpServlet {

    @Inject
    private ClassroomService classroomService;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private MessagesBean messages;

    @Inject
    private URLUtils url;

    @Inject
    private PageInfoBean pageInfo;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (classroom.isEmpty()) {
            messages.add("Classroom not found.");
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<ClassroomMember> member = classroomService.getMemberForClassroomAndUser(
                classroom.get().getId(), login.getUserId());

        // Go to classroom page
        pageInfo.setPageTitle(classroom.get().getName());
        request.setAttribute("classroom", classroom.get());
        request.setAttribute("member", member.orElse(null));
        request.setAttribute("link", classroomService.getInviteLinkForClassroom(classroom.get().getUUID()));
        request.setAttribute("canEditClassroom", classroomService.canEditClassroom(member.orElse(null)));
        request.setAttribute("canChangeRoles", classroomService.canChangeRoles(member.orElse(null)));
        request.setAttribute("canChangeOwner", classroomService.canChangeOwner(member.orElse(null)));
        request.setAttribute("canKickStudents", classroomService.canKickStudents(member.orElse(null)));
        request.setAttribute("canKickModerators", classroomService.canKickModerators(member.orElse(null)));
        request.setAttribute("canCreateGames", classroomService.canCreateGames(member.orElse(null)));
        request.setAttribute("canLeave", classroomService.canLeave(member.orElse(null)));
        request.setAttribute("canJoin", classroomService.canJoin(classroom.get(), member.orElse(null)));
        request.getRequestDispatcher("/jsp/classroom_page.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Optional<String> action = ServletUtils.getStringParameter(request, "action");
        if (action.isEmpty()) {
            messages.add("Missing required parameter: action.");
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (!action.get().equals("create-classroom") && classroom.isEmpty()) {
            messages.add("Classroom not found.");
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<ClassroomMember> member = classroom.flatMap(c ->
                classroomService.getMemberForClassroomAndUser(c.getId(), login.getUserId()));

        try {
            switch (action.get()) {
                case "create-classroom":
                    createClassroom(request, response);
                    break;
                case "enable-joining":
                    setOpen(request, response, classroom.get(), member.orElse(null), true);
                    break;
                case "disable-joining":
                    setOpen(request, response, classroom.get(), member.orElse(null), false);
                    break;
                case "make-public":
                    setVisible(request, response, classroom.get(), member.orElse(null), true);
                    break;
                case "make-private":
                    setVisible(request, response, classroom.get(), member.orElse(null), false);
                    break;
                case "change-name":
                    changeName(request, response, classroom.get(), member.orElse(null));
                    break;
                case "set-password":
                    setPassword(request, response, classroom.get(), member.orElse(null));
                    break;
                case "remove-password":
                    removePassword(request, response, classroom.get(), member.orElse(null));
                    break;
                case "change-role":
                    changeRole(request, response, classroom.get(), member.orElse(null));
                    break;
                case "change-owner":
                    changeOwner(request, response, classroom.get(), member.orElse(null));
                    break;
                case "kick-member":
                    kickMember(request, response, classroom.get(), member.orElse(null));
                    break;
                case "join":
                    join(request, response, classroom.get(), member.orElse(null));
                    break;
                case "leave":
                    leave(request, response, classroom.get(), member.orElse(null));
                    break;
                case "archive":
                    setArchived(request, response, classroom.get(), member.orElse(null), true);
                    break;
                case "restore":
                    setArchived(request, response, classroom.get(), member.orElse(null), false);
                    break;
                default:
                    messages.add("Invalid action: " + action);
                    Redirect.redirectBack(request, response);
            }
        } catch (ValidationException e) {
            messages.add("Validation failed: " + e.getMessage());
            Redirect.redirectBack(request, response);
        } catch (PermissionDeniedException e) {
            messages.add("You're not allowed to do that.");
            Redirect.redirectBack(request, response);
        } catch (NoSuchElementException e) {
            messages.add("Missing or invalid parameter.");
            Redirect.redirectBack(request, response);
        }
    }

    public void redirectToClassroomPage(HttpServletResponse response, UUID classroomUUID) throws IOException {
        response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomUid=" + classroomUUID.toString());
    }

    public void checkPermission(boolean permitted) throws PermissionDeniedException {
        if (!permitted) {
            throw new PermissionDeniedException();
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

    private void createClassroom(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = ServletUtils.getStringParameter(request, "name").get();
        classroomService.addClassroom(name);
        Redirect.redirectBack(request, response);
    }

    private void setOpen(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                         ClassroomMember member, boolean open)
            throws IOException {
        checkPermission(classroomService.canEditClassroom(member));
        classroomService.setOpen(classroom.getId(), open);

        messages.add("Successfully set classroom to " + (open ? "open" : "closed") + ".");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void setVisible(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                            ClassroomMember member, boolean visible) throws IOException {
        checkPermission(classroomService.canEditClassroom(member));
        classroomService.setVisible(classroom.getId(), visible);

        messages.add("Successfully set classroom to " + (visible ? "public" : "private") + ".");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void setArchived(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                             ClassroomMember member, boolean archived) throws IOException {
        checkPermission(classroomService.canEditClassroom(member));
        classroomService.setArchived(classroom.getId(), archived);

        messages.add("Successfully " + (archived ? "archived" : "restored") + " classroom.");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void changeName(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                            ClassroomMember member) throws IOException {
        String name = ServletUtils.getStringParameter(request, "name").get();

        checkPermission(classroomService.canEditClassroom(member));
        classroomService.changeName(classroom.getId(), name);

        messages.add("Successfully changed the name to: " + name);
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void setPassword(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                             ClassroomMember member) throws IOException {
        String password = ServletUtils.getStringParameter(request, "password").get();

        checkPermission(classroomService.canEditClassroom(member));
        classroomService.setPassword(classroom.getId(), password);

        messages.add("Successfully set the password.");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void removePassword(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                                ClassroomMember member) throws IOException {
        checkPermission(classroomService.canEditClassroom(member));
        classroomService.removePassword(classroom.getId());

        messages.add("Successfully removed the password.");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void changeRole(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                            ClassroomMember member) throws IOException {
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

        checkPermission(classroomService.canChangeRoles(member));
        classroomService.changeRole(classroom.getId(), userId, role);

        messages.add("Successfully changed role.");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void changeOwner(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                             ClassroomMember member) throws IOException {
        int userId = ServletUtils.getIntParameter(request, "userId").get();

        checkPermission(classroomService.canChangeOwner(member));
        classroomService.changeOwner(classroom.getId(), userId);

        messages.add("Successfully changed the owner.");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void kickMember(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                            ClassroomMember member) throws IOException {
        int userId = ServletUtils.getIntParameter(request, "userId").get();

        Optional<ClassroomMember> kickedMember = classroomService.getMemberForClassroomAndUser(
                classroom.getId(), userId);
        if (kickedMember.isEmpty()) {
            messages.add("Member not found.");
            Redirect.redirectBack(request, response);
        } else if (kickedMember.get().getRole() == ClassroomRole.MODERATOR) {
            checkPermission(classroomService.canKickModerators(member));
        } else if (kickedMember.get().getRole() == ClassroomRole.STUDENT) {
            checkPermission(classroomService.canKickStudents(member));
        }

        classroomService.removeMember(classroom.getId(), userId);

        messages.add("Successfully kicked member.");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void join(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                      ClassroomMember member) throws IOException {
        if (member != null) {
            messages.add("You're already a member of this classroom.");
            Redirect.redirectBack(request, response);
            return;
        }

        if (!classroom.isOpen() || classroom.isArchived()) {
            messages.add("Can't join this classroom.");
            Redirect.redirectBack(request, response);
            return;
        }

        if (classroom.getPassword().isPresent()) {
            String password = ServletUtils.getStringParameter(request, "password").get();
            boolean matches = passwordEncoder.matches(password, classroom.getPassword().get());
            if (!matches) {
                messages.add("Wrong password");
                Redirect.redirectBack(request, response);
                return;
            }
        }

        member = new ClassroomMember(login.getUserId(), classroom.getId(), ClassroomRole.STUDENT);
        classroomService.addMember(member);

        messages.add("Successfully joined classroom.");
        redirectToClassroomPage(response, classroom.getUUID());
    }

    private void leave(HttpServletRequest request, HttpServletResponse response, Classroom classroom,
                       ClassroomMember member) throws IOException {
        if (member == null) {
            messages.add("You're not a member of this classroom.");
            Redirect.redirectBack(request, response);
            return;
        }

        classroomService.removeMember(classroom.getId(), login.getUserId());

        messages.add("Successfully left classroom.");
        response.sendRedirect(url.forPath(Paths.CLASSROOMS_OVERVIEW));
    }

    private static class PermissionDeniedException extends RuntimeException {}
}
