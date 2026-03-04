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
<%@ tag import="org.codedefenders.util.Paths" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%@ attribute name="activePage" required="true" %>

<ul class="nav nav-tabs mb-4">
    <li class="nav-item">
        <a class="nav-link ${activePage.equals(\"adminCreateGames\") ? 'active' : ''}" id="adminCreateGames" href="${url.forPath(Paths.ADMIN_GAMES)}">${i18n.tr('Create Games')}</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals(\"adminMonitorGames\") ? 'active' : ''}" id="adminMonitorGames" href="${url.forPath(Paths.ADMIN_MONITOR)}">${i18n.tr('Monitor Games')}</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals(\"adminUserMgmt\") ? 'active' : ''}" id="adminUserMgmt" href="${url.forPath(Paths.ADMIN_USERS)}">${i18n.tr('Users')}</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals(\"classrooms\") ? 'active' : ''}" id="classrooms" href="${url.forPath(Paths.ADMIN_CLASSROOMS)}">${i18n.tr('Classrooms')}</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals(\"adminClasses\") ? 'active' : ''}" id="adminClasses" href="${url.forPath(Paths.ADMIN_CLASSES)}">${i18n.tr('Classes')}</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals(\"adminPuzzles\") ? 'active' : ''}" id="adminPuzzleManagement" href="${url.forPath(Paths.ADMIN_PUZZLE_MANAGEMENT)}">${i18n.tr('Puzzles')}</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals(\"adminKillMaps\") ? 'active' : ''}" id="adminKillMaps" href="${url.forPath(Paths.ADMIN_KILLMAPS)}/manual">${i18n.tr('Analysis')}</a>
    </li>
    <li class="nav-item dropdown">
        <a class="nav-link ${activePage.equals(\"adminAnalytics\") ? 'active' : ''} dropdown-toggle" id="adminAnalytics" data-bs-toggle="dropdown" href="#">${i18n.tr('Analytics')}</a>
        <ul class="dropdown-menu">
            <li><a class="dropdown-item" id="adminAnalyticsUsers" href="${url.forPath(Paths.ADMIN_ANALYTICS_USERS)}">${i18n.tr('Users')}</a></li>
            <li><a class="dropdown-item" id="adminAnalyticsClasses" href="${url.forPath(Paths.ADMIN_ANALYTICS_CLASSES)}">${i18n.tr('Classes')}</a></li>
            <li><a class="dropdown-item" id="adminAnalyticsKillmaps" href="${url.forPath(Paths.ADMIN_ANALYTICS_KILLMAPS)}">${i18n.tr('KillMaps')}</a></li>
        </ul>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals(\"adminSystemSettings\") ? 'active' : ''}" id="adminSystemSettings" href="${url.forPath(Paths.ADMIN_SETTINGS)}">${i18n.tr('System Settings')}</a>
    </li>
</ul>
