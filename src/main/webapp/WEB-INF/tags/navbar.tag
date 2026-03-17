<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ tag import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>
<%--@elvariable id="auth" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="puzzleRepo" type="org.codedefenders.persistence.database.PuzzleRepository"--%>
<%--@elvariable id="puzzleNavigation" type="org.codedefenders.beans.page.PuzzleNavigationBean"--%>
<%--@elvariable id="i18nService" type="org.codedefenders.service.I18nService"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%--
    Provides the navigation bar.
    Different menu entries are available depending on if the user is logged in or not.
--%>

<nav class="navbar navbar-expand-md navbar-cd" id="header">
    <div class="container-fluid">

        <a class="navbar-brand" href="${url.forPath("/")}">
            <img src="${url.forPath("/images/logo.png")}" alt="" class="d-inline-block"
                 <%-- Negative margin to prevent the navbar from getting tall from the tall image. --%>
                 style="height: 2.5rem; margin: -2rem .25rem -1.7rem .2rem;" />
            ${i18n.tr('Code Defenders')}
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
                                    ${i18n.tr('Multiplayer')}
                            </a>
                            <ul class="dropdown-menu" aria-labelledby="header-multiplayer">
                                <li><a class="dropdown-item" id="header-games"
                                       href="${url.forPath(Paths.GAMES_OVERVIEW)}">${i18n.tr('Games')}</a></li>
                                <li><a class="dropdown-item" id="header-classrooms"
                                       href="${url.forPath(Paths.CLASSROOMS_OVERVIEW)}">${i18n.tr('Classrooms')}</a>
                                </li>
                                <li><a class="dropdown-item" id="header-games-history"
                                       href="${url.forPath(Paths.GAMES_HISTORY)}">${i18n.tr('History')}</a></li>
                                <li><a class="dropdown-item" id="header-leaderboard"
                                       href="${url.forPath(Paths.LEADERBOARD_PAGE)}">${i18n.tr('Leaderboard')}</a></li>
                            </ul>
                        </li>

                        <t:puzzle_navigation/>

                        <c:if test="${auth.admin}">
                            <li class="nav-item nav-item-highlight me-3">
                                <a class="nav-link" id="header-admin"
                                   href="${url.forPath("/admin")}">${i18n.tr('Admin')}</a>
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
                                <li><a class="dropdown-item" id="header-profile"
                                       href="${url.forPath(Paths.USER_PROFILE)}">${i18n.tr('Profile')}</a></li>
                                <li><a class="dropdown-item" id="header-account"
                                       href="${url.forPath(Paths.USER_SETTINGS)}">${i18n.tr('Account')}</a></li>
                                <li><a class="dropdown-item" id="header-help"
                                       href="${url.forPath(Paths.HELP_PAGE)}">${i18n.tr('Help')}</a></li>
                                <li><a class="dropdown-item" id="header-logout"
                                       href="${url.forPath(Paths.LOGOUT)}">${i18n.tr('Logout')}</a></li>
                            </ul>
                        </li>
                    </ul>

                </c:when>
                <c:otherwise>

                    <ul class="navbar-nav me-auto">
                    </ul>
                    <c:if test="${!pageContext.request.requestURI.contains('login')}">
                        <ul class="navbar-nav ms-auto gap-4">
                            <c:set var="supportedLocales" value="${i18nService.supportedLocales}"/>
                            <c:if test="${fn:length(supportedLocales) > 1}">
                                <li class="nav-item dropdown">
                                    <a class="nav-link dropdown-toggle" id="header-language" href="#" role="button"
                                       data-bs-toggle="dropdown" aria-expanded="false">
                                            ${i18n.tr('Language')}
                                    </a>
                                    <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="header-language">
                                        <c:forEach var="l" items="${supportedLocales}">
                                            <li>
                                                <form method="post" action="${url.forPath("/change-language")}">
                                                    <input type="hidden" name="lang" value="${l.language}"/>
                                                    <button type="submit" class="btn btn-link dropdown-item px-3 py-1">
                                                        <c:choose>
                                                            <c:when test="${i18n.locale.language == l.language}">
                                                                ${l.getDisplayLanguage(i18n.locale)}
                                                            </c:when>
                                                            <c:otherwise>
                                                                ${l.getDisplayLanguage(i18n.locale)} (${l.getDisplayLanguage(l)})
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </button>
                                                </form>
                                            </li>
                                        </c:forEach>
                                    </ul>
                                </li>
                            </c:if>
                            <li class="nav-item">
                                <a class="nav-link" href="${url.forPath("/login")}">${i18n.tr('Login')}</a>
                            </li>
                        </ul>
                    </c:if>

                </c:otherwise>
            </c:choose>
        </div>
    </div>
</nav>
