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

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<div id="create-game-cut-preview" class="w-100 h-100">
    <div class="card loading loading-bg-gray loading-height-200" id="class-preview-card"
         style="height: 100%; min-height: 200px; resize: vertical; overflow: auto;">
        <div class="card-header" hidden>
            <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link py-1 active" data-bs-toggle="tab"
                            id="cut-header"
                            data-bs-target="#cut-body"
                            aria-controls="cut-body"
                            type="button" role="tab" aria-selected="true">
                    </button>
                </li>
            </ul>
        </div>
        <div class="card-body p-0 codemirror-expand codemirror-class-modal-size">
            <div class="tab-content">
                <div class="tab-pane active"
                     id="cut-body"
                     aria-labelledby="cut-header"
                     role="tabpanel">
                            <pre class="m-0"><textarea id="cut-area" name="cut" title="cut"
                                                       readonly></textarea></pre>
                </div>
            </div>
        </div>
    </div>

    <script type="module">
        const {DynamicClassViewer} = await import('${url.forPath("/js/codedefenders_main.mjs")}');


        const cutPreview = document.querySelector('#create-game-cut-preview')

        const classSelector = document.querySelector('#class-select');

        const updatePreview = async function () {
            const classId = Number(classSelector.value);
            const card = cutPreview.querySelector('#class-preview-card');
            await DynamicClassViewer.show_code(classId, card);
        };

        // Load initial selected class
        updatePreview();
        classSelector.addEventListener('change', updatePreview);
    </script>
</div>
