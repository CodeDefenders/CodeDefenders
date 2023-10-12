<%--
  ~ Copyright (C) 2020 Code Defenders contributors
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
<%--@elvariable id="mutantAccordion" type="org.codedefenders.beans.game.MutantAccordionBean"--%>

<link href="${url.forPath("/css/specific/mutant_accordion.css")}" rel="stylesheet">

<div id="mutants-div">

    <div class="game-component-header">
        <h3>Existing Mutants</h3>
        <div id="ma-filter">
            <input type="radio" class="btn-check" name="filter" id="all" value="ALL" checked autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="all">
                <span class="align-middle">All</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="alive" value="ALIVE" autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="alive">
                <span class="mutantCUTImage mutantImageAlive align-middle"></span>
                <span class="align-middle">Alive</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="killed" value="KILLED" autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="killed">
                <span class="mutantCUTImage mutantImageKilled align-middle"></span>
                <span class="align-middle">Killed</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="marked" value="FLAGGED" autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="marked">
                <span class="mutantCUTImage mutantImageFlagged align-middle"></span>
                <span class="align-middle">Claimed Equivalent</span>
            </label>
            <input type="radio" class="btn-check" name="filter" id="equivalent" value="EQUIVALENT" autocomplete="off">
            <label class="btn btn-xs btn-outline-secondary" for="equivalent">
                <span class="mutantCUTImage mutantImageEquiv align-middle"></span>
                <span class="align-middle">Equivalent</span>
            </label>
        </div>
    </div>

    <div class="accordion loading loading-border-card loading-bg-gray" id="mutants-accordion">
        <c:forEach items="${mutantAccordion.categories}" var="category">
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
                     data-bs-parent="#mutants-accordion"
                     aria-expanded="false" aria-labelledby="ma-heading-${category.id}">
                    <div class="accordion-body p-0">
                        <table id="ma-table-${category.id}" class="table table-sm"></table>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>

    <script type="module">
        import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
        import {MutantAccordion} from '${url.forPath("/js/codedefenders_game.mjs")}';


        const categories = JSON.parse('${mutantAccordion.jsonFromCategories()}');
        const mutants = new Map(JSON.parse('${mutantAccordion.jsonMutants()}'));
        const gameId = ${mutantAccordion.gameId};

        const mutantAccordion = new MutantAccordion(
                categories,
                mutants,
                gameId);


        objects.register('mutantAccordion', mutantAccordion);
    </script>
</div>
