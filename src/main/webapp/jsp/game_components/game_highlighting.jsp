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

<script type="module">
    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
    import {GameHighlighting} from '${url.forPath("/js/codedefenders_game.mjs")}';


    const data = JSON.parse('${gameHighlighting.JSON}');
    const enableFlagging = Boolean(${gameHighlighting.enableFlagging});
    const gameId = Number(${gameHighlighting.gameId});

    const gameHighlighting = await new GameHighlighting(
            data,
            enableFlagging,
            gameId).initAsync();

    gameHighlighting.highlightCoverage();
    gameHighlighting.highlightMutants();


    objects.register('gameHighlighting', gameHighlighting);
</script>
