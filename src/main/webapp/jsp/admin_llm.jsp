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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="settingsRepository" type="org.codedefenders.persistence.database.SettingsRepository"--%>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>


<p:main_page title="LLM Management">
    <div class="container">
        <t:admin_navigation activePage="adminLlm"/>

        <h2>Active LLM Defenders:</h2>
        <table id="models" class="table table-v-align-middle table-striped">
            <thead>
            <tr>
                <th>Provider</th>
                <th>Name</th>
                <th>Active</th>
            </tr>
            </thead>
            <tbody id="modelBody">
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
            const allModels = await fetchJSON("${url.forPath("api/llm")}?action=getall");
            const modelBody = document.getElementById("modelBody");
            allModels.forEach((model, index) => {
                const row = document.createElement("tr");
                dataToRow(row, model.type);
                dataToRow(row, model.name);
                //dataToRow(form, model.active);

                const activeTd = document.createElement("td");
                const activeButton = document.createElement("input");
                activeButton.setAttribute("type", "checkbox");
                activeButton.classList.add("form-check-input");
                activeButton.checked = model.active;
                activeButton.addEventListener("click", x => {
                    console.log("checked: " +activeButton.checked);
                    fetch("${url.forPath("api/llm")}", {
                        method: "POST",
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            action: "setActive",
                            type: model.type,
                            name: model.name,
                            active: activeButton.checked
                        })
                    })
                });

                activeTd.appendChild(activeButton);
                row.appendChild(activeTd);
                modelBody.appendChild(row);
            })

            function dataToRow(row, data) {
                const td = document.createElement("td");
                td.textContent = data;
                row.appendChild(td);
            }

            /**
             * Fetches an object from a given JSON API.
             * @async
             * @param {string} url The URL to fetch from.
             * @returns {Promise<object>} A promise containing the response.
             */
            async function fetchJSON(url) {
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                if (!response.ok) {
                    return Promise.reject();
                }
                return await response.json();
            }
        </script>
    </div>
</p:main_page>
