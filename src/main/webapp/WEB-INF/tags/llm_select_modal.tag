<%@ tag import="org.codedefenders.model.llm.LlmStrategy" %>
<%@ tag import="org.codedefenders.persistence.database.LlmRepository" %>
<%@ tag import="org.codedefenders.util.CDIUtil" %>
<%@ tag import="org.codedefenders.game.Role" %><%--

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
<%--@elvariable id="LlmStrategy" type="org.codedefenders.model.llm.LlmStrategy--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%@ attribute name="gameType" required="true" %>
<%@ attribute name="gameId" required="true" %>

<%@ attribute name="htmlId" required="true" %>
<%
    LlmRepository llmRepo = CDIUtil.getBeanFromCDI(LlmRepository.class);
    request.setAttribute("attackStrategies", llmRepo.getAttackStrategies());
    request.setAttribute("defendStrategies", llmRepo.getDefendStrategies());
%>

<div>

    <t:modal title="${i18n.tr('Manage LLM players')}" id="${htmlId}" modalDialogClasses="modal-lg">
                <jsp:attribute name="content">
                    <div id="${htmlId}-loading-div" class="loading loading-bg-gray loading-height-200">
                        <div class="mb-3 container">

                            <div class="row mb-4 p-2 border">
                                <div class="col-6">
                                    <label for="${htmlId}-defenderSelect" class="form-label">${i18n.tr('Choose defender model')}</label>
                                    <select class="form-select" id="${htmlId}-defenderSelect" name="defenderModel">
                                        <option id="${htmlId}-no-defender" value="NONE">${i18n.tr('Don\'t use an LLM defender')}</option>
                                    </select>
                                </div>

                                <div class="col-5">
                                    <label class="form-label" for="${htmlId}-defenderStrategySelect">${i18n.tr('Choose defend strategy')}</label>
                                    <select class="form-select" id="${htmlId}-defenderStrategySelect"
                                            name="defenderStrat">
                                    <c:forEach items="${defendStrategies}" var="strat">
                                            <option id="${htmlId}-def-${strat}-option" value="${strat.name}">
                                                    ${strat.toString()}
                                            </option>
                                    </c:forEach>
                                    </select>
                                </div>

                                <div class="col-1 align-self-end">
                                    <i id="${htmlId}-defender-error-icon" class="fa fa-exclamation-triangle fa-2x"
                                       hidden="hidden"></i>
                                </div>
                            </div>

                            <div class="row p-2 border">
                                <div class="col-6">
                                    <label for="${htmlId}-attackerSelect" class="form-label">${i18n.tr('Choose attacker model')}</label>
                                    <select class="form-select" id="${htmlId}-attackerSelect" name="attackerModel">
                                        <option id="${htmlId}-no-attacker" value="NONE">${i18n.tr('Don\'t use an LLM attacker')}</option>
                                    </select>
                                </div>
                                <div class="col-5">
                                    <label class="form-label" for="${htmlId}-attackerStrategySelect">${i18n.tr('Choose attack strategy')}</label>
                                    <select class="form-select" id="${htmlId}-attackerStrategySelect"
                                            name="defenderStrat">
                                    <c:forEach items="${attackStrategies}" var="strat">
                                            <option id="${htmlId}-def-${strat}-option" value="${strat.name}">
                                                    ${strat.toString()}
                                            </option>
                                    </c:forEach>
                                    </select>
                                </div>
                                <div class="col-1 align-self-end">
                                    <i id="${htmlId}-attacker-error-icon" class="fa fa-exclamation-triangle fa-2x"
                                       hidden="hidden"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </jsp:attribute>
        <jsp:attribute name="footer">
            <button type="button" id="${htmlId}-open-conversation-modal-button" class="btn btn-outline-dark"
                    data-bs-toggle="modal" data-bs-target="#${htmlId}-conversation-modal">
                <i class="fa fa-comments"></i>${i18n.tr('See conversations')}</button>
                    <button type="button" id="${htmlId}-submit-button" class="btn btn-primary">${i18n.tr('Confirm')}</button>

        </jsp:attribute>
    </t:modal>
    <t:modal title="${i18n.tr('LLM conversations')}" id="${htmlId}-conversation-modal" modalDialogClasses="modal-restricted-height">
                <jsp:attribute name="content">
                    <div id="${htmlId}-loading-cons-div" class="loading loading-bg-gray loading-height-200">
                        <div class="mb-3" id="${htmlId}-conversation-panel">
                        </div>
                    </div>
                </jsp:attribute>
    </t:modal>
    <script>

        async function getError(role) {
            const response = await fetch("${url.forPath("api/llm")}?action=error&gameId=" + ${gameId} +"&role=" + role, {
                method: 'GET',
                headers: {
                    'Content-Type': 'text/plain'
                }
            });
            if (!response.ok) {
                return Promise.reject();
            }
            return await response.text();
        }

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

        function setStrategy(selectElement, selectedStrategy) {
            if (selectedStrategy == null) {
                return;
            }
            const options = selectElement.children;
            for (let i = 0; i < options.length; i++) {
                options[i].selected = options[i].value === selectedStrategy;
            }
        }

        function addConversations(conversations, conversationPanel) {

            while (conversationPanel.hasChildNodes()) {
                conversationPanel.removeChild(conversationPanel.firstChild);
            }
            conversations.forEach((c, index) => {
                const conCard = document.createElement("div");
                conCard.classList.add("card")
                const header = document.createElement("div");
                header.classList.add("card-header");
                if (c.role === "DEFENDER") {
                    header.classList.add("bg-defender")
                } else if (c.role === "ATTACKER") {
                    header.classList.add("bg-attacker");
                } else {
                    console.log("Unknown conversation role: " + c.role)
                }
                conCard.appendChild(header);
                const toggleButton = document.createElement("button");
                toggleButton.classList.add("btn", "collapsed");
                if (c.success) {
                    toggleButton.classList.add("btn-success");
                } else if (c.active) {
                    toggleButton.classList.add("btn-secondary");
                } else {
                    toggleButton.classList.add("btn-danger");
                }

                toggleButton.setAttribute("data-bs-toggle", "collapse");
                toggleButton.setAttribute("data-bs-target", "#${htmlId}-collapse-" + index);
                toggleButton.textContent = c.type + "(" + c.messages.filter(msg => msg.messageType === "AI").length + " tries)";
                header.appendChild(toggleButton);
                const collapse = document.createElement("div");
                collapse.id = "${htmlId}-collapse-" + index;
                collapse.classList.add("collapse");
                collapse.setAttribute("data-bs-parent", "#" + conversationPanel.id);
                conCard.appendChild(collapse);
                const body = document.createElement("div");
                body.classList.add("card-body");
                c.messages.forEach(dto => {
                    const messageCard = document.createElement("div");
                    messageCard.classList.add("card");
                    const messageHeader = document.createElement("header");
                    messageHeader.classList.add("card-header");
                    if (dto.messageType === "AI") {
                        messageHeader.textContent = dto.model + " (" + dto.timestamp + ")";
                    } else {
                        messageHeader.textContent = dto.messageType + " (" + dto.timestamp + ")";
                    }
                    messageCard.appendChild(messageHeader);

                    const messageBody = document.createElement("div");
                    messageBody.setAttribute("style", "white-space: pre-wrap");
                    messageBody.innerHTML = dto.message.replaceAll("\n", "<br>");
                    messageCard.appendChild(messageBody);

                    body.appendChild(messageCard);
                })

                collapse.appendChild(body);

                conversationPanel.appendChild(conCard);
            })
        }

        (async function () {
            const {InfoApi} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
            const modal = document.getElementById("${htmlId}")
            const loadingDiv = document.getElementById("${htmlId}-loading-div");
            const defenderSelect = document.getElementById("${htmlId}-defenderSelect");
            const defenderStrategySelect = document.getElementById("${htmlId}-defenderStrategySelect")
            const attackerStrategySelect = document.getElementById("${htmlId}-attackerStrategySelect")
            const attackerSelect = document.getElementById("${htmlId}-attackerSelect");
            const submitButton = document.getElementById("${htmlId}-submit-button");
            submitButton.addEventListener('click', async function () {
                const params = new URLSearchParams();
                params.append("formType", "setLlmPlayer");
                params.append("gameId", "${gameId}");
                params.append("defenderModel", defenderSelect.value);
                params.append("defenderStrategy", defenderStrategySelect.value)
                params.append("attackerModel", attackerSelect.value);
                params.append("attackerStrategy", attackerStrategySelect.value)
                await fetch("${url.forPath(gameType.equals("multiplayer") ? "/multiplayergame" : "/meleegame")}", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    body: params.toString()
                });
                location.reload();
            })


            modal.addEventListener('shown.bs.modal', async function () {
                const defenderInfo = await InfoApi.getLlmForGame(${gameId}, "DEFENDER");
                const defenderModel = defenderInfo !== null ? defenderInfo.model : null;
                const defenderStrategy = defenderInfo !== null ? defenderInfo.strategy : null;
                const attackerInfo = await InfoApi.getLlmForGame(${gameId}, "ATTACKER");
                const attackerModel = attackerInfo !== null ? attackerInfo.model : null;
                const attackerStrategy = attackerInfo !== null ? attackerInfo.strategy : null;
                const activeModels = await InfoApi.getActiveLlms();
                const defenderError = await getError("DEFENDER");
                const attackerError = await getError("ATTACKER");

                const noDefenderOption = document.getElementById("${htmlId}-no-defender");

                noDefenderOption.selected = defenderModel == null;
                const noAttackerOption = document.getElementById("${htmlId}-no-attacker");
                noAttackerOption.selected = attackerModel == null;

                const conversations = await InfoApi.getLlmConversations(${gameId});
                const conversationPanel = document.getElementById("${htmlId}-conversation-panel");
                addConversations(conversations, conversationPanel);
                document.getElementById("${htmlId}-loading-cons-div").classList.remove("loading")


                addOptions(defenderSelect, activeModels, defenderModel);
                addOptions(attackerSelect, activeModels, attackerModel);

                setStrategy(defenderStrategySelect, defenderStrategy);
                setStrategy(attackerStrategySelect, attackerStrategy);

                if (defenderError.length > 0) {
                    const defenderErrorIcon = document.getElementById("${htmlId}-defender-error-icon");
                    defenderErrorIcon.removeAttribute("hidden");
                    defenderErrorIcon.parentElement.title = defenderError;
                }
                if (attackerError.length > 0) {
                    const attackerErrorItem = document.getElementById("${htmlId}-attacker-error-icon");
                    attackerErrorItem.removeAttribute("hidden");
                    attackerErrorItem.parentElement.title = attackerError;
                }

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
