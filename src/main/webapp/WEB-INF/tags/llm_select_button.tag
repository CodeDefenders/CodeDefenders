<%@ tag import="org.codedefenders.persistence.database.LlmRepository" %>
<%@ tag import="org.codedefenders.util.CDIUtil" %><%--

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

<%@ attribute name="htmlId" required="true" %>

<%
    LlmRepository llmRepo = CDIUtil.getBeanFromCDI(LlmRepository.class);
    request.setAttribute("attackStrategies", llmRepo.getAttackStrategies());
    request.setAttribute("defendStrategies", llmRepo.getDefendStrategies());
%>

<%--
TODO Further generalize for use in llm_select_modal.tag
--%>
<div>
    <div class="mb-3">
        <label for="${htmlId}-defenderSelect" class="form-label">Choose defender model</label>
        <div class="d-flex gap-2 align-items-center">
            <select class="form-select" id="${htmlId}-defenderSelect" name="defenderModel">
                <option id="${htmlId}-no-defender" value="NONE" selected>Don't use an LLM defender
                </option>
            </select>
        </div>
    </div>

    <div class="mb-3">
        <label class="form-label" for="${htmlId}-defenderStrategySelect">Choose defend
            strategy</label>
        <select class="form-select" id="${htmlId}-defenderStrategySelect"
                name="defenderStrat">
            <c:forEach items="${defendStrategies}" var="strat">
                <option id="${htmlId}-def-${strat}-option" value="${strat.name}">
                        ${strat.toString()}
                </option>
            </c:forEach>
        </select>
    </div>

    <div class="mb-3">
        <label for="${htmlId}-attackerSelect" class="form-label">Choose attacker model</label>
        <div class="d-flex gap-2 align-items-center">
            <select class="form-select" id="${htmlId}-attackerSelect" name="attackerModel">
                <option id="${htmlId}-no-attacker" value="NONE" selected>Don't use an LLM attacker
                </option>
            </select>
        </div>
    </div>

    <div class="mb-3">
        <label class="form-label" for="${htmlId}-attackerStrategySelect">Choose attack
            strategy</label>
        <select class="form-select" id="${htmlId}-attackerStrategySelect"
                name="defenderStrat">
            <c:forEach items="${attackStrategies}" var="strat">
                <option id="${htmlId}-def-${strat}-option" value="${strat.name}">
                        ${strat.toString()}
                </option>
            </c:forEach>
        </select>
    </div>

    <script>
        function addOptions(selectElement, activeModels) {

            activeModels.forEach(m => {
                const modelOption = document.createElement("option");
                selectElement.appendChild(modelOption);
                modelOption.value = m.type + "|" + m.name;
                modelOption.textContent = m.type + ": " + m.name;
            });
        }

        (async function () {
            const {InfoApi} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
            const defenderSelect = document.getElementById("${htmlId}-defenderSelect");
            const attackerSelect = document.getElementById("${htmlId}-attackerSelect");

            const activeModels = await InfoApi.getActiveLlms();

            addOptions(defenderSelect, activeModels);
            addOptions(attackerSelect, activeModels);
        })();
    </script>
</div>
