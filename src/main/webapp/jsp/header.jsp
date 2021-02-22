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
<%@ page import="org.codedefenders.model.NotificationType" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.servlets.UserProfileManager" %>

<jsp:include page="/jsp/header_base.jsp"/>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    boolean profileEnabled = UserProfileManager.checkEnabled();
%>

<script>
(function () {

    let notificationCount = 0;

    var updateUserNotifications = function (url) {
        $.getJSON(url, function (r) {
            $(r).each(function (index) {
                if (r[index].eventStatus === 'NEW') {
                    notificationCount++;
                }

                const userDropdown = document.getElementById('user-dropdown');

                const li = document.createElement('li');
                const a = document.createElement('a');

                a.href = '<%=request.getContextPath() + Paths.BATTLEGROUND_GAME%>' + '?gameId=' + r[index].gameId;
                a.innerHTML = r[index].parsedMessage;

                li.appendChild(a);
                userDropdown.appendChild(li);
            });

            if (notificationCount > 0) {
                document.getElementById('notification-count').innerText = notificationCount;
                document.getElementById('notification-count').style.display = null;
                document.getElementById('notification-separator').style.display = null;
            }
        });
    };

    $(document).ready(function () {
            if (document.getElementById('user-dropdown') !== null) {
                //notifications written here:
                // refreshed every 5 seconds
                var interval = 5000;
                setInterval(function () {
                    var url = "<%=request.getContextPath() + Paths.API_NOTIFICATION%>?type=<%=NotificationType.USEREVENT%>&timestamp=" + (new Date().getTime() - interval);
                    updateUserNotifications(url);
                }, interval);

                var url = "<%=request.getContextPath() + Paths.API_NOTIFICATION%>?type=<%=NotificationType.USEREVENT%>&timestamp=0";
                updateUserNotifications(url);
            }
            $('[data-toggle="tooltip"]').tooltip();
        }
    );

})();
</script>

<nav class="navbar navbar-cd" id="header">
    <div> <%-- class="container-fluid" --%>
        <div class="navbar-header">

            <%-- The style attributes here are a workaround to make Logo + Text work in the navbar brand. --%>
            <a class="navbar-brand" href="${pageContext.request.contextPath}" style="position: relative;">
                <img src="images/logo.png" style="width: 2em; position: absolute; top: .1em; left: .4em;"/>
                <span style="margin-left: 2em;">Code Defenders</span>
            </a>

            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#header-navbar-controls" aria-expanded="true">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>

        </div>
        <div class="collapse navbar-collapse" id="header-navbar-controls">

            <ul class="nav navbar-nav">
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                        Multiplayer
                        <span class="caret"></span>
                    </a>
                    <ul class="dropdown-menu">
                        <li><a id="headerUserGames" href="<%=request.getContextPath()  + Paths.GAMES_OVERVIEW%>">Games</a></li>
                        <li><a id="headerGamesHistory" href="<%=request.getContextPath() + Paths.GAMES_HISTORY %>">History</a></li>
                        <li><a id="headerLeaderboardButton" href="<%= request.getContextPath() + Paths.LEADERBOARD_PAGE%>">Leaderboard</a></li>
                    </ul>
                </li>
                <li><a id="puzzleOverview" href="<%=request.getContextPath() + Paths.PUZZLE_OVERVIEW%>">Puzzles</a></li>
            </ul>

            <ul class="nav navbar-nav navbar-right">
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
                        ${login.user.username}
                        <span id="notification-count" class="label label-warning" style="margin-left: .25em; display: none;"></span>
                        <span class="caret"></span>
                    </a>
                    <ul class="dropdown-menu" id="user-dropdown">
                        <% if (profileEnabled) { %>
                            <li><a id="headerProfileButton" href="<%=request.getContextPath() + Paths.USER_PROFILE%>">Profile</a></li>
                        <% } %>
                        <li><a id="headerHelpButton" href="<%=request.getContextPath() + Paths.HELP_PAGE%>">Help</a></li>
                        <li><a id="headerLogout" href="<%=request.getContextPath() + Paths.LOGOUT%>">Logout</a></li>
                        <li id="notification-separator" role="separator" class="divider" style="display: none;"></li>
                    </ul>
                </li>
            </ul>

        </div>
    </div>
</nav>

<jsp:include page="/jsp/game_components/progress_bar_common.jsp"/>

<jsp:include page="/jsp/messages.jsp"/>
