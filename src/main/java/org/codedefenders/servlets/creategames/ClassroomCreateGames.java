/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.servlets.creategames;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.creategames.ClassroomCreateGamesBean;
import org.codedefenders.beans.creategames.CreateGamesBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.service.CreateGamesService;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;


@WebServlet(Paths.CLASSROOM_CREATE_GAMES)
public class ClassroomCreateGames extends CreateGamesServlet {
    private ClassroomCreateGamesBean createGamesBean;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private MessagesBean messages;

    @Inject
    private CreateGamesService createGamesService;

    @Inject
    private ClassroomService classroomService;

    @Inject
    private URLUtils url;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (classroom.isEmpty()) {
            messages.add("Classroom not found.");
            response.sendRedirect(url.forPath(Paths.CLASSROOMS_OVERVIEW));
            return;
        }

        Optional<ClassroomMember> member = classroomService.getMemberForClassroomAndUser(
                classroom.get().getId(), login.getUserId());
        if (!classroomService.canCreateGames(member.orElse(null))) {
            messages.add("You don't have access to this page.");
            response.sendRedirect(url.forPath(Paths.CLASSROOMS_OVERVIEW));
            return;
        }

        if (classroom.get().isArchived()) {
            messages.add("This classroom is archived. You cannot create games for it.");
            response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomUid=" + classroom.get().getUUID());
            return;
        }

        createGamesBean = createGamesService.getContextForClassroom(login.getUserId(), classroom.get().getId());
        synchronized (createGamesBean.getSynchronizer()) {
            request.setAttribute("createGamesBean", createGamesBean);
            request.setAttribute("classroom", classroom.get());
            request.getRequestDispatcher("/jsp/classroom_create_games.jsp").forward(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Optional<Classroom> classroom = getClassroomFromRequest(request);
        if (classroom.isEmpty()) {
            messages.add("Classroom not found.");
            response.sendRedirect(url.forPath(Paths.CLASSROOMS_OVERVIEW));
            return;
        }

        Optional<ClassroomMember> member = classroomService.getMemberForClassroomAndUser(
                classroom.get().getId(), login.getUserId());
        if (!classroomService.canCreateGames(member.orElse(null))) {
            messages.add("You don't have access to this page.");
            response.sendRedirect(url.forPath(Paths.CLASSROOMS_OVERVIEW));
            return;
        }

        if (classroom.get().isArchived()) {
            messages.add("This classroom is archived. You cannot create games for it.");
            response.sendRedirect(url.forPath(Paths.CLASSROOM) + "?classroomUid=" + classroom.get().getUUID());
            return;
        }

        createGamesBean = createGamesService.getContextForClassroom(login.getUserId(), classroom.get().getId());
        super.doPost(request, response);
    }

    @Override
    protected CreateGamesBean getContext() {
        return createGamesBean;
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
}
