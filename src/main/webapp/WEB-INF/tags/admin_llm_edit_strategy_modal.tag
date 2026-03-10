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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ attribute name="htmlId" required="true" %>
<%@ attribute name="strategy" required="false" type="org.codedefenders.model.llm.LlmStrategy" %>

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
    <input type="hidden" name="baseStrategy" value="${strategy.base.name()}">
    <input type="hidden" name="oldCustomName" value="${strategy.name}">

    <t:modal title="Edit strategy" id="${htmlId}">
    <jsp:attribute name="content">
        <div class="d-flex flex-row border-bottom border-2 border-secondary p-2 mb-2">
            <label class="form-label flex-fill" for="${htmlId}-new-custom-name">Strategy name:</label>
            <input class="form-control" id="${htmlId}-new-custom-name" type="text" name="newCustomName" value="${strategy.name}">
        </div>
        <button class="btn btn-primary" id="${htmlId}-add-prompt" type="button">
            Add custom prompt
        </button>
        <div id="${htmlId}-content-pane">
<%--            <c:forEach items="${strategy.customPrompts.keySet()}" var="prompt_type">--%>
<%--                <c:set var="prompt" value="${strategy.getPrompt(prompt_type)}"/>--%>
<%--                <div class="border rounded bg-light mt-2 mb-2 p-2">--%>
<%--                    <label class="form-label" for="${htmlId}-select">Prompt type:</label>--%>
<%--                    <select class="form-select" id="${htmlId}-select">--%>
<%--                        <c:forEach items="${strategy.base.relevantPrompts}" var="possible_prompt_type">--%>
<%--                            <option value="${possible_prompt_type.name()}"--%>
<%--                                ${possible_prompt_type == prompt_type ? "selected=\"selected\"" : ""}>--%>
<%--                                    ${possible_prompt_type.displayName()}--%>
<%--                            </option>--%>
<%--                        </c:forEach>--%>
<%--                    </select>--%>
<%--                    <label class="form-label" for="${htmlId}-area">Prompt content:</label>--%>
<%--                    <textarea class="form-control" id="${htmlId}-area" form="${htmlId}-form" name="${prompt_type}">${prompt}--%>
<%--                    </textarea>--%>

<%--                </div>--%>
<%--            </c:forEach>--%>
        </div>
    </jsp:attribute>
        <jsp:attribute name="footer">
            <button class="btn btn-primary" type="submit">
                Create custom strategy
            </button>
        </jsp:attribute>

    </t:modal>

</form>

<!-- This is the template to be cloned -->
<div id="${htmlId}-template" style="display: none">
    <div class="border rounded bg-light mt-2 mb-2 p-2">
        <label class="form-label" for="${htmlId}-select">Prompt type:</label>
        <select class="form-select" id="${htmlId}-select">
            <c:forEach items="${strategy.base.relevantPrompts}" var="prompt_type">
                <option value="${prompt_type.name()}">
                        ${prompt_type.displayName()}
                </option>
            </c:forEach>
        </select>
        <label class="form-label" for="${htmlId}-area">Prompt content:</label>
        <textarea class="form-control" id="${htmlId}-area" form="${htmlId}-form"></textarea>

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

    //This feel illegal, but there's nothing really wrong about it?
    <c:forEach items="${strategy.customPrompts.keySet()}" var="prompt_type">
        <c:set var="prompt" value="${strategy.getHtmlPrompt(prompt_type)}"/>
    {
        //console.log("${prompt_type}");
        counter++;
        const clone = document.getElementById("${htmlId}-template").firstElementChild.cloneNode(true);
        clone.querySelectorAll("[id]").forEach(el => {
            el.id = el.id + "_" + counter;
        });

        clone.querySelectorAll("label[for]").forEach(label => {
            label.htmlFor = label.htmlFor + "_" + counter;
        });
        const select = clone.querySelector("select");
        const textarea = clone.querySelector("textarea");
        clone.querySelectorAll("option").forEach(o => {
            if (o.value === "${prompt_type.name()}") {
                console.log("Found match: " + o.value);
                console.log(o);
                o.selected = true;
            }
        })
        textarea.textContent = "${prompt}"
        textarea.name = "${prompt_type.name()}"
        //textarea.textContent = namePromptMap.get(select
          //      .value.replaceAll("\n", "<b>"));

        select.addEventListener("change", e => {
            textarea.name = select.value;
            textarea.textContent = namePromptMap.get(select
                    .value.replaceAll("\n", "<b>"));
        });
        document.getElementById("${htmlId}-content-pane").appendChild(clone);
    }

    </c:forEach>

    addPromptButton.addEventListener("click", evt => {
        counter++;
        const clone = document.getElementById("${htmlId}-template").firstElementChild.cloneNode(true);
        clone.querySelectorAll("[id]").forEach(el => {
            el.id = el.id + "_" + counter;
        });

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
