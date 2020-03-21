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

<% if(mutantErrorHighlighting.hasErrorLines()) { %>
<script>
    /* Wrap in a function so it has it's own scope. Inspired by game_highlighting.jsp*/
    (function () {

        /* Game highlighting data. */
        const errorLines = JSON.parse('${mutantErrorHighlighting.errorLinesJSON}');

        /**
         * Highlights errors on the given CodeMirror instance.
         * @param {object} codeMirror The CodeMirror instance.
         * See: https://creativewebspecialist.co.uk/2013/07/15/highlight-errors-in-codemirror/
         */
        const highlightErrors = function (codeMirror) {
            for (const errorLine of errorLines) {
            	// Maybe we need to remove the
                codeMirror.addLineClass(errorLine - 1, 'background', 'line-error');
            }
        };

        const codeMirror = $('${mutantErrorHighlighting.codeDivSelector}').find('.CodeMirror')[0].CodeMirror;
        codeMirror.highlightErrors = function () { highlightErrors(this) };
        codeMirror.highlightErrors();
    }());
</script>
<% } %>
