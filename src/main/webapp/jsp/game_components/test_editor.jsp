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
    Displays the test code in a CodeMirror textarea.
--%>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<jsp:useBean id="testEditor" class="org.codedefenders.beans.game.TestEditorBean" scope="request"/>

<div class="card game-component-resize">
    <div class="card-body p-0 codemirror-fill">
        <pre class="m-0"><textarea id="test-code" name="test" title="test">${testEditor.testCode}</textarea></pre>
    </div>
</div>

<script>
    /* Wrap in a function to avoid polluting the global scope. */
    (function () {
        const editableLinesStart = ${testEditor.hasEditableLinesStart() ? testEditor.editableLinesStart : "null"};
        const editableLinesEnd = ${testEditor.hasEditableLinesEnd() ? testEditor.editableLinesEnd : "null"};
        const mockingEnabled = ${testEditor.mockingEnabled};
        const keymap = '${login.user.keyMap.CMName}';

        const editorElement = document.getElementById('test-code');

        CodeDefenders.objects.testEditor = new CodeDefenders.TestEditor(
                editorElement,
                editableLinesStart,
                editableLinesEnd,
                mockingEnabled,
                keymap);
    })();
</script>
