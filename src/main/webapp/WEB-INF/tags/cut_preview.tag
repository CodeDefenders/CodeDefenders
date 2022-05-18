<%--
  ~ Copyright (C) 2021 Code Defenders contributors
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

<div id="create-game-cut-preview" class="w-100 h-100">
<div class="card" style="height: 100%; min-height: 200px; resize: vertical; overflow: auto;">
    <div class="card-body p-0 codemirror-fill w-100 h-100">
        <pre class="m-0"><textarea name=""></textarea></pre>
    </div>
</div>

<script type="module">
    import CodeMirror from './js/codemirror.mjs';

    import {InfoApi} from './js/codedefenders_main.mjs';


    const cutPreview = document.querySelector('#create-game-cut-preview')

    const classSelector = document.querySelector('#class-select');
    const textarea = cutPreview.querySelector('textarea');

    const updatePreview = function () {
        const classId = Number(classSelector.value);
        const codeMirrorContainer = cutPreview.querySelector('.CodeMirror');

        if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
            InfoApi.setClassEditorValue(codeMirrorContainer.CodeMirror, classId);
        } else {
            const editor = CodeMirror.fromTextArea(textarea, {
                lineNumbers: true,
                readOnly: true,
                mode: 'text/x-java',
                autoRefresh: true
            });
            editor.getWrapperElement().classList.add('codemirror-readonly');
            InfoApi.setClassEditorValue(editor, classId);
        }
    };

    // Load initial selected class
    document.addEventListener("DOMContentLoaded", updatePreview);

    classSelector.addEventListener('change', updatePreview);
</script>
</div>
