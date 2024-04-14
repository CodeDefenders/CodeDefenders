<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="auth" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="puzzleRepo" type="org.codedefenders.persistence.database.PuzzleRepository"--%>

<jsp:include page="/jsp/header_base.jsp"/>

<nav class="navbar navbar-expand-md navbar-cd" id="header">
    <div class="container-fluid">

        <a class="navbar-brand" href="${url.forPath("/")}">
            <img src="${url.forPath("/images/logo.png")}" alt="" class="d-inline-block"
                 <%-- Negative margin to prevent the navbar from getting tall from the tall image. --%>
                 style="height: 2.5rem; margin: -2rem .25rem -1.7rem .2rem;" />
            Code Defenders
        </a>

        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#header-navbar-controls"
                aria-controls="header-navbar-controls" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="header-navbar-controls">

            <ul class="navbar-nav me-auto">
                <li class="nav-item nav-item-highlight dropdown me-3">
                    <a class="nav-link dropdown-toggle" id="header-multiplayer" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        Multiplayer
                    </a>
                    <ul class="dropdown-menu" aria-labelledby="header-multiplayer">
                        <li><a class="dropdown-item" id="header-games" href="${url.forPath(Paths.GAMES_OVERVIEW)}">Games</a></li>
                        <li><a class="dropdown-item" id="header-classrooms" href="${url.forPath(Paths.CLASSROOMS_OVERVIEW)}">Classrooms</a></li>
                        <li><a class="dropdown-item" id="header-games-history" href="${url.forPath(Paths.GAMES_HISTORY)}">History</a></li>
                        <li><a class="dropdown-item" id="header-leaderboard" href="${url.forPath(Paths.LEADERBOARD_PAGE)}">Leaderboard</a></li>
                    </ul>
                </li>
                <c:if test="${puzzleRepo.checkPuzzlesEnabled() && puzzleRepo.checkActivePuzzlesExist()}">
                    <li class="nav-item nav-item-highlight me-3">
                    <a class="nav-link" id="header-puzzle" href="${url.forPath(Paths.PUZZLE_OVERVIEW)}">Puzzles</a>
                    </li>
                </c:if>
                <c:if test="${auth.admin}">
                    <li class="nav-item nav-item-highlight me-3">
                        <a class="nav-link" id="header-admin" href="${url.forPath("/admin")}">Admin</a>
                    </li>
                </c:if>
            </ul>

            <ul class="navbar-nav">
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" id="header-user" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        ${auth.simpleUser.name}
                    </a>
                    <ul class="dropdown-menu" id="user-dropdown" aria-labelledby="header-user"
                        <%-- Align dropdown menu to the right, so it doesn't get cut off. --%>
                        style="left: auto; right: 0;">
                        <li><a class="dropdown-item" id="header-profile" href="${url.forPath(Paths.USER_PROFILE)}">Profile</a></li>
                        <li><a class="dropdown-item" id="header-account" href="${url.forPath(Paths.USER_SETTINGS)}">Account</a></li>
                        <li><a class="dropdown-item" id="header-help" href="${url.forPath(Paths.HELP_PAGE)}">Help</a></li>
                        <li><a class="dropdown-item" id="header-logout" href="${url.forPath(Paths.LOGOUT)}">Logout</a></li>
                    </ul>
                </li>
            </ul>

        </div>
    </div>
</nav>

<jsp:include page="/jsp/game_components/progress_bar_common.jsp"/>
<jsp:include page="/jsp/messages.jsp"/>

<div id="content"> <%-- closed in footer --%>
