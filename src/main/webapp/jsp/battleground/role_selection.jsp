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
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    request.setAttribute("gameId", game.getId());
%>

<p:main_page title="Select a role">
    <div class="d-flex justify-content-center">
        <h2>You have been invited to game #${game.id}</h2>
    </div>
    <div class="d-flex justify-content-center">
        <a class="btn btn-lg btn-attacker m-3" href="${url.forPath("invite")}?inviteId=${inviteId}&role=attacker">
            Join as attacker
        </a>
        <a class="btn btn-lg btn-secondary m-3" href="${url.forPath("invite")}?inviteId=${inviteId}&role=flex">
            Balance teams
        </a>
        <a class="btn btn-lg btn-defender m-3" href="${url.forPath("invite")}?inviteId=${inviteId}&role=defender">
            Join as defender
        </a>
    </div>
</p:main_page>
