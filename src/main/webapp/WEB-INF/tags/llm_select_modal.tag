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

<%@ attribute name="gameType" required="true" %>
<%@ attribute name="gameId" required="true" %>

<%@ attribute name="htmlId" required="true" %>

<div>
    <form id="setLlmPlayer" action="${url.forPath(gameType.equals("multiplayer") ? "/multiplayergame" : "/meleegame")}"
          method="post">

        <input type="hidden" name="formType" value="setLlmPlayer">
        <input type="hidden" name="gameId" value="${gameId}">
        <t:modal title="Manage LLM players" id="${htmlId}">
                <jsp:attribute name="content">
                    <div id="loading-div" class="loading loading-bg-gray loading-height-200">
                        <div class="mb-3">
                            <label for="defenderSelect" class="form-label">Choose defender model</label>
                            <select class="form-select" id="defenderSelect" name="defenderModel">
                                <option id="no-defender" value="NONE">Don't use an LLM defender
                                </option>
                            </select>
                        </div>

                        <div class="mb-3">
                            <label for="attackerSelect" class="form-label">Choose attacker model</label>
                            <select class="form-select" id="attackerSelect" name="attackerModel">
                                <option id="no-attacker" value="NONE">Don't use an LLM attacker
                                </option>
                            </select>
                        </div>
                    </div>
                </jsp:attribute>
            <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-primary">Confirm</button>
                </jsp:attribute>
        </t:modal>
    </form>

    <script>

        function removeOptions(selectElement) {
            for (let i = selectElement.children.length - 1; i >= 0; i--) {
                const child = selectElement.children[i];
                if (child.value !== "NONE") {
                    selectElement.removeChild(child);
                }
            }
        }

        function addOptions(selectElement, activeModels, selectedModel) {

            activeModels.forEach(m => {
                const modelOption = document.createElement("option");
                modelOption.selected = (selectedModel != null && m.name === selectedModel.name
                        && m.type === selectedModel.type);
                selectElement.appendChild(modelOption);
                modelOption.value = m.type + "|" + m.name;
                modelOption.textContent = m.type + ": " + m.name;
            });
        }

        (async function () {
            const {InfoApi} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
            const modal = document.getElementById("${htmlId}")
            const loadingDiv = document.getElementById("loading-div");
            const defenderSelect = document.getElementById("defenderSelect");
            const attackerSelect = document.getElementById("attackerSelect");

            modal.addEventListener('shown.bs.modal', async function () {
                const defenderModel = await InfoApi.getLlmForGame(${gameId}, "DEFENDER");
                const attackerModel = await InfoApi.getLlmForGame(${gameId}, "ATTACKER");
                const activeModels = await InfoApi.getActiveLlms();

                const noDefenderOption = document.getElementById("no-defender");
                noDefenderOption.selected = defenderModel == null;
                const noAttackerOption = document.getElementById("no-attacker");
                noAttackerOption.selected = attackerModel == null;

                addOptions(defenderSelect, activeModels, defenderModel);
                addOptions(attackerSelect, activeModels, attackerModel);
                loadingDiv.classList.remove("loading")
            });

            modal.addEventListener('hidden.bs.modal', () => {
                loadingDiv.classList.add("loading");
                removeOptions(defenderSelect);
                removeOptions(attackerSelect);
            })

        })();
    </script>
</div>
