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
<%--
    Displays the test code in a CodeMirror textarea.
--%>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>

<%--<jsp:useBean id="testEditor" class="org.codedefenders.beans.game.TestEditorBean" scope="request"/>--%>

<div class="card game-component-resize loading">
    <div class="card-body p-0 codemirror-fill">
        <pre class="m-0"><textarea id="test-code" name="test" title="test">${testEditor.testCode}</textarea></pre>
    </div>
    <div class="card-footer">
        <span class="align-content-end me-1">Assertion Library: ${testEditor.assertionLibrary.description}</span>
    </div>
</div>

<script type="module">
    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
    import {TestEditor} from '${url.forPath("/js/codedefenders_game.mjs")}';


    const editableLinesStart = ${testEditor.hasEditableLinesStart() ? testEditor.editableLinesStart : "null"};
    const editableLinesEnd = ${testEditor.hasEditableLinesEnd() ? testEditor.editableLinesEnd : "null"};
    const mockingEnabled = ${testEditor.mockingEnabled};
    const assertionLibrary = '${testEditor.assertionLibrary.name()}';
    const keymap = '${login.user.keyMap.CMName}';
    const readonly = Boolean(${testEditor.readonly ? 1 : 0});

    const editorElement = document.getElementById('test-code');

    const testEditor = new TestEditor(
        editorElement,
        editableLinesStart,
        editableLinesEnd,
        mockingEnabled,
        assertionLibrary,
        keymap,
        readonly);


    objects.register('testEditor', testEditor);
</script>
