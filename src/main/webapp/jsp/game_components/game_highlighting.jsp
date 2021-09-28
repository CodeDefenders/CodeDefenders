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

<%--@elvariable id="gameHighlighting" type="org.codedefenders.beans.game.GameHighlightingBean"--%>

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

<script type="text/javascript" src="js/game_highlighting.js"></script>

<script>
    /* Wrap in a function to avoid polluting the global scope. */
    (function () {
        const data = JSON.parse('${gameHighlighting.JSON}');
        const enableFlagging = Boolean(${gameHighlighting.enableFlagging});
        const flaggingUrl = '${pageContext.request.contextPath}${Paths.EQUIVALENCE_DUELS_GAME}';
        const gameId = ${gameHighlighting.gameId};

        CodeDefenders.objects.gameHighlighting = new CodeDefenders.classes.GameHighlighting(data,
                enableFlagging,
                flaggingUrl,
                gameId);

        CodeDefenders.objects.gameHighlighting.highlightCoverage();
        CodeDefenders.objects.gameHighlighting.highlightMutants();
    })();
</script>
