<%--
  ~ Copyright (C) 2023 Code Defenders contributors
  ~
  ~ This file is part of Code Defenders.
  ~
  ~ Code Defenders is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or (at
  ~ your option) any later version.
  ~
  ~ Code Defenders is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
  --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="killMapAccordion" type="org.codedefenders.beans.game.KillMapAccordionBean"--%>

<link href="${url.forPath("/css/specific/killmap_mutant_accordion.css")}" rel="stylesheet">

<div id="mutants-div">

    <div class="game-component-header">
        <h3>Mutants with kill-maps</h3>
        <div id="ma-filter">
            <input type="radio" class="btn-check" name="filter" id="all" value="ALL" checked>
            <label class="btn btn-xs btn-outline-secondary" for="all">
                <span class="align-middle">All</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="alive" value="ALIVE">
            <label class="btn btn-xs btn-outline-secondary" for="alive">
                <span class="mutantCUTImage mutantImageAlive align-middle"></span>
                <span class="align-middle">Alive</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="killed" value="KILLED">
            <label class="btn btn-xs btn-outline-secondary" for="killed">
                <span class="mutantCUTImage mutantImageKilled align-middle"></span>
                <span class="align-middle">Killed</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="marked" value="FLAGGED">
            <label class="btn btn-xs btn-outline-secondary" for="marked">
                <span class="mutantCUTImage mutantImageFlagged align-middle"></span>
                <span class="align-middle">Claimed Equivalent</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="equivalent" value="EQUIVALENT">
            <label class="btn btn-xs btn-outline-secondary" for="equivalent">
                <span class="mutantCUTImage mutantImageEquiv align-middle"></span>
                <span class="align-middle">Equivalent</span>
            </label>
        </div>
    </div>

    <div class="accordion" id="mutant-categories-accordion">
        <c:forEach items="${killMapAccordion.categories}" var="category">
            <div class="accordion-item">
                <h2 class="accordion-header" id="ma-heading-${category.id}">
                        <%-- ${empty …} doesn't work with Set --%>
                    <button class="${category.mutantIds.size() == 0 ? "" : 'ma-covered'} accordion-button collapsed"
                            type="button" data-bs-toggle="collapse"
                            data-bs-target="#ma-collapse-${category.id}"
                            aria-controls="ma-collapse-${category.id}">
                            <%-- ${empty …} doesn't work with Set --%>
                        <span class="badge bg-attacker me-2 ma-count"
                              id="ma-count-${category.id}"
                              <c:if test="${category.mutantIds.size() == 0}">hidden</c:if>>
                                ${category.mutantIds.size()}
                        </span>
                            ${category.description}
                    </button>
                </h2>
                <div class="accordion-collapse collapse"
                     id="ma-collapse-${category.id}"
                     data-bs-parent="#mutant-categories-accordion"
                     aria-expanded="false" aria-labelledby="ma-heading-${category.id}">
                    <div class="accordion-body p-0">
                        <div class="accordion" id="mutant-killmap-accordion">
                            <c:if test="${category.mutantIds.size() == 0}">
                                <div class="accordion-item">
                                    <div class="accordion-header">
                                        <p class="text-center m-0">No mutants in this category.</p>
                                    </div>
                                </div>

                            </c:if>
                            <c:forEach items="${killMapAccordion.getMutantsByCategory(category)}" var="mutant">
                                <div class="accordion-item">
                                    <h3 class="accordion-header" id="ma-heading-mutant-${mutant.id}">
                                        <button class="${category.testIds.size() == 0 ? "" : 'ma-covered'}
                                                        accordion-button collapsed"
                                                type="button" data-bs-toggle="collapse"
                                                data-bs-target="#ma-collapse-mutant-${mutant.id}"
                                                aria-controls="ma-collapse-mutant-${mutant.id}">
                                            <c:set var="state"
                                                   value="${mutant.state}"/>
                                            <c:choose>
                                                <c:when test="${state == 'ALIVE'}">
                                                    <span class="mutantCUTImage mutantImageAlive"></span>
                                                </c:when>
                                                <c:when test="${state == 'KILLED'}">
                                                    <span class="mutantCUTImage mutantImageKilled"></span>
                                                </c:when>
                                                <c:when test="${state == 'EQUIVALENT'}">
                                                    <span class="mutantCUTImage mutantImageEquiv"></span>
                                                </c:when>
                                                <c:when test="${state == 'FLAGGED'}">
                                                    <span class="mutantCUTImage mutantImageFlagged"></span>
                                                </c:when>
                                            </c:choose>
                                            <span class="ma-mutant-link">
                                                Mutant ${mutant.id}
                                            </span>
                                            <span class="ma-column-name mx-2">by</span>
                                                ${mutant.creator.name}
                                        </button>
                                    </h3>
                                    <div class="accordion-collapse collapse"
                                         id="ma-collapse-mutant-${mutant.id}"
                                         data-bs-parent="#mutant-killmap-accordion"
                                         aria-expanded="false" aria-labelledby="ma-heading-mutant-${mutant.id}">
                                        <div class="accordion-body p-0">
                                            <table id="ma-table-mutant-${mutant.id}" class="table table-sm"></table>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>

    <script type="module">
        import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
        import {KillMapMutantAccordion} from '${url.forPath("/js/codedefenders_game.mjs")}';

        const categories = JSON.parse('${killMapAccordion.categoriesAsJSON}');
        const mutants = new Map(JSON.parse('${killMapAccordion.mutantsAsJSON}'));
        const tests = new Map(JSON.parse('${killMapAccordion.testsAsJSON}'));
        const killMap = JSON.parse('${killMapAccordion.killMapForMutantsAsJSON}');
        const gameId = ${killMapAccordion.gameId};
        const killMapMutantAccordion = new KillMapMutantAccordion(categories, mutants, tests, killMap, gameId);

        objects.register('killMapMutantAccordion', killMapMutantAccordion);
    </script>
</div>
