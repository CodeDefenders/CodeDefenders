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

<%@ tag import="org.codedefenders.util.Paths" %>

<%--@elvariable id="mutantAccordion" type="org.codedefenders.beans.game.MutantAccordionBean"--%>


<div id="mutants-div">

    <style>
        <%-- Prefix all classes with "ta-" to avoid conflicts.
        We probably want to extract some common CSS when we finally tackle the CSS issue. --%>

        /* Customization of Bootstrap 5 accordion style.
        ----------------------------------------------------------------------------- */

        #mutants-accordion .accordion-button {
            padding: .6rem .8rem;
            background-color: rgba(0, 0, 0, .03);
        }

        /* Clear the box shadow from .accordion-button. This removes the blue outline when selecting a button, and the
           border between the header and content of accordion items when expanded. */
        #mutants-accordion .accordion-button {
            box-shadow: none;
        }

        /* Add back the border between header and content of accordion items. */
        #mutants-accordion .accordion-body {
            border-top: 1px solid rgba(0, 0, 0, .125);
        }

        /* Always display the chevron icon in black. */
        #mutants-accordion .accordion-button:not(.collapsed)::after {
            /* Copied from Bootstrap 5. */
            background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='%23212529'%3e%3cpath fill-rule='evenodd' d='M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z'/%3e%3c/svg%3e");
        }

        /* Categories.
        ----------------------------------------------------------------------------- */

        #mutants-accordion .accordion-button:not(.ma-covered) {
            color: #B0B0B0;
        }

        #mutants-accordion .accordion-button.ma-covered {
            color: black;
        }

        /* Tables.
        ----------------------------------------------------------------------------- */

        #mutants-accordion thead {
            display: none;
        }

        #mutants-accordion .dataTables_scrollHead {
            display: none;
        }

        #mutants-accordion td {
            vertical-align: middle;
        }

        #mutants-accordion table {
            font-size: inherit;
        }

        #mutants-accordion tr:last-child > td {
            border-bottom: none;
        }

        /* Inline elements.
        ----------------------------------------------------------------------------- */

        #mutants-accordion .ma-column-name {
            color: #B0B0B0;
        }

        #mutants-accordion .ma-mutant-link {
            cursor: default;
        }
    </style>


    <div class="game-component-header">
        <h3>Existing Mutants</h3>
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

    <div class="accordion" id="mutants-accordion">
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

    <script type="text/javascript" src="js/modal.js"></script>
    <script type="text/javascript" src="js/mutant_accordion.js"></script>

    <script>
        /* Wrap in a function to avoid polluting the global scope. */
        (function () {
            const categories = JSON.parse('${mutantAccordion.jsonFromCategories()}');
            const mutants = new Map(JSON.parse('${mutantAccordion.jsonMutants()}'));
            const flaggingUrl = '${pageContext.request.contextPath}${Paths.EQUIVALENCE_DUELS_GAME}';
            const gameId = ${mutantAccordion.gameId};

            CodeDefenders.objects.mutantAccordion = new CodeDefenders.classes.MutantAccordion(
                    categories,
                    mutants,
                    flaggingUrl,
                    gameId);
        })();
    </script>
</div>
