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
<%@ tag import="java.util.Map" %>
<%@ tag import="org.codedefenders.model.llm.LlmPromptType" %>
<%@ tag import="java.util.HashMap" %>
<%@ tag import="org.codedefenders.model.llm.LlmDefaultStrategies" %>
<%@ tag import="org.codedefenders.persistence.database.LlmRepository" %>
<%@ tag import="org.codedefenders.util.CDIUtil" %>
<%@ tag import="com.google.gson.Gson" %>
<%@ tag import="org.codedefenders.dto.LlmStrategyDTO" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%@ attribute name="htmlId" required="true" %>
<%@ attribute name="strategy" required="true" type="org.codedefenders.model.llm.LlmStrategy" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%
    Map<String, String> namesToContent = new HashMap<>();
    for (LlmPromptType promptType : LlmPromptType.values()) {
        namesToContent.put(promptType.name(), promptType.getDefaultPrompt());
    }
    request.setAttribute("namesToContent", new Gson().toJson(namesToContent));
    request.setAttribute("defaultStrategyNames", LlmDefaultStrategies.values());
    LlmRepository llmRepository = CDIUtil.getBeanFromCDI(LlmRepository.class);
    request.setAttribute("allStrategiesJson", new Gson().toJson(llmRepository.getAllStrategies()));
    request.setAttribute("strategyJson", new Gson().toJson(new LlmStrategyDTO(strategy)));

%>

<form id="${htmlId}-form" action="${url.forPath("/api/llm")}" method="post">
    <input type="hidden" name="formType" value="updatePrompts">
    <input type="hidden" name="baseStrategy" value="${strategy.base.name()}">
    <input type="hidden" name="oldCustomName" value="${strategy.name}">

    <t:modal title="${i18n.tr('Edit strategy')}" id="${htmlId}">
    <jsp:attribute name="content">
        <div class="d-flex flex-row border-bottom border-2 border-secondary p-2 mb-2">
            <label class="form-label flex-fill" for="${htmlId}-new-custom-name">${i18n.tr('Strategy name:')}</label>
            <input class="form-control" id="${htmlId}-new-custom-name" type="text" name="newCustomName"
                   value="${strategy.name}${strategy.readOnly ? "_clone" : ""}" required pattern="[a-zA-Z0-9_\-]+">
        </div>
        <button class="btn btn-primary" id="${htmlId}-add-prompt" type="button">
                ${i18n.tr('Add custom prompt')}
        </button>
        <div id="${htmlId}-content-pane">
            <!--Populated by JS with clones of the template-->
        </div>
    </jsp:attribute>
        <jsp:attribute name="footer">
            <c:if test="${!strategy.readOnly}">
                <button id="${htmlId}-remove-strat-btn" class="btn btn-danger" type="button">
                        ${i18n.tr('Delete this strategy')}
                </button>
                <div class="flex-fill"></div>
            </c:if>
            <button class="btn btn-primary" type="submit">
                    ${i18n.tr('Save custom strategy')}
            </button>
        </jsp:attribute>

    </t:modal>

</form>

<!-- This is the template to be cloned -->
<div id="${htmlId}-template" style="display: none">
    <div class="border rounded bg-light mt-2 mb-2 p-2">
        <div class="pb-2 d-flex justify-content-between align-items-center">
            <label class="form-label" for="${htmlId}-select">${i18n.tr('Prompt type')}:</label>
            <button id="${htmlId}-remove-prompt-btn" class="btn btn-danger">
                <i class="fa fa-trash"></i>
            </button>
        </div>
        <select class="form-select" id="${htmlId}-select">
            <c:forEach items="${strategy.base.relevantPrompts}" var="prompt_type">
                <option value="${prompt_type.name()}">
                        ${prompt_type.displayName()}
                </option>
            </c:forEach>
        </select>
        <div class="pt-2">
            <label class="form-label" for="${htmlId}-area">${i18n.tr('Prompt content:')}</label>
            <textarea class="form-control" rows="15" id="${htmlId}-area" form="${htmlId}-form"></textarea>
        </div>

    </div>
</div>

<script type="application/json" id="${htmlId}-namesToContent">${namesToContent}</script>
<script type="application/json" id="${htmlId}-strategy-json">${strategyJson}</script>
<script type="application/json" id="${htmlId}-all-strategies-json">${strategyJson}</script>

<script type="module">
    const htmlId = "${htmlId}";
    const strategy = JSON.parse(document.getElementById(htmlId + "-strategy-json").textContent);
    const allStrategies = JSON.parse(document.getElementById(htmlId + "-all-strategies-json").textContent);


    const addPromptButton = document.getElementById(htmlId + "-add-prompt");
    const contentPane = document.getElementById(htmlId + "-content-pane");
    let counter = 0;

    const defaultPrompts = JSON.parse(document.getElementById(htmlId + "-namesToContent").textContent)


    function addPromptElement(prompt_type, prompt) {
        counter++;
        const clone = document.getElementById(htmlId + "-template").firstElementChild.cloneNode(true);
        clone.querySelectorAll("[id]").forEach(el => {
            el.id = el.id + "_" + counter;
        });

        clone.querySelectorAll("label[for]").forEach(label => {
            label.htmlFor = label.htmlFor + "_" + counter;
        });
        const select = clone.querySelector("select");
        const textarea = clone.querySelector("textarea");
        if (prompt_type != null && prompt != null) {
            clone.querySelectorAll("option").forEach(o => {
                if (o.value === prompt_type) {
                    o.selected = true;
                }
            })
            textarea.value = prompt
            textarea.name = prompt_type
        } else {
            const options = clone.querySelectorAll("option");
            if (options.length > 0) {
                options[0].selected = true;
                textarea.value = defaultPrompts[options[0].text];
                textarea.name = options[0].text;
            }
        }

        const removeBtn = clone.querySelector("button");
        removeBtn.addEventListener("click", _ => clone.remove())

        select.addEventListener("change", _ => {
            textarea.name = select.value;
            textarea.value = defaultPrompts[select.value];//.replaceAll("\\n", "\n");//.replaceAll("\n", "<b>"));
        });
        document.getElementById(htmlId + "-content-pane").appendChild(clone);
    }

    for (const type in strategy.prompts) {
        addPromptElement(type, strategy.prompts[type]);
    }

    addPromptButton.addEventListener("click", _ => addPromptElement(null, null))

    if (!strategy.readOnly) {
        const deleteStratButton = document.getElementById(htmlId + "-remove-strat-btn");
        deleteStratButton.addEventListener("click", async _ => {
            const params = new URLSearchParams();
            params.append("formType", "deleteCustomStrat");
            params.append("stratName", strategy.name)
            await fetch("${url.forPath("/api/llm")}", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: params.toString()
            });
            location.reload();
        })
    }

    const form = document.getElementById(htmlId + "-form");
    form.addEventListener("submit", (event) => {
        const chosenName = document.getElementById(htmlId + "-new-custom-name").value;
        if (chosenName !== strategy.name) {
            for (let i = 0; i < allStrategies.length; i++) {
                let s = allStrategies[i]
                if (chosenName === s.name) {
                    alert("${i18n.tr('The strategy already exists.')}")
                    event.preventDefault();
                    return;
                }
            }

        }

        const selects = contentPane.querySelectorAll("select");
        const names = [];
        for (let i = 0; i < selects.length; i++) {
            const v = selects[i].value;
            if (names.includes(v)) {
                alert("${i18n.tr('You defined the same prompt twice.')}")
                event.preventDefault();
                break;
            }
            names.push(selects[i].value);
        }
    })


</script>
