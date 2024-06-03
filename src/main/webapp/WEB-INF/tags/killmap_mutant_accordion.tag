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
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="killMapAccordion" type="org.codedefenders.beans.game.KillMapAccordionBean"--%>

<link href="${url.forPath("/css/specific/killmap_mutant_accordion.css")}" rel="stylesheet">

<div id="mutants-div">

    <div class="game-component-header">
        <h2>Kill-maps by Mutants</h2>
        <div id="kma-filter">
            <input type="radio" class="btn-check" name="kma-filter" id="km-all" value="ALL" checked>
            <label class="btn btn-xs btn-outline-secondary" for="km-all">
                <span class="align-middle">All</span>
            </label>
            <input type="radio" class="btn-check" name="kma-filter" id="km-no-kill" value="NO_KILL">
            <label class="btn btn-xs btn-outline-secondary" for="km-no-kill">
                <span class="killMapImage killMapImageNoKill align-middle"></span>
                <span class="align-middle">No Kill</span>
            </label>
            <input type="radio" class="btn-check" name="kma-filter" id="km-kill" value="KILL">
            <label class="btn btn-xs btn-outline-secondary" for="km-kill">
                <span class="killMapImage killMapImageKill align-middle"></span>
                <span class="align-middle">Kill</span>
            </label>
            <input type="radio" class="btn-check" name="kma-filter" id="km-error" value="ERROR">
            <label class="btn btn-xs btn-outline-secondary" for="km-error">
                <span class="killMapImage killMapImageError align-middle"></span>
                <span class="align-middle">Error</span>
            </label>
            <input type="radio" class="btn-check" name="kma-filter" id="km-no-cov" value="NO_COVERAGE">
            <label class="btn btn-xs btn-outline-secondary" for="km-no-cov">
                <span class="killMapImage killMapImageNoCoverage align-middle"></span>
                <span class="align-middle">No Coverage</span>
            </label>
            <input type="radio" class="btn-check" name="kma-filter" id="km-unknown" value="UNKNOWN">
            <label class="btn btn-xs btn-outline-secondary" for="km-unknown">
                <span class="killMapImage killMapImageUnknown align-middle"></span>
                <span class="align-middle">Unknown</span>
            </label>
        </div>
    </div>

    <div class="accordion loading loading-border-card loading-bg-gray" id="kill-map-mutant-accordion">
        <c:forEach items="${killMapAccordion.categories}" var="category">
            <div class="accordion-item">
                <h2 class="accordion-header" id="kma-heading-${category.id}">
                        <%-- ${empty …} doesn't work with Set --%>
                    <button class="${category.mutantIds.size() == 0 ? "" : 'kma-covered'} accordion-button collapsed"
                            type="button" data-bs-toggle="collapse"
                            data-bs-target="#kma-collapse-${category.id}"
                            aria-controls="kma-collapse-${category.id}">
                            <%-- ${empty …} doesn't work with Set --%>
                        <span class="badge bg-attacker me-2 kma-count"
                              id="kma-count-${category.id}"
                              <c:if test="${category.mutantIds.size() == 0}">hidden</c:if>>
                                ${category.mutantIds.size()}
                        </span>
                            ${category.description}
                    </button>
                </h2>
                <div class="accordion-collapse collapse"
                     id="kma-collapse-${category.id}"
                     data-bs-parent="#kill-map-mutant-accordion"
                     aria-expanded="false" aria-labelledby="kma-heading-${category.id}">
                    <div class="accordion-body p-0">
                        <div class="accordion ${category.mutantIds.size() == 0 ? 'empty' : ''}"
                             id="mutant-kill-map-accordion-${category.id}">
                            <c:if test="${category.mutantIds.size() == 0}">
                                <div class="accordion-item">
                                    <div class="accordion-header">
                                        <p class="text-center m-0 py-1">No mutants in this category.</p>
                                    </div>
                                </div>
                            </c:if>
                            <c:forEach items="${killMapAccordion.getMutantsByCategory(category)}" var="mutant">
                                <div class="accordion-item">
                                    <h3 class="accordion-header"
                                        id="kma-heading-category-${category.id}-mutant-${mutant.id}">
                                        <button class="${category.testIds.size() == 0 ? "" : 'kma-covered'}
                                                        accordion-button collapsed"
                                                type="button" role="button"
                                                aria-controls="kma-collapse-category-${category.id}-mutant-${mutant.id}">
                                            <span class="kma-button-content">
                                                <c:set var="state" value="${mutant.state}"/>
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
                                                <span class="kma-mutant-link">
                                                    Mutant ${mutant.id}
                                                </span>
                                                <span class="kma-col">
                                                    <span class="kma-column-name mx-2">by</span>
                                                    ${mutant.creator.name}
                                                </span>
                                                <span class="kma-col">
                                                    ${mutant.description}
                                                </span>
                                                <span class="kma-col">
                                                    <span class="kma-column-name mx-2">Points:</span>
                                                    ${mutant.points}
                                                </span>
                                                <span class="kta-col ms-auto me-3 text-end">
                                                    <span class="kta-view-mutant-button btn btn-xs btn-primary">
                                                        View
                                                    </span>
                                                    <c:if test="${state == 'KILLED'}">
                                                        <span class="kta-view-killing-test-button btn btn-xs btn-secondary ms-2">
                                                            View Killing Test
                                                        </span>
                                                    </c:if>
                                                </span>
                                            </span>
                                        </button>
                                    </h3>
                                    <div class="accordion-collapse collapse"
                                         id="kma-collapse-category-${category.id}-mutant-${mutant.id}"
                                         data-bs-parent="#mutant-kill-map-accordion-${category.id}"
                                         aria-expanded="false"
                                         aria-labelledby="kma-heading-category-${category.id}-mutant-${mutant.id}">
                                        <div class="accordion-body p-0">
                                            <table id="kma-table-category-${category.id}-mutant-${mutant.id}"
                                                   class="table table-sm"></table>
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
