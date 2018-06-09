<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %><%
    /* Check what admin page is currently active. */

    /* Name of the active page. */
    String activePage = null;

    Object activePageObject = request.getAttribute("adminActivePage");
    if (activePageObject instanceof String) {
        activePage = (String) activePageObject;
    }

    final Logger logger = LoggerFactory.getLogger("game_view.jsp");
    logger.info("adminActivePage: " + activePage);
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
        <a id="adminCreateGames" href="<%=request.getContextPath()%>/admin/games">Create Games</a>
    </li>
    <li class="<%=active(activePage,"adminMonitorGames")%>">
        <a id="adminMonitorGames" href="<%=request.getContextPath()%>/admin/monitor">Monitor Games</a>
    </li>
    <li class="<%=active(activePage,"adminUserMgmt")%>">
        <a id="adminUserMgmt" href="<%=request.getContextPath()%>/admin/users">Manage Users</a>
    </li>
    <li class="<%=active(activePage,"adminSystemSettings")%>">
        <a id="adminSystemSettings" href="<%=request.getContextPath()%>/admin/settings">System Settings</a>
    </li>
    <li class="<%=active(activePage,"adminAnalytics")%> dropdown">
        <a id="adminAnalytics" class="dropdown-toggle" data-toggle="dropdown" href="#">Analytics&#160&#160<span class="glyphicon glyphicon-menu-hamburger"></span></a>
        <ul class="dropdown-menu">
            <li><a id="adminAnalyticsUsers" href="<%=request.getContextPath()%>/admin/analytics/users">Users</a></li>
        </ul>
    </li>
</ul>
