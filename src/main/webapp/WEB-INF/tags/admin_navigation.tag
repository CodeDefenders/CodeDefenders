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
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ tag import="org.codedefenders.util.Paths" %>

<%@ attribute name="activePage" required="true" %>

<ul class="nav nav-tabs mb-4">
    <li class="nav-item">
        <a class="nav-link ${activePage.equals("adminCreateGames") ? 'active' : ''}" id="adminCreateGames" href="${url.forPath(Paths.ADMIN_GAMES)}">Create Games</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals("adminMonitorGames") ? 'active' : ''}" id="adminMonitorGames" href="${url.forPath(Paths.ADMIN_MONITOR)}">Monitor Games</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals("adminUserMgmt") ? 'active' : ''}" id="adminUserMgmt" href="${url.forPath(Paths.ADMIN_USERS)}">Users</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals("classrooms") ? 'active' : ''}" id="classrooms" href="${url.forPath(Paths.ADMIN_CLASSROOMS)}">Classrooms</a>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals("adminClasses") ? 'active' : ''}" id="adminClasses" href="${url.forPath(Paths.ADMIN_CLASSES)}">Classes</a>
    </li>
    <li class="nav-item dropdown">
        <a class="nav-link ${activePage.equals("adminPuzzles") ? 'active' : ''} dropdown-toggle" id="adminPuzzles" data-bs-toggle="dropdown" href="#">Puzzles</a>
        <ul class="dropdown-menu">
            <li><a class="dropdown-item" id="adminPuzzleManagement" href="${url.forPath(Paths.ADMIN_PUZZLE_MANAGEMENT)}">Manage</a></li>
            <li><a class="dropdown-item" id="adminPuzzleUpload" href="${url.forPath(Paths.ADMIN_PUZZLE_UPLOAD)}">Upload</a></li>
        </ul>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals("adminKillMaps") ? 'active' : ''}" id="adminKillMaps" href="${url.forPath(Paths.ADMIN_KILLMAPS)}/manual">Analysis</a>
    </li>
    <li class="nav-item dropdown">
        <a class="nav-link ${activePage.equals("adminAnalytics") ? 'active' : ''} dropdown-toggle" id="adminAnalytics" data-bs-toggle="dropdown" href="#">Analytics</a>
        <ul class="dropdown-menu">
            <li><a class="dropdown-item" id="adminAnalyticsUsers" href="${url.forPath(Paths.ADMIN_ANALYTICS_USERS)}">Users</a></li>
            <li><a class="dropdown-item" id="adminAnalyticsClasses" href="${url.forPath(Paths.ADMIN_ANALYTICS_CLASSES)}">Classes</a></li>
            <li><a class="dropdown-item" id="adminAnalyticsKillmaps" href="${url.forPath(Paths.ADMIN_ANALYTICS_KILLMAPS)}">KillMaps</a></li>
        </ul>
    </li>
    <li class="nav-item">
        <a class="nav-link ${activePage.equals("adminSystemSettings") ? 'active' : ''}" id="adminSystemSettings" href="${url.forPath(Paths.ADMIN_SETTINGS)}">System Settings</a>
    </li>
</ul>
