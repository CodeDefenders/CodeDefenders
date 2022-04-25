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

<%--
    Adds highlighting of error (red) lines to a CodeMirror editor.

    The CSS is located in error_highlighting.css.
--%>

<jsp:useBean id="mutantErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>

<script>
    /* Wrap in a function to avoid polluting the global scope. */
    (async function () {
        const errorLines = JSON.parse('${mutantErrorHighlighting.errorLinesJSON}');

        const mutantEditor = await CodeDefenders.objects.await('mutantEditor');

        const mutantErrorHighlighting = new CodeDefenders.ErrorHighlighting(
                errorLines, mutantEditor.editor);
        mutantErrorHighlighting.highlightErrors();

        CodeDefenders.objects.register('mutantErrorHighlighting', mutantErrorHighlighting);
    })();
</script>
