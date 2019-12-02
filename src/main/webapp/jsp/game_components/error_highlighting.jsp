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
<%@ page import="com.google.gson.Gson" %>
<%@ page import="java.util.List" %>


<%--
    Adds highlighting of error (red) lines to a CodeMirror editor.

    @param String codeDivSelector
        Selector for the div the CodeMirror container is in. Should only contain one CodeMirror instance.
    @param List<Integer> errorLines
        The line numbers of the errors reported by the compiler
--%>

<%--
    The CSS is located in error_highlighting.css.
--%>

<%
    String codeDivSelector = (String) request.getAttribute("codeDivSelectorForError");
    List<Integer> errorLinesList = (List<Integer>) request.getAttribute("errorLines");
    String errorLinesAsJSON = "[]";
    if( errorLinesList != null ){
       Gson gson = new Gson();
       errorLinesAsJSON = gson.toJson(errorLinesList);
    }
%>

<%
if( codeDivSelector != null ) {
%>
<script>
    /* Wrap in a function so it has it's own scope. Inspired by game_highlighting.jsp*/
    (function () {

        /* Game highlighting data. */
        const errorLines = JSON.parse(`<%=errorLinesAsJSON%>`);

        /**
         * Highlights errors on the given CodeMirror instance.
         * @param {object} codeMirror The CodeMirror instance.
         * See: https://creativewebspecialist.co.uk/2013/07/15/highlight-errors-in-codemirror/
         */
        const highlightErrors = function (codeMirror) {
            for (errorLine of errorLines) {
            	// Maybe we need to remove the
                codeMirror.addLineClass(errorLine - 1, 'background', 'line-error');
            }
        };

        const codeMirror = $('<%=codeDivSelector%>').find('.CodeMirror')[0].CodeMirror;
        codeMirror.highlightErrors = function () { highlightErrors(this) };
        codeMirror.highlightErrors();
    }());
</script>
<% } %>
