<%--
  ~ Copyright (C) 2023 Code Defenders contributors
  ~
  ~ This file is part of Code Defenders.
  ~
  ~ Code Defenders is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or (at
  ~ your option) any later version.
  ~
  ~ Code Defenders is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
  --%>
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ attribute name="game" required="true" type="org.codedefenders.game.AbstractGame" %>
<%@ attribute name="previousTest" required="false" type="org.codedefenders.game.Test" %>

<button class="btn btn-warning" id="load-previous-test"
${(game.state != 'ACTIVE' || previousTest == null) ? 'disabled' : ''}>
    Clone previous test
</button>

<t:modal title="Clone previous test" id="clone-previous-test-modal" closeButtonText="Cancel">
    <jsp:attribute name="content">
        Are you sure you want to copy the previous test?
        This will overwrite your current code and replace it with your last submission.
    </jsp:attribute>
    <jsp:attribute name="footer">
        <button class="btn btn-primary" id="confirm-clone-previous-test-btn">Confirm Clone</button>
    </jsp:attribute>
</t:modal>

<script type="module">
    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
    import {TestEditor} from '${url.forPath("/js/codedefenders_game.mjs")}';

    /** @type {TestEditor} */
    const testEditor = await objects.await('testEditor');
    const loadPreviousTestButton = document.getElementById('load-previous-test');
    const previousTestCode = `${previousTest != null ? previousTest.asString : ''}`;
    const templateCode = `${game.CUT.testTemplate}`;

    const setEditableLines = (a, b) => {
        testEditor.editableLinesStart = a;
        testEditor.editableLinesEnd = b;
    };

    const loadPreviousTest = () => {
        const {editableLinesStart, editableLinesEnd} = testEditor;
        setEditableLines(0, testEditor.initialNumLines);
        testEditor.editor.setValue(previousTestCode);
        setEditableLines(editableLinesStart, editableLinesEnd);
    };

    const modal = new bootstrap.Modal(document.getElementById('clone-previous-test-modal'));
    document.getElementById('confirm-clone-previous-test-btn').addEventListener('click', event => {
        loadPreviousTest();
        modal.hide();
    });

    if (loadPreviousTestButton && previousTestCode.length > 0) {
        loadPreviousTestButton.addEventListener('click', event => {
            if (testEditor.editor.getValue() === templateCode) {
                loadPreviousTest();
            } else {
                modal.show();
            }
        });
    }
</script>