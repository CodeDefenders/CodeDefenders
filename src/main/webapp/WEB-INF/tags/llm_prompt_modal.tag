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
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="classViewer" type="org.codedefenders.beans.game.ClassViewerBean"--%>

<%@ attribute name="type" required="true" %>
<%@ attribute name="name" required="true" %>

<%@ attribute name="attackerPrompt" required="true" %>
<%@ attribute name="attackerDeps" required="true" %>
<%@ attribute name="attackerDepsPrompt" required="true" %>
<%@ attribute name="attackerFocus" required="true" %>
<%@ attribute name="attackerFocusPrompt" required="true" %>
<%@ attribute name="defenderPrompt" required="true" %>
<%@ attribute name="defenderDeps" required="true" %>
<%@ attribute name="defenderDepsPrompt" required="true" %>
<%@ attribute name="defenderFocus" required="true" %>
<%@ attribute name="defenderFocusPrompt" required="true" %>

<%@ attribute name="htmlId" required="true" %>

<div>
    <form action="${url.forPath("api/llm")}?formType=updatePrompts" method="post">
        <t:modal title="${type} — ${name}" id="${htmlId}"
                 modalDialogClasses="modal-dialog-responsive" closeButtonText="Cancel">
        <jsp:attribute name="content">
            <div class="card" id="llm-prompt-modal-card-${htmlId}">
                <div class="card-header">
                    <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1 active" data-bs-toggle="tab"
                                    id="defender-header-${htmlId}"
                                    data-bs-target="#defender-body-${htmlId}"
                                    aria-controls="defender-body-${htmlId}"
                                    type="button" role="tab" aria-selected="true">
                                Defender Prompts
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1" data-bs-toggle="tab"
                                    id="attacker-header-${htmlId}"
                                    data-bs-target="#attacker-body-${htmlId}"
                                    aria-controls="attacker-body-${htmlId}"
                                    type="button" role="tab" aria-selected="false">
                                Attacker Prompts
                            </button>
                        </li>
                    </ul>
                </div>
                <div class="card-body p-0">

                    <div class="tab-content">
                        <div class="tab-pane active"
                             id="defender-body-${htmlId}"
                             aria-labelledby="defender-header-${htmlId}"
                             role="tabpanel">
                            <div class="row g-3 p-2">
                                <div class="col-12">
                                    <label for="defender-standard-${htmlId}" class="form-label">
                                        Standard prompt
                                    </label>
                                    <textarea class="form-control" rows="5" id="defender-standard-${htmlId}"
                                              name="defenderPrompt">${defenderPrompt}</textarea>
                                </div>

                                <div class="col-12">
                                    <div class="form-check form-switch"
                                         title="Send dependency code as part of the prompt">
                                        <input class="form-check-input" type="checkbox" id="defender-deps-${htmlId}"
                                               name="defenderDependencies" ${defenderDeps.equals("true") ? "checked" : ""}>
                                        <label class="form-check-label" for="defender-deps-${htmlId}">Include
                                            dependencies</label>
                                    </div>
                                    <label for="defender-deps-prompt-${htmlId}" class="form-label">
                                        Dependency prompt
                                    </label>
                                    <textarea class="form-control" rows="5" id="defender-deps-prompt-${htmlId}"
                                              name="defenderDependencyPrompt"
                                        ${defenderDeps.equals("false") ? "disabled" : ""}>${defenderDepsPrompt}</textarea>
                                </div>

                                <div class="col-12">
                                    <div class="form-check form-switch"
                                         title="Use prompts to focus on special methods">
                                        <input class="form-check-input" type="checkbox" id="defender-focus-${htmlId}"
                                               name="defenderMethodFocus" ${defenderFocus.equals("true") ? "checked" : ""}>
                                        <label class="form-check-label" for="defender-focus-${htmlId}">Use prompts to
                                            focus
                                            on special methods</label>
                                    </div>
                                    <label for="defender-focus-prompt-${htmlId}" class="form-label">
                                        Focus prompt
                                    </label>
                                    <textarea class="form-control" rows="5" id="defender-focus-prompt-${htmlId}"
                                              name="defenderMethodFocusPrompt"
                                        ${defenderFocus.equals("false") ? "disabled" : ""}>${defenderFocusPrompt}</textarea>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="tab-content">
                        <div class="tab-pane"
                             id="attacker-body-${htmlId}"
                             aria-labelledby="attacker-header-${htmlId}"
                             role="tabpanel">
                            <div class="row g-3 p-2">
                                <div class="col-12">
                                    <label for="attacker-standard-${htmlId}" class="form-label">
                                        Standard prompt
                                    </label>
                                    <textarea class="form-control" rows="5" id="attacker-standard-${htmlId}"
                                              name="attackerPrompt">${attackerPrompt}</textarea>
                                </div>

                                <div class="col-12">
                                    <div class="form-check form-switch"
                                         title="Send dependency code as part of the prompt">
                                        <input class="form-check-input" type="checkbox" id="attacker-deps-${htmlId}"
                                               name="attackerDependencies" ${attackerDeps.equals("true") ? "checked" : ""}>
                                        <label class="form-check-label" for="attacker-deps-${htmlId}">Include
                                            dependencies</label>
                                    </div>
                                    <label for="attacker-deps-prompt-${htmlId}" class="form-label">
                                        Dependency prompt
                                    </label>
                                    <textarea class="form-control" rows="5" id="attacker-deps-prompt-${htmlId}"
                                              name="attackerDependencyPrompt"
                                        ${attackerDeps.equals("false") ? "disabled" : ""}>${attackerDepsPrompt}</textarea>
                                </div>

                                <div class="col-12">
                                    <div class="form-check form-switch"
                                         title="Use prompts to focus on special methods">
                                        <input class="form-check-input" type="checkbox" id="attacker-focus-${htmlId}"
                                               name="attackerMethodFocus" ${attackerFocus.equals("true") ? "checked" : ""}>
                                        <label class="form-check-label" for="attacker-focus-${htmlId}">Use prompts to
                                            focus
                                            on special methods</label>
                                    </div>
                                    <label for="attacker-focus-prompt-${htmlId}" class="form-label">
                                        Focus prompt
                                    </label>
                                    <textarea class="form-control" rows="5" id="attacker-focus-prompt-${htmlId}"
                                              name="attackerMethodFocusPrompt"
                                        ${attackerFocus.equals("false") ? "disabled" : ""}>${attackerFocusPrompt}</textarea>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </jsp:attribute>
            <jsp:attribute name="footer">

            <button type="button" class="btn btn-primary" data-bs-dismiss="modal" id="update-button-${htmlId}">Save changes</button>

        </jsp:attribute>
        </t:modal>
    </form>

    <script>
        (function () {
            const modal = document.currentScript.parentElement.querySelector('.modal');
            const submitButton = modal.querySelector("#update-button-${htmlId}");
            submitButton.addEventListener("click", () => {
                const defenderPrompt = modal.querySelector("#defender-standard-${htmlId}").value.trim();
                const defenderDeps = modal.querySelector("#defender-deps-${htmlId}").checked;
                const defenderDepsPrompt = modal.querySelector("#defender-deps-prompt-${htmlId}").value.trim();
                const defenderFocus = modal.querySelector("#defender-focus-${htmlId}").checked;
                const defenderFocusPrompt = modal.querySelector("#defender-focus-prompt-${htmlId}").value.trim();

                const attackerPrompt = modal.querySelector("#attacker-standard-${htmlId}").value.trim();
                const attackerDeps = modal.querySelector("#attacker-deps-${htmlId}").checked;
                const attackerDepsPrompt = modal.querySelector("#attacker-deps-prompt-${htmlId}").value.trim();
                const attackerFocus = modal.querySelector("#attacker-focus-${htmlId}").checked;
                const attackerFocusPrompt = modal.querySelector("#attacker-focus-prompt-${htmlId}").value.trim();

                fetch("${url.forPath("api/llm")}?formType=updatePrompts", {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        type: "${type}",
                        name: "${name}",
                        active: true,
                        defenderPrompt: defenderPrompt,
                        defenderDependencies: defenderDeps,
                        defenderDependencyPrompt: defenderDepsPrompt,
                        defenderMethodFocus: defenderFocus,
                        defenderMethodFocusPrompt: defenderFocusPrompt,
                        attackerPrompt: attackerPrompt,
                        attackerDependencies: attackerDeps,
                        attackerDependencyPrompt: attackerDepsPrompt,
                        attackerMethodFocus: attackerFocus,
                        attackerMethodFocusPrompt: attackerFocusPrompt
                    })
                });
            });

            const defenderDepsButton = modal.querySelector("#defender-deps-${htmlId}");
            const defenderDepsPrompt = modal.querySelector("#defender-deps-prompt-${htmlId}");
            defenderDepsButton.addEventListener("click", () => {
                defenderDepsPrompt.disabled = !defenderDepsButton.checked;
            });

            const defenderFocusButton = modal.querySelector("#defender-focus-${htmlId}");
            const defenderFocusPrompt = modal.querySelector("#defender-focus-prompt-${htmlId}");
            defenderFocusButton.addEventListener("click", () => {
                defenderFocusPrompt.disabled = !defenderFocusButton.checked;
            });

            const attackerDepsButton = modal.querySelector("#attacker-deps-${htmlId}");
            const attackerDepsPrompt = modal.querySelector("#attacker-deps-prompt-${htmlId}");
            attackerDepsButton.addEventListener("click", () => {
                attackerDepsPrompt.disabled = !attackerDepsButton.checked;
            });

            const attackerFocusButton = modal.querySelector("#attacker-focus-${htmlId}");
            const attackerFocusPrompt = modal.querySelector("#attacker-focus-prompt-${htmlId}");
            attackerFocusButton.addEventListener("click", () => {
                attackerFocusPrompt.disabled = !attackerFocusButton.checked;
            });
        })();
    </script>
</div>
