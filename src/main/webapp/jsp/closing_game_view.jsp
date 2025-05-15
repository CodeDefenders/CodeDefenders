<%--

    Copyright (C) 2025 Code Defenders contributors

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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="game" type="org.codedefenders.game.multiplayer.MultiplayerGame"--%>

<c:set var="title" value="${'Game ' += game.id += ' is closing.'}"/>

<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/closing_game.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <div class="content">
            <div class="branding">
                <img src="${url.forPath("/images/logo.png")}"
                     alt="Code Defenders Logo"
                     width="58">
                <h1>${title}</h1>
            </div>
            <p>
                The game is currently closing&hellip;<br>
                User interactions are no longer possible while open equivalence duels are automatically resolved.
            </p>
        </div>
        <t:game_js_init/>
    </jsp:body>
</p:main_page>

