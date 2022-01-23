<%--

    Copyright (C) 2016-2019 Code Defenders contributors

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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="testAccordion" type="org.codedefenders.beans.game.TestAccordionBean"--%>

<%--
    Displays an accordion of tables of tests, grouped by which of the CUT's methods they cover.

    The accordion is generated by the JSP, the tables in the accordion sections as well as popovers and models are
    generated through JavaScript.
--%>

<link href="${pageContext.request.contextPath}/css/specific/test_accordion.css" rel="stylesheet">

<div class="accordion" id="tests-accordion">
    <c:forEach items="${testAccordion.categories}" var="category">
        <div class="accordion-item">
            <h2 class="accordion-header" id="ta-heading-${category.id}">
                <%-- ${empty …} doesn't work with Set --%>
                <button class="${category.testIds.size() == 0 ? "" : "ta-covered"} accordion-button collapsed"
                        type="button" data-bs-toggle="collapse"
                        data-bs-target="#ta-collapse-${category.id}"
                        aria-controls="ta-collapse-${category.id}">
                    <%-- ${empty …} doesn't work with Set --%>
                    <c:if test="${!(category.testIds.size() == 0)}">
                        <span class="badge bg-defender me-2 ta-count">${category.testIds.size()}</span>
                    </c:if>
                    ${category.description}
                </button>
            </h2>
            <div class="accordion-collapse collapse"
                 id="ta-collapse-${category.id}"
                 data-bs-parent="#tests-accordion"
                 aria-expanded="false" aria-labelledby="ta-heading-${category.id}">
                <div class="accordion-body p-0">
                    <table id="ta-table-${category.id}" class="table table-sm"></table>
                </div>
            </div>
        </div>
    </c:forEach>
</div>

<script type="text/javascript" src="js/modal.js"></script>
<script type="text/javascript" src="js/test_accordion.js"></script>

<script>
    /* Wrap in a function to avoid polluting the global scope. */
    (function () {
        const categories = JSON.parse('${testAccordion.categoriesAsJSON}');
        const tests = new Map(JSON.parse('${testAccordion.testsAsJSON}'));

        CodeDefenders.objects.testAccordion = new CodeDefenders.classes.TestAccordion(categories, tests);
    })();
</script>
