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

<%--
    Adds highlighting of error (red) lines to a CodeMirror editor.

    The CSS is located in error_highlighting.css.
--%>

<jsp:useBean id="mutantErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>

<script type="module">
    import {ErrorHighlighting, objects} from '';


    const errorLines = JSON.parse('${mutantErrorHighlighting.errorLinesJSON}');

    const mutantEditor = await objects.await('mutantEditor');

    const mutantErrorHighlighting = new ErrorHighlighting(errorLines, mutantEditor.editor);
    mutantErrorHighlighting.highlightErrors();


    objects.register('mutantErrorHighlighting', mutantErrorHighlighting);
</script>
