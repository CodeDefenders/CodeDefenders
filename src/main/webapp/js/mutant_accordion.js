/* Wrap in a function so it has it's own scope. */
(function () {

class MutantAccordion {

    /**
     * @param {object[]} categories
     *      Given by [JSON.parse('${mutantAccordion.jsonFromCategories()}']
     * @param {Map<number, object>} mutants
     *      Given by [new Map(JSON.parse('${mutantAccordion.jsonMutants()}'))]
     * @param {string} equivalenceDuelUrl
     *      Given by ['${pageContext.request.contextPath}${Paths.EQUIVALENCE_DUELS_GAME}']
     * @param {number} gameId
     *      Given by [${mutantAccordion.gameId}]
     */
    constructor (categories, mutants, equivalenceDuelUrl, gameId) {
        /** The categories of mutants to display. */
        this.categories = categories;

        /** Maps mutant ids to the corresponding mutants. */
        this.mutants = mutants

        /** URL to POST to for flagging mutants. */
        this.flaggingUrl = flaggingUrl;
        /** The id of the current game. */
        this.gameId = gameId;

        /** Maps mutant ids to modals that show the mutants' code. */
        this.mutantModals = new Map();
        /** Maps mutant ids to modals that show their killing test's code. */
        this.testModals = new Map();

        /** Stores datatables by the id of the category they display. */
        this.dataTablesByCategory = new Map();

        this._init();
    }

    /**
     * Creates a modal to display the given mutant and shows it.
     * References to created models are cached in a map so they don't need to be generated again.
     * @param {object} mutant The mutant DTO to display.
     */
    _viewMutantModal (mutant) {
        let modal = this.mutantModals.get(mutant.id);
        if (modal !== undefined) {
            modal.modal('show');
            return;
        }

        modal = $(
                `<div class="modal fade" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-dialog-responsive">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title">Mutant ${mutant.id} (by ${mutant.creator.name})</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="card">
                                        <div class="card-body p-0 codemirror-expand codemirror-mutant-modal-size">
                                            <pre class="m-0"><textarea></textarea></pre>
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
        this.mutantModals.set(mutant.id, modal);

        const textarea = modal.find('textarea').get(0);
        const editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: 'text/x-diff',
            readOnly: true,
            autoRefresh: true
        });

        CodeDefenders.classes.InfoApi.setMutantEditorValue(editor, mutant.id);
        modal.modal('show');
    };

    /**
     * Creates a modal to display the given mutant's killing tests and kill message.
     * References to created models are cached in a map so they don't need to be generated again.
     * @param {object} mutant The mutant DTO for which to display the test.
     */
    _viewTestModal (mutant) {
        let modal = this.testModals.get(mutant.id);
        if (modal !== undefined) {
            modal.modal('show');
            return;
        }

        modal = $(
                `<div class="modal fade" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-dialog-responsive">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title">Test ${mutant.killedByTestId} (by ${mutant.killedBy.name})</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="card mb-3">
                                        <div class="card-body p-0 codemirror-expand codemirror-test-modal-size">
                                            <pre class="m-0"><textarea></textarea></pre>
                                        </div>
                                    </div>
                                    <pre class="m-0 terminal-pre"></pre>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                </div>
                            </div>
                        </div>
                    </div>`);
        modal.appendTo(document.body);
        this.testModals.set(mutant.killedByTestId, modal);

        const killMessageElement = modal.find('.modal-body .terminal-pre').get(0);
        killMessageElement.innerText = mutant.killMessage;
        const textarea = modal.find('textarea').get(0);
        const editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: "text/x-java",
            readOnly: true,
            autoRefresh: true
        });

        CodeDefenders.classes.InfoApi.setTestEditorValue(editor, mutant.killedByTestId);
        modal.modal('show');
    };

    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Loop through the categories and create a mutant table for each one. */
        for (const category of this.categories) {
            const rows = category.mutantIds
                    .sort()
                    .map(this.mutants.get, this.mutants);

            /* Create the DataTable. */
            const tableElement = $('#ma-table-' + category.id);
            const dataTable = tableElement.DataTable({
                data: rows,
                columns: [
                    {data: null, title: '', defaultContent: ''},
                    {data: MutantAccordion.RenderFunctions.renderIcon, title: ''},
                    {data: MutantAccordion.RenderFunctions.renderId, title: ''},
                    {data: MutantAccordion.RenderFunctions.renderLines, title: ''},
                    {data: MutantAccordion.RenderFunctions.renderPoints, title: ''},
                    {data: MutantAccordion.RenderFunctions.renderViewButton, title: ''},
                    {data: MutantAccordion.RenderFunctions.renderAdditionalButton.bind(this), title: ''}
                ],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {
                    emptyTable: category.id === 'all'
                            ? 'No mutants.'
                            : 'No mutants in this method.',
                    zeroRecords: 'No mutants match the selected category.'
                },
                createdRow: function (row, data, index) {
                    /* Assign function to the "View" buttons. */
                    let element = row.querySelector('.ma-view-button');
                    if (element !== null) {
                        element.addEventListener('click', function (event) {
                            self._viewMutantModal(data);
                        })
                    }

                    /* Assign function to the "View killing test" buttons. */
                    element = row.querySelector('.ma-view-test-button');
                    if (element !== null) {
                        element.addEventListener('click', function (event) {
                            self._viewTestModal(data);
                        })
                    }

                    /* Assign function to the "Mutant <id>" link. */
                    row.querySelector('.ma-mutant-link').addEventListener('click', function (event) {
                        let editor = null;
                        if (CodeDefenders.objects.hasOwnProperty('classViewer')) {
                            editor = CodeDefenders.objects.classViewer.editor;
                        } else if (CodeDefenders.objects.hasOwnProperty('mutantEditor')) {
                            editor = CodeDefenders.objects.mutantEditor.editor;
                        }

                        if (editor == null) {
                            return;
                        }

                        editor.getWrapperElement().scrollIntoView();
                        editor.scrollIntoView(
                                {line: data.lines[0] - 1, char: 0},
                                editor.getScrollInfo().clientHeight / 2 - 10);
                    });
                }
            });

            this.dataTablesByCategory.set(category.id, dataTable);
        }

        this._initFilters();
    }

    /**
     * Initializes the filter radio to filter mutants by equivalence status.
     */
    _initFilters () {
        /* Setup filter functionality for mutant-accordion */
        document.getElementById('ma-filter')
                .addEventListener('change', function(event) {
                    const selectedCategory = event.target.value;

                    const searchFunction = (settings, renderedData, index, data, counter) => {
                        /* Let this only affect mutant accordion tables. */
                        if (!settings.nTable.id.startsWith('ma-table-')) {
                            return true;
                        }

                        return selectedCategory === 'ALL'
                                || data.state === selectedCategory;
                    }

                    $.fn.dataTable.ext.search.push(searchFunction);

                    for (const category of this.categories) {
                        this.dataTablesByCategory.get(category.id).draw();

                        const filteredMutants = category.mutantIds
                                .map(this.mutants.get, this.mutants)
                                .filter(mutant => selectedCategory === 'ALL'
                                        || mutant.state === selectedCategory);

                        document.getElementById('ma-count-' + category.id).innerText = filteredMutants.length;
                    }

                    /* Remove search function again after tables have been filtered. */
                    $.fn.dataTable.ext.search.splice($.fn.dataTable.ext.search.indexOf(searchFunction), 1);
                })
    }

    /**
     * Functions to render the table content from data.
     */
    static RenderFunctions = class RenderFunctions {
        static renderId (data) {
            const killedByText =  data.killedBy
                    ? `<span class="ma-column-name mx-2">killed by</span>${data.killedBy.name}`
                    : '';
            return `<span class="ma-mutant-link">Mutant ${data.id}</span>
                <span class="ma-column-name mx-2">by</span>${data.creator.name}
                ${killedByText}`;
        }

        static renderPoints (data) {
            return `<span class="ma-column-name">Points:</span> ${data.points}`;
        }

        static renderLines (data) {
           return data.description;
        }

        static renderIcon (data) {
            switch (data.state) {
                case "ALIVE":
                    return '<span class="mutantCUTImage mutantImageAlive"></span>';
                case "KILLED":
                    return '<span class="mutantCUTImage mutantImageKilled"></span>';
                case "EQUIVALENT":
                    return '<span class="mutantCUTImage mutantImageEquiv"></span>';
                case "FLAGGED":
                    return '<span class="mutantCUTImage mutantImageFlagged"></span>';
            }
        }

        static renderViewButton (data) {
            return data.canView
                    ? '<button class="ma-view-button btn btn-primary btn-xs pull-right">View</button>'
                    : '';
        }

        static renderAdditionalButton (data) {
            switch (data.state) {
                case "ALIVE":
                    if (data.canMarkEquivalent) {
                        if (data.covered) {
                            return `
                                <form id="equiv" action="${this.flaggingUrl}" method="post"
                                onsubmit="return confirm('This will mark all player-created mutants on line(s) ${data.lineString} as equivalent. Are you sure?');">
                                    <input type="hidden" name="formType" value="claimEquivalent">
                                    <input type="hidden" name="equivLines" value="${data.lineString}">
                                    <input type="hidden" name="gameId" value="${this.gameId}">
                                    <button type="submit" class="btn btn-outline-danger btn-xs text-nowrap">Claim Equivalent</button>
                                </form>`;
                        } else {
                            // We need the wrapper element (<span …), because tooltips do not work on disabled elements:
                            // https://getbootstrap.com/docs/5.1/components/tooltips/#disabled-elements
                            return `
                                <span class="d-inline-block" tabindex="0" data-bs-toggle="tooltip" title="Cover this mutant with a test to be able to claim it as equivalent">
                                    <button type="submit" class="btn btn-outline-danger btn-xs text-nowrap" disabled>Claim Equivalent</button>
                                </span>`;
                        }
                    } else {
                        return '';
                    }
                case "KILLED":
                    return '<button class="ma-view-test-button btn btn-secondary btn-xs text-nowrap">View Killing Test</button>';
                default:
                    return '';
            }
        }
    }
}

CodeDefenders.classes.MutantAccordion = MutantAccordion;

})();
