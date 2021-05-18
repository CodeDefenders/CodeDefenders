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
<%@ page import="org.codedefenders.util.Paths" %>

<%
    /* Name of the active page. */
    String activePage = (String) request.getAttribute("adminActivePage");
%>

<%!
    /**
     * Returns {@code "active"} if the {@code page} equals {@code activePage},
     * or an empty String otherwise.
     */
    public String active(String activePage, String page) {
        if (page.equals(activePage)) {
            return "active";
        } else {
            return "";
        }
    }
%>

<ul class="nav nav-tabs mb-4">
    <li class="nav-item">
        <a class="nav-link <%=active(activePage,"adminCreateGames")%>" id="adminCreateGames" href="<%=request.getContextPath() + Paths.ADMIN_GAMES%>">Create Games</a>
    </li>
    <li class="nav-item">
        <a class="nav-link <%=active(activePage,"adminMonitorGames")%>" id="adminMonitorGames" href="<%=request.getContextPath() + Paths.ADMIN_MONITOR%>">Monitor Games</a>
    </li>
    <li class="nav-item">
        <a class="nav-link <%=active(activePage,"adminUserMgmt")%>" id="adminUserMgmt" href="<%=request.getContextPath() + Paths.ADMIN_USERS%>">Users</a>
    </li>
    <li class="nav-item">
        <a class="nav-link <%=active(activePage,"adminClasses")%>" id="adminClasses" href="<%=request.getContextPath() + Paths.ADMIN_CLASSES%>">Classes</a>
    </li>
    <li class="nav-item dropdown">
        <a class="nav-link <%=active(activePage,"adminPuzzles")%> dropdown-toggle" id="adminPuzzles" data-bs-toggle="dropdown" href="#">Puzzles</a>
        <ul class="dropdown-menu">
            <li><a class="dropdown-item" id="adminPuzzleManagement" href="<%=request.getContextPath() + Paths.ADMIN_PUZZLE_MANAGEMENT%>">Manage</a></li>
            <li><a class="dropdown-item" id="adminPuzzleUpload" href="<%=request.getContextPath() + Paths.ADMIN_PUZZLE_UPLOAD%>">Upload</a></li>
        </ul>
    </li>
    <li class="nav-item">
        <a class="nav-link <%=active(activePage,"adminKillMaps")%>" id="adminKillMaps" href="<%=request.getContextPath() + Paths.ADMIN_KILLMAPS + "/manual"%>">Analysis</a>
    </li>
    <li class="nav-item dropdown">
        <a class="nav-link <%=active(activePage,"adminAnalytics")%> dropdown-toggle" id="adminAnalytics" data-bs-toggle="dropdown" href="#">Analytics</a>
        <ul class="dropdown-menu">
            <li><a class="dropdown-item" id="adminAnalyticsUsers" href="<%=request.getContextPath() + Paths.ADMIN_ANALYTICS_USERS%>">Users</a></li>
            <li><a class="dropdown-item" id="adminAnalyticsClasses" href="<%=request.getContextPath() + Paths.ADMIN_ANALYTICS_CLASSES%>">Classes</a></li>
            <li><a class="dropdown-item" id="adminAnalyticsKillmaps" href="<%=request.getContextPath() + Paths.ADMIN_ANALYTICS_KILLMAPS%>">KillMaps</a></li>
        </ul>
    </li>
    <li class="nav-item <%=active(activePage,"adminSystemSettings")%>">
        <a class="nav-link" id="adminSystemSettings" href="<%=request.getContextPath() + Paths.ADMIN_SETTINGS%>">System Settings</a>
    </li>
</ul>
