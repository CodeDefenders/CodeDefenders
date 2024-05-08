<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ tag import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>
<%--@elvariable id="auth" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="puzzleRepo" type="org.codedefenders.persistence.database.PuzzleRepository"--%>


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
            <c:choose>
                <c:when test="${auth.loggedIn}">

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

                </c:when>
                <c:otherwise>

                    <ul class="navbar-nav me-auto">
                    </ul>
                    <c:if test="${!pageContext.request.requestURI.contains(\"login\")}">
                        <ul class="navbar-nav ms-auto">
                            <li class="nav-item">
                                <a class="nav-link" href="${url.forPath("/login")}">Login</a>
                            </li>
                        </ul>
                    </c:if>

                </c:otherwise>
            </c:choose>
        </div>
    </div>
</nav>
