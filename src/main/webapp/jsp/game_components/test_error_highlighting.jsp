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

<jsp:useBean id="testErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>

<script type="module">
    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
    import {ErrorHighlighting} from '${url.forPath("/js/codedefenders_game.mjs")}';


    const errorLines = JSON.parse('${testErrorHighlighting.errorLinesJSON}');

    const testEditor = await objects.await('testEditor');

    const testErrorHighlighting= new ErrorHighlighting(errorLines, testEditor.editor);
    testErrorHighlighting.highlightErrors();


    objects.register('testErrorHighlighting', testErrorHighlighting);
</script>
