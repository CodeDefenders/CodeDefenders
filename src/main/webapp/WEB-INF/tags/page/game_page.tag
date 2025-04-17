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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%@ tag import="org.codedefenders.game.AbstractGame" %>
<%@ tag import="org.codedefenders.game.Role" %>
<%@ tag import="org.codedefenders.game.multiplayer.MeleeGame" %>
<%@ tag import="org.codedefenders.game.multiplayer.MultiplayerGame" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>
<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>

<%@ attribute name="additionalImports" required="false" fragment="true"
              description="Additional CSS or JavaScript tags to include in the head." %>

<%--
    Adds game header, game container and game-specific JS/CSS to the main page layout.
--%>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
    AbstractGame game = (AbstractGame) request.getAttribute("game");

    Role role = null;
    if (game instanceof MeleeGame) {
        role = ((MeleeGame) game).getRole(login.getUserId());
    } else if (game instanceof MultiplayerGame) {
        role = ((MultiplayerGame) game).getRole(login.getUserId());
    }

    String roleStr = role.getFormattedString();
    if (roleStr == null) {
        roleStr = "no role";
    }

    request.setAttribute("title", "Game " + game.getId() + " (" + roleStr + ")");
%>

<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">
        <jsp:invoke fragment="additionalImports"/>
    </jsp:attribute>

    <jsp:body>
        <t:game_js_init/>

        <div id="game-container" class="container-fluid">
            <t:game_header/>
            <jsp:doBody/>
        </div>
    </jsp:body>
</p:main_page>
