<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
<%--@elvariable id="gameHighlighting" type="org.codedefenders.beans.game.GameHighlightingBean"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%@ page import="org.codedefenders.util.Paths" %>

<%--
    Adds highlighting of coverage (green lines) and mutants (gutter icons) to a CodeMirror editor.

    The game highlighting uses these HTML elements:
        - Mutant Icons:
            <div class="gh-mutant-icons">
                <div class="gh-mutant-icon">
                    ::after
                </div>
            </div>

    The CSS is located in codemirror_customize.css.
--%>

<c:set var="scoreboardBaseUrl" value="${
    gameHighlighting.gameMode == 'MELEE' ? Paths.MELEE_GAME_SCOREBOARD :
    gameHighlighting.gameMode == 'PARTY' ? Paths.BATTLEGROUND_GAME_SCOREBOARD :
    ''}" />

<script type="module">
    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
    import {GameHighlighting} from '${url.forPath("/js/codedefenders_game.mjs")}';

    const data = JSON.parse('${gameHighlighting.JSON}');
    const enableFlagging = Boolean(${gameHighlighting.enableFlagging});
    const gameId = Number(${gameHighlighting.gameId});

    const gameHighlighting = await new GameHighlighting(data, enableFlagging, gameId)
        .initAsync();

    gameHighlighting.highlightCoverage();
    gameHighlighting.highlightMutants();

    <c:if test="${not empty scoreboardBaseUrl}">
    function updateScoreboard() {
        fetch("${url.forPath(scoreboardBaseUrl)}?gameId=${gameHighlighting.gameId}")
            .then(r => {
                if (r.ok) {
                    r.text().then(text =>
                        document.querySelector('#scoreboard .modal-body').innerHTML = text.trim()
                    );
                } else {
                    console.error("Failed to update scoreboard: " + r.status);
                }
            });
    }

    const pushSocket = await objects.await('pushSocket');
    pushSocket.register('highlighting.MutantAddedEvent', updateScoreboard);
    pushSocket.register('highlighting.TestAddedEvent', updateScoreboard);
    </c:if>

    objects.register('gameHighlighting', gameHighlighting);
</script>
