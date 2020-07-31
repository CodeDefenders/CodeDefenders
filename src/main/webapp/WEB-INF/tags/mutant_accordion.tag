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

<style type="text/css">
    <%-- Prefix all classes with "ta-" to avoid conflicts.
    We probably want to extract some common CSS when we finally tackle the CSS issue. --%>

    #mutants-accordion {
        margin-bottom: 0;
    }

    #mutants-accordion .panel-body {
        padding: 0;
    }

    #mutants-accordion thead {
        display: none;
    }

    #mutants-accordion .dataTables_scrollHead {
        display: none;
    }

    #mutants-accordion .panel-heading {
        padding-top: .375em;
        padding-bottom: .375em;
    }

    #mutants-accordion td {
        vertical-align: middle;
    }

    #mutants-accordion .panel-title.ma-covered {
        color: black;
    }

    #mutants-accordion .panel-title:not(.ma-covered) {
        color: #B0B0B0;
    }

    #mutants-accordion .ma-column-name {
        color: #B0B0B0;
    }

    #mutants-accordion .ma-count {
        margin-right: .5em;
        padding-bottom: .2em;
    }

    #mutants-accordion .ma-covered-link,
    #mutants-accordion .ma-killed-link {
        color: inherit;
    }

    #mutants-accordion .panel-heading a {
        text-decoration: none;
    }

    #mutants-accordion .ma-mutant-link {
        padding: 0
    }

    .modal.mutant-modal .modal-dialog {
        width: max-content;
        max-width: 90%;
        min-width: 500px;
    }
</style>

<div class="panel panel-default">
    <div class="panel-body" id="mutants">
        <div class="panel-group" id="mutants-accordion">
            <c:forEach items="${mutantAccordion.categories}" var="category">
                <div class="panel panel-default">
                    <div class="panel-heading" id="ma-heading-${category.id}">
                        <a role="button" data-toggle="collapse" aria-expanded="false"
                           href="#ma-collapse-${category.id}"
                           aria-controls="ma-collapse-${category.id}"
                            <%-- ${empty …} doesn't work with Set --%>
                           class="panel-title ${category.mutantIds.size() == 0 ? "" : 'ma-covered'}"
                           style="text-decoration: none;">
                                <%-- ${empty …} doesn't work with Set --%>
                            <c:if test="${!(category.mutantIds.size() == 0)}">
                                <span class="label bg-attacker ma-count">${category.mutantIds.size()}</span>
                            </c:if>
                                ${category.description}
                        </a>
                    </div>
                    <div class="panel-collapse collapse" data-parent="#mutants-accordion"
                         id="ma-collapse-${category.id}"
                         aria-labelledby="ma-heading-${category.id}">
                        <div class="panel-body">
                            <table id="ma-table-${category.id}" class="table table-sm"></table>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </div>
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
        const genId = row => `<span class="ma-mutant-link btn-link">Mutant \${row.id}</span>
                <span class="ma-column-name">  by  </span> \${row.creator.name}
                \${row.killedByName ? ' <span class="ma-column-name">  killed by  </span> ' + row.killedByName : ''}`;
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
        const genViewButton = row => row.canView ? '<button class="ma-view-button btn btn-primary btn-ssm btn-right">View</button>' : '';

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
                                    '      <button type="submit" class="btn btn-default btn-ssm btn-right">Claim Equivalent</button>\n' +
                                    '   </form>';
                        } else {
                            return '<button type="submit" class="btn btn-default btn-ssm btn-right" disabled>Claim Equivalent</button>';
                        }
                    } else {
                        return '';
                    }

                    </c:if>
                case "KILLED":
                    return '<button class="ma-view-test-button btn btn-default btn-ssm btn-right">View Killing Test</button>';
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
                    `<div class="modal mutant-modal fade" role="dialog">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal">&times;</button>
                                <h4 class="modal-title">Mutant ` + mutant.id + ` (by ` + mutant.creatorName + `)</h4>
                            </div>
                            <div class="modal-body">
                                <pre class="readonly-pre"><textarea class="mutdiff" name="mutant-` + mutant.id + `"></textarea></pre>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
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

            });
            editor.setSize('max-content', 'max-content');

            <%-- TODO: Is there a better solution for this? --%>
            /* Refresh the CodeMirror instance once the modal is displayed.
             * If this is not done, it will display an empty textarea until it is clicked. */
            new MutationObserver((mutations, observer) => {
                for (const mutation of mutations) {
                    if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                        editor.refresh();
                        observer.disconnect();
                    }
                }
            }).observe(modal.get(0), {attributes: true});

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
                    `<div class="modal mutant-modal fade" role="dialog">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal">&times;</button>
                                <h4 class="modal-title">Test ` + test.id + ` (by ` + test.creatorName + `)</h4>
                            </div>
                            <div class="modal-body">
                                <pre class="readonly-pre"><textarea name="test-` + test.id + `"></textarea></pre>
                                <pre class="readonly-pre build-trace">` + test.killMessage + `</pre>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
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

            });
            editor.setSize('max-content', 'max-content');

            <%-- TODO: Is there a better solution for this? --%>
            /* Refresh the CodeMirror instance once the modal is displayed.
             * If this is not done, it will display an empty textarea until it is clicked. */
            new MutationObserver((mutations, observer) => {
                for (const mutation of mutations) {
                    if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                        editor.refresh();
                        observer.disconnect();
                    }
                }
            }).observe(modal.get(0), {attributes: true});

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
