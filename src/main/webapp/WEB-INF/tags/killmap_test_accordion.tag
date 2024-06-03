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

<link href="${url.forPath("/css/specific/killmap_test_accordion.css")}" rel="stylesheet">

<div id="mutants-div">

    <div class="game-component-header">
        <h2>Kill-maps by Tests</h2>
        <div id="kta-filter">
            <input type="radio" class="btn-check" name="kta-filter" id="kt-all" value="ALL" checked>
            <label class="btn btn-xs btn-outline-secondary" for="kt-all">
                <span class="align-middle">All</span>
            </label>
            <input type="radio" class="btn-check" name="kta-filter" id="kt-no-kill" value="NO_KILL">
            <label class="btn btn-xs btn-outline-secondary" for="kt-no-kill">
                <span class="killMapImage killMapImageNoKill align-middle"></span>
                <span class="align-middle">No Kill</span>
            </label>
            <input type="radio" class="btn-check" name="kta-filter" id="kt-kill" value="KILL">
            <label class="btn btn-xs btn-outline-secondary" for="kt-kill">
                <span class="killMapImage killMapImageKill align-middle"></span>
                <span class="align-middle">Kill</span>
            </label>
            <input type="radio" class="btn-check" name="kta-filter" id="kt-error" value="ERROR">
            <label class="btn btn-xs btn-outline-secondary" for="kt-error">
                <span class="killMapImage killMapImageError align-middle"></span>
                <span class="align-middle">Error</span>
            </label>
            <input type="radio" class="btn-check" name="kta-filter" id="kt-no-cov" value="NO_COVERAGE">
            <label class="btn btn-xs btn-outline-secondary" for="kt-no-cov">
                <span class="killMapImage killMapImageNoCoverage align-middle"></span>
                <span class="align-middle">No Coverage</span>
            </label>
            <input type="radio" class="btn-check" name="kta-filter" id="kt-unknown" value="UNKNOWN">
            <label class="btn btn-xs btn-outline-secondary" for="kt-unknown">
                <span class="killMapImage killMapImageUnknown align-middle"></span>
                <span class="align-middle">Unknown</span>
            </label>
        </div>
    </div>

    <div class="accordion loading loading-border-card loading-bg-gray" id="kill-map-test-accordion">
        <c:forEach items="${killMapAccordion.categoriesForTests}" var="category">
            <div class="accordion-item">
                <h2 class="accordion-header" id="kta-heading-${category.id}">
                    <button class="${category.testIds.size() == 0 ? "" : 'kta-covered'} accordion-button collapsed"
                            type="button" data-bs-toggle="collapse"
                            data-bs-target="#kta-collapse-${category.id}"
                            aria-controls="kta-collapse-${category.id}">
                        <span class="badge bg-defender me-2 kta-count"
                              id="kta-count-${category.id}"
                              <c:if test="${category.testIds.size() == 0}">hidden</c:if>>
                                ${category.testIds.size()}
                        </span>
                            ${category.description}
                    </button>
                </h2>
                <div class="accordion-collapse collapse"
                     id="kta-collapse-${category.id}"
                     data-bs-parent="#kill-map-test-accordion"
                     aria-expanded="false" aria-labelledby="kta-heading-${category.id}">
                    <div class="accordion-body p-0">
                        <div class="accordion ${category.testIds.size() == 0 ? 'empty' : ''}"
                             id="test-kill-map-accordion-${category.id}">
                            <c:if test="${category.testIds.size() == 0}">
                                <div class="accordion-item">
                                    <div class="accordion-header">
                                        <p class="text-center m-0 py-1">No tests in this category.</p>
                                    </div>
                                </div>
                            </c:if>
                            <c:forEach items="${killMapAccordion.getTestsByCategory(category)}" var="test">
                                <div class="accordion-item">
                                    <h3 class="accordion-header"
                                        id="kta-heading-category-${category.id}-test-${test.id}">
                                        <button class="${category.testIds.size() == 0 ? "" : 'kta-covered'}
                                                        accordion-button collapsed"
                                                type="button" role="button"
                                                aria-controls="kta-collapse-category-${category.id}-test-${test.id}">
                                            <span class="kta-button-content">
                                                <span class="kta-test-link">
                                                    Test ${test.id}
                                                </span>
                                                <span class="kta-col">
                                                    <span class="kta-column-name mx-2">by</span>
                                                    ${test.creator.name}
                                                </span>
                                                <span class="kta-col">
                                                    <span class="kta-column-name mx-2">Covered:</span>
                                                    ${test.coveredMutantIds.size()}
                                                </span>
                                                <span class="kta-col">
                                                    <span class="kta-column-name mx-2">Killed:</span>
                                                    ${test.killedMutantIds.size()}
                                                </span>
                                                <span class="kta-col">
                                                    <span class="kta-column-name mx-2">Points:</span>
                                                    ${test.points}
                                                </span>
                                                <span class="kta-col ms-auto me-3 text-end">
                                                    <span class="kta-view-test-button btn btn-xs btn-primary">
                                                        View
                                                    </span>
                                                </span>
                                            </span>
                                        </button>
                                    </h3>
                                    <div class="accordion-collapse collapse"
                                         id="kta-collapse-category-${category.id}-test-${test.id}"
                                         data-bs-parent="#test-kill-map-accordion-${category.id}"
                                         aria-expanded="false"
                                         aria-labelledby="kta-heading-category-${category.id}-test-${test.id}">
                                        <div class="accordion-body p-0">
                                            <table id="kta-table-category-${category.id}-test-${test.id}"
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
        import {KillMapTestAccordion} from '${url.forPath("/js/codedefenders_game.mjs")}';

        const categories = JSON.parse('${killMapAccordion.categoriesForTestsAsJSON}');
        const mutants = new Map(JSON.parse('${killMapAccordion.mutantsAsJSON}'));
        const tests = new Map(JSON.parse('${killMapAccordion.testsAsJSON}'));
        // Only lookup, but no iteration is done on the kill-map, so the killMapForMutants is sufficient:
        const killMap = JSON.parse('${killMapAccordion.killMapForMutantsAsJSON}');
        const gameId = ${killMapAccordion.gameId};
        const killMapTestAccordion = new KillMapTestAccordion(categories, mutants, tests, killMap, gameId);

        objects.register('killMapTestAccordion', killMapTestAccordion);
    </script>
</div>
