<%@ page import="org.codedefenders.model.LLModel" %>
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
    List<LLModel> models = (List<LLModel>) request.getAttribute("models");
    pageContext.setAttribute("models", models);
%>

<p:main_page title="LLM Management">
    <div class="container">
        <t:admin_navigation activePage="adminLlm"/>

        <h2>Available Large Language Models:</h2>
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
                        <a href="#" data-bs-toggle="modal" data-bs-target="#${identifier}" id="model-name-${identifier}">
                                ${model.name}
                        </a>
                        <t:llm_prompt_modal type="${type}" name="${model.name}"
                                            attackerPrompt="${model.attackerPrompt.orElse(\"\")}"
                                            attackerDeps="${model.attackerDependencies}"
                                            attackerDepsPrompt="${model.attackerDependencyPrompt.orElse(\"\")}"
                                            attackerFocus="${model.attackerMethodFocus}"
                                            attackerFocusPrompt="${model.attackerMethodFocusPrompt.orElse(\"\")}"
                                            defenderPrompt="${model.defenderPrompt.orElse(\"\")}"
                                            defenderDeps="${model.defenderDependencies}"
                                            defenderDepsPrompt="${model.defenderDependencyPrompt.orElse(\"\")}"
                                            defenderFocus="${model.defenderMethodFocus}"
                                            defenderFocusPrompt="${model.defenderMethodFocusPrompt.orElse(\"\")}"

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

        <h2>Active LLM Defenders:</h2>
        <table id="defenderTable" class="table table-v-align-middle table-striped">
            <thead>
            <tr>
                <th>GameID</th>
                <th>Class</th>
                <th>Creator</th>
                <th>#Players</th>
                <th>Model</th>
                <th>Control</th>
            </tr>
            </thead>
            <tbody id="defenderBody">
            <tr>
                <td>123</td>
                <td>Helloword</td>
                <td>Peter</td>
                <td>5</td>
                <td>openai:gpt4.0</td>
                <td>2 buttons</td>
            </tr>
            </tbody>
        </table>

        <script type="module">

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
