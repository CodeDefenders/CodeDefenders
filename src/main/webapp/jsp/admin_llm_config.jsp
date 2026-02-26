<%@ page import="org.codedefenders.model.llm.LlModel" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.model.llm.LlmStrategy" %><%--

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
    @SuppressWarnings("unchecked")
    List<LlmStrategy> attackStrategies = (List<LlmStrategy>) request.getAttribute("attackStrategies");
    @SuppressWarnings("unchecked")
    List<LlmStrategy> defendStrategies = (List<LlmStrategy>) request.getAttribute("defendStrategies");
    @SuppressWarnings("unchecked")
    List<LlmStrategy> equivalenceStrategies = (List<LlmStrategy>) request.getAttribute("equivalenceStrategies");
    pageContext.setAttribute("models", models);
    pageContext.setAttribute("attackStrategies", attackStrategies);
    pageContext.setAttribute("defendStrategies", defendStrategies);
    pageContext.setAttribute("equivalenceStrategies", equivalenceStrategies);
%>

<p:main_page title="LLM Management">
    <div class="container">
        <t:admin_navigation activePage="adminLlmConfig"/>

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
                            <td id="model-name-${identifier}">
                                    ${model.name}
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

        <t:admin_llm_strat_table strategies="${attackStrategies}" title="Attack strategies" htmlId="attackTable"/>
        <t:admin_llm_strat_table strategies="${defendStrategies}" title="Defend strategies" htmlId="defendTable"/>
        <t:admin_llm_strat_table strategies="${equivalenceStrategies}" title="Equivalence strategies" htmlId="equivTable"/>

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
