<%@ tag import="java.util.Map" %>
<%@ tag import="org.codedefenders.model.llm.LlmPromptType" %>
<%@ tag import="java.util.Arrays" %>
<%@ tag import="java.util.HashMap" %><%--

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

<%@ attribute name="htmlId" required="true" %>
<%@ attribute name="baseStrategy" required="true" type="org.codedefenders.model.llm.LlmDefaultStrategy" %>
<%@ attribute name="oldCustomName" required="false" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%
    Map<String, String> namesToContent = new HashMap<>();
    for (LlmPromptType promptType : LlmPromptType.values()) {
        namesToContent.put(promptType.name(), promptType.getDefaultPrompt());
    }
    //pageContext.setAttribute("namesToContent", namesToContent);
%>

<form id="${htmlId}-form" action="${url.forPath("/api/llm")}" method="post">
    <input type="hidden" name="formType" value="updatePrompts">
    <input type="hidden" name="baseStrategy" value="${baseStrategy.name()}">
    <c:if test="${oldCustomName}">
        <input type="hidden" name="oldCustomName" value="${oldCustomName}">
    </c:if>

    <t:modal title="Edit strategy" id="${htmlId}">
    <jsp:attribute name="content">
        <label for="${htmlId}-new-custom-name"></label>
        <input id="${htmlId}-new-custom-name" type="text" name="newCustomName">
        <button id="${htmlId}-add-prompt" type="button">
            Add custom prompt
        </button>
        <div id="${htmlId}-content-pane">

        </div>
    </jsp:attribute>
        <jsp:attribute name="footer">
            <button type="submit">
                Create custom strategy
            </button>
        </jsp:attribute>

    </t:modal>

</form>

<div id="${htmlId}-template" style="display: none">
    <div><!-- TODO style -->
        <label for="${htmlId}-select">Prompt type:</label>
        <select id="${htmlId}-select">
            <c:forEach items="${baseStrategy.relevantPrompts}" var="prompt">
                <option value="${prompt.name()}">
                        ${prompt.displayName()}
                </option>
            </c:forEach>
        </select>
        <label for="${htmlId}-area">Prompt content:</label>
        <textarea id="${htmlId}-area" form="${htmlId}-form"></textarea>

    </div>
</div>

<script type="module">

    const addPromptButton = document.getElementById("${htmlId}-add-prompt");
    const contentPane = document.getElementById("${htmlId}-content-pane");
    let counter = 0;

    const namePromptMap = new Map([//TODO This is horrible
        <% for (Map.Entry<String, String> entry : namesToContent.entrySet()) { %>
        ["<%= entry.getKey() %>", "<%= entry.getValue()
        .replace("\r\n", "\\n")
        .replace("\r", "\\n")
         .replace("\n", "\\n")
         .replace("\"", "\\\"")
         .replace("#", "\\#")%>"],
        <% } %>]);


    addPromptButton.addEventListener("click", evt => {
        counter++;
        const clone = document.getElementById("${htmlId}-template").firstElementChild.cloneNode(true);
        clone.querySelectorAll("[id]").forEach(el => {
            el.id = el.id + "_" + counter;
        });

        // Optional: auch label[for] anpassen
        clone.querySelectorAll("label[for]").forEach(label => {
            label.htmlFor = label.htmlFor + "_" + counter;
        });

        clone.querySelector("select").addEventListener("change", e => {
            const select = clone.querySelector("select");
            const textarea = clone.querySelector("textarea")
            textarea.name = select.value;

            clone.querySelector("textarea").textContent = namePromptMap.get(select
                    .value.replaceAll("\n", "<b>"));
        });
        document.getElementById("${htmlId}-content-pane").appendChild(clone);
    })

</script>
