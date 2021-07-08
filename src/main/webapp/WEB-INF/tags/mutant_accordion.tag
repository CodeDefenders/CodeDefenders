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

<style>
    <%-- Prefix all classes with "ta-" to avoid conflicts.
    We probably want to extract some common CSS when we finally tackle the CSS issue. --%>

    /* Customization of Bootstrap 5 accordion style.
    ----------------------------------------------------------------------------- */

    #mutants-accordion .accordion-button {
        padding: .6rem .8rem;
        background-color: rgba(0,0,0,.03);
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
                    <c:if test="${!(category.mutantIds.size() == 0)}">
                        <span class="badge bg-attacker me-2 ma-count">${category.mutantIds.size()}</span>
                    </c:if>
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

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {

        /** A description and list of mutant ids for each category (method). */
        const categories = JSON.parse('${mutantAccordion.jsonFromCategories()}');

        /** Maps mutant ids to their DTO representation. */
        const mutants = new Map(JSON.parse('${mutantAccordion.jsonMutants()}'));

        /** Maps mutant ids to modals that show the tests' code. */
        const mutantModals = new Map();
        const testModals = new Map();

        /* Functions to generate table columns. */
        const genId = row => `<span class="ma-mutant-link">Mutant \${row.id}</span>
                <span class="ma-column-name mx-2">by</span>\${row.creator.name}
                \${row.killedByName ? '<span class="ma-column-name mx-2">killed by</span>' + row.killedByName : ''}`;
        const genPoints = row => `<span class="ma-column-name">Points:</span> \${row.points}`;
        const genLines = row => row.description;
        const genIcon = row => {
            switch (row.state) {
                case "ALIVE":
                    return '<span class="mutantCUTImage mutantImageAlive"></span>';
                case "KILLED":
                    return '<span class="mutantCUTImage mutantImageKilled"></span>';
                case "EQUIVALENT":
                    return '<span class="mutantCUTImage mutantImageEquiv"></span>';
                case "FLAGGED":
                    return '<span class="mutantCUTImage mutantImageFlagged"></span>';
            }
        };
        const genViewButton = row => row.canView ? '<button class="ma-view-button btn btn-primary btn-xs pull-right">View</button>' : '';

        const genAdditionalButton = row => {
            switch (row.state) {
                    <c:if test="mutantAccordion.flag">
                case "ALIVE":
                    if (row.canMarkEquivalent) {
                        if (row.covered) {
                            return '<form id="equiv" action="${pageContext.request.contextPath + Paths.EQUIVALENCE_DUELS_GAME}" method="post" onsubmit="return confirm(\'This will mark all player-created mutants on line(s) ' + row.lineString + ' as equivalent. Are you sure?\');">\n' +
                                    '      <input type="hidden" name="formType" value="claimEquivalent">\n' +
                                    '      <input type="hidden" name="equivLines" value="' + row.lineString + '">\n' +
                                    '      <input type="hidden" name="gameId" value="${mutantAccordion.gameId}">\n' +
                                    '      <button type="submit" class="btn btn-default btn-xs pull-right">Claim Equivalent</button>\n' +
                                    '   </form>';
                        } else {
                            return '<button type="submit" class="btn btn-default btn-xs pull-right" disabled>Claim Equivalent</button>';
                        }
                    } else {
                        return '';
                    }

                    </c:if>
                case "KILLED":
                    return '<button class="ma-view-test-button btn btn-secondary btn-xs text-nowrap">View Killing Test</button>';
                default:
                    return '';
            }
        };

        /**
         * Returns the mutant DTO that describes the row of an element in a DataTables row.
         * @param {HTMLElement} element An HTML element contained in a table row.
         * @param {object} dataTable The DataTable the row belongs to.
         * @return {object} The mutant DTO the row describes.
         */
        const rowData = function (element, dataTable) {
            const row = $(element).closest('tr');
            return dataTable.row(row).data();
        };

        /**
         * Creates a modal to display the given mutant and shows it.
         * References to created models are cached in a map so they don't need to be generated again.
         * @param {object} mutant The mutant DTO to display.
         */
        const viewMutantModal = function (mutant) {
            let modal = mutantModals.get(mutant.id);
            if (modal !== undefined) {
                modal.modal('show');
                return;
            }

            modal = $(
                `<div class="modal fade" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-responsive">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Mutant \${mutant.id} (by \${mutant.creator.name})</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <div class="card">
                                    <div class="card-body p-0 codemirror-expand codemirror-mutant-modal-size">
                                        <pre class="m-0"><textarea name="mutant-\${mutant.id}"></textarea></pre>
                                    </div>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div>`);
            modal.appendTo(document.body);
            mutantModals.set(mutant.id, modal);

            const textarea = modal.find('textarea').get(0);
            const editor = CodeMirror.fromTextArea(textarea, {
                lineNumbers: true,
                matchBrackets: true,
                mode: "text/x-diff",
                readOnly: true,
                autoRefresh: true
            });

            MutantAPI.getAndSetEditorValueWithDiff(textarea, editor);
            modal.modal('show');
        };

        /**
         * Creates a modal to display the given test and shows it.
         * References to created models are cached in a map so they don't need to be generated again.
         * @param {object} test The test DTO to display.
         */
        const viewTestModal = function (test) {
            let modal = testModals.get(test.id);
            if (modal !== undefined) {
                modal.modal('show');
                return;
            }

            modal = $(
                `<div class="modal fade" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-responsive">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Test \${test.id} (by \${test.creatorName})</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <div class="card mb-3">
                                    <div class="card-body p-0 codemirror-expand codemirror-test-modal-size">
                                        <pre class="m-0"><textarea name="test-\${test.id}"></textarea></pre>
                                    </div>
                                </div>
                                <pre class="m-0 terminal-pre">\${test.killMessage}</pre>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div>`);
            modal.appendTo(document.body);
            testModals.set(test.id, modal);

            const textarea = modal.find('textarea').get(0);
            const editor = CodeMirror.fromTextArea(textarea, {
                lineNumbers: true,
                matchBrackets: true,
                mode: "text/x-java",
                readOnly: true,
                autoRefresh: true
            });

            TestAPI.getAndSetEditorValue(textarea, editor);
            modal.modal('show');
        };


        /* Loop through the categories and create a mutant table for each one. */
        for (const category of categories) {
            const rows = category.mutantIds
                    .sort()
                    .map(mutants.get, mutants);

            /* Create the DataTable. */
            const tableElement = $('#ma-table-' + category.id);
            const dataTable = tableElement.DataTable({
                data: rows,
                columns: [
                    {data: null, title: '', defaultContent: ''},
                    {data: genIcon, title: ''},
                    {data: genId, title: ''},
                    {data: genLines, title: ''},
                    {data: genPoints, title: ''},
                    {data: genViewButton, title: ''},
                    {data: genAdditionalButton, title: ''}
                ],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {
                    emptyTable: category.id === 'all'
                            ? 'No mutants.'
                            : 'No mutants in this method.'
                }
            });

            /* Assign function to the "View" buttons. */
            tableElement.on('click', '.ma-view-button', function () {
                const test = rowData(this, dataTable);
                viewMutantModal(test);
            });

            /* Assign function to the "View killing test" buttons. */
            tableElement.on('click', '.ma-view-test-button', function () {
                const mutant = rowData(this, dataTable);
                viewTestModal({
                    "id": mutant.killedByTestId,
                    "creatorName": mutant.killedByName,
                    "killMessage": mutant.killMessage
                });
            });

            /* Assign function to the "Mutant <id>" link. */
            tableElement.on('click', '.ma-mutant-link', function () {
                const mutant = rowData(this, dataTable);
                const cm = $('#cut-div, #newmut-div').find('.CodeMirror').get(0).CodeMirror;
                cm.scrollIntoView({line: mutant.lines[0] - 1, char: 0}, cm.getScrollInfo().clientHeight / 2 - 10);
                $("#cut-div, #newmut-div")[0].scrollIntoView();
            });
        }
    })();
</script>
