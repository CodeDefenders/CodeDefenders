<%@ page import="org.codedefenders.model.llm.LlModel" %>
<%@ page import="java.util.List" %><%--

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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="settingsRepository" type="org.codedefenders.persistence.database.SettingsRepository"--%>

<%
    @SuppressWarnings("unchecked")
    List<LlModel> models = (List<LlModel>) request.getAttribute("models");
    LlModel defaultModel = (LlModel) request.getAttribute("defaultModel");
    pageContext.setAttribute("defaultModel", defaultModel);
    pageContext.setAttribute("models", models);
%>

<p:main_page title="LLM Management">
    <div class="container">
        <t:admin_navigation activePage="adminLlm"/>

        <div class="card m-2">
            <div class="card-body">
                <h4 class="card-title">Default prompts</h4>
                <t:llm_prompt_modal type="${defaultModel.type.name()}" name="${defaultModel.name}"
                                    attackerPrompt="${defaultModel.getPrompt('ATTACK_DEFAULT').orElse(\"\")}"
                                    attackerDeps="${defaultModel.attackerDependencies}"
                                    attackerDepsPrompt="${defaultModel.getPrompt('ATTACK_DEPENDENCIES').orElse(\"\")}"
                                    resolveEquivalencePrompt="${defaultModel.getPrompt('ATTACK_EQUIVALENCE').orElse(\"\")}"
                                    defenderPrompt="${defaultModel.getPrompt('DEFEND_DEFAULT').orElse(\"\")}"
                                    defenderDeps="${defaultModel.defenderDependencies}"
                                    defenderDepsPrompt="${defaultModel.getPrompt('DEFEND_DEPENDENCIES').orElse(\"\")}"
                                    defenderFocus="${defaultModel.defenderMethodFocus}"
                                    defenderFocusPrompt="${defaultModel.getPrompt('DEFEND_FOCUS').orElse(\"\")}"

                                    htmlId="default-modal"/>
                <div class="d-flex gap-4">
                    <button type="button" class="btn btn-primary" data-bs-toggle="modal"
                            data-bs-target="#default-modal">
                        Edit
                    </button>

                    <form action="${url.forPath("api/llm")}?formType=resetDefault" method="post">
                        <button type="submit" class="btn btn-outline-dark">
                            Reset
                        </button>
                    </form>
                </div>
            </div>
        </div>

        <div class="card m-2">
            <div class="card-body">
                <h4 class="card-title">Available Large Language Models:</h4>
                <table id="models" class="table table-v-align-middle table-striped">
                    <thead>
                    <tr>
                        <th>Provider</th>
                        <th>Name</th>
                        <th>Active</th>
                    </tr>
                    </thead>
                    <tbody id="modelBody">
                    <c:forEach var="model" items="${models}">
                        <c:set var="type" value="${model.type.name()}"/>
                        <c:set var="identifier"
                               value="prompt-modal-${type.replace('.', '-').replace(':', '-')}-${model.name.replace('.', '-').replace(':', '-')}"/>
                        <tr id="model-row-${identifier}">

                            <td id="model-type-${identifier}">${model.type}</td>
                            <td>
                                <a href="#" data-bs-toggle="modal" data-bs-target="#${identifier}"
                                   id="model-name-${identifier}">
                                        ${model.name}
                                </a>
                                <t:llm_prompt_modal type="${type}" name="${model.name}"
                                                    attackerPrompt="${model.getPrompt('ATTACK_DEFAULT').orElse(\"\")}"
                                                    attackerDeps="${model.attackerDependencies}"
                                                    attackerDepsPrompt="${model.getPrompt('ATTACK_DEPENDENCIES').orElse(\"\")}"
                                                    resolveEquivalencePrompt="${model.getPrompt('ATTACK_EQUIVALENCE').orElse(\"\")}"
                                                    defenderPrompt="${model.getPrompt('DEFEND_DEFAULT').orElse(\"\")}"
                                                    defenderDeps="${model.defenderDependencies}"
                                                    defenderDepsPrompt="${model.getPrompt('DEFEND_DEPENDENCIES').orElse(\"\")}"
                                                    defenderFocus="${model.defenderMethodFocus}"
                                                    defenderFocusPrompt="${model.getPrompt('DEFEND_FOCUS').orElse(\"\")}"

                                                    htmlId="${identifier}"/>
                            </td>
                            <td>
                                <label>
                                    <input type="checkbox" class="form-check-input" ${model.active ? "checked" : ""}
                                           id="active-button-${identifier}"/>
                                </label>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>

        <script type="module">
            console.log("MODELS: ${models}");
            const rows = document.querySelectorAll("[id^='model-row-']");
            rows.forEach(r => {
                const name = r.querySelector("[id^='model-name']").textContent.trim();
                const type = r.querySelector("[id^='model-type']").textContent.trim();
                const button = r.querySelector("[id^='active-button-']");

                button.addEventListener("click", () => {
                    fetch("${url.forPath("api/llm")}?formType=setActive", {
                        method: "POST",
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            type: type,
                            name: name,
                            active: button.checked
                        })
                    })
                });
            });
        </script>
    </div>
</p:main_page>
