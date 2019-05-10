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
<%
    /* Check what admin page is currently active. */

    /* Name of the active page. */
    String activePage = null;

    Object activePageObject = request.getAttribute("adminActivePage");
    if (activePageObject instanceof String) {
        activePage = (String) activePageObject;
    }
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

<ul class="nav nav-tabs">
    <li class="<%=active(activePage,"adminCreateGames")%>">
        <a id="adminCreateGames" href="<%=request.getContextPath() + Paths.ADMIN_GAMES%>">Create Games</a>
    </li>
    <li class="<%=active(activePage,"adminMonitorGames")%>">
        <a id="adminMonitorGames" href="<%=request.getContextPath() + Paths.ADMIN_MONITOR%>">Monitor Games</a>
    </li>
    <li class="<%=active(activePage,"adminUserMgmt")%>">
        <a id="adminUserMgmt" href="<%=request.getContextPath() + Paths.ADMIN_USERS%>">Manage Users</a>
    </li>
    <li class="<%=active(activePage,"adminPuzzles")%>">
        <a id="adminPuzzles" href="<%=request.getContextPath() + Paths.ADMIN_PUZZLES%>">Manage Puzzles</a>
    </li>
    <li class="<%=active(activePage,"adminSystemSettings")%>">
        <a id="adminSystemSettings" href="<%=request.getContextPath() + Paths.ADMIN_SETTINGS%>">System Settings</a>
    </li>
    <li class="<%=active(activePage,"adminKillMaps")%>">
        <a id="adminKillMaps" href="<%=request.getContextPath() + Paths.ADMIN_KILLMAPS + "/manual"%>">Generate KillMaps</a>
    </li>
    <li class="<%=active(activePage,"adminAnalytics")%> dropdown">
        <a id="adminAnalytics" class="dropdown-toggle" data-toggle="dropdown" href="#">Analytics&#160&#160<span class="glyphicon glyphicon-menu-hamburger"></span></a>
        <ul class="dropdown-menu">
            <li><a id="adminAnalyticsUsers" href="<%=request.getContextPath() + Paths.ADMIN_ANALYTICS_USERS%>">Users</a></li>
            <li><a id="adminAnalyticsClasses" href="<%=request.getContextPath() + Paths.ADMIN_ANALYTICS_CLASSES%>">Classes</a></li>
            <li><a id="adminAnalyticsKillmaps" href="<%=request.getContextPath() + Paths.ADMIN_ANALYTICS_KILLMAPS%>">KillMaps</a></li>
        </ul>
    </li>
</ul>
