import DataTable from '../thirdparty/datatables';
import {InfoApi, LoadingAnimation, Modal, objects} from '../main';


class MutantAccordion {

    /**
     * @param {object[]} categories
     *      Given by [JSON.parse('${mutantAccordion.jsonFromCategories()}']
     * @param {Map<number, object>} mutants
     *      Given by [new Map(JSON.parse('${mutantAccordion.jsonMutants()}'))]
     * @param {number} gameId
     *      Given by [${mutantAccordion.gameId}]
     */
    constructor (categories, mutants, gameId) {
        /**
         * The categories of mutants to display, i.e. one category per method + all + outside methods.
         * @type {MutantAccordionCategory[]}
         */
        this._categories = categories;
        /**
         * Maps mutant ids to their mutant DTO.
         * @type {Map<number, MutantDTO>}
         */
        this._mutants = mutants


        /**
         * The id of the current game.
         * @type {number}
         */
        this._gameId = gameId;


        /**
         * Maps mutant ids to the modal that show the mutant's code.
         * @type {Map<number, Modal>}
         */
        this._mutantModals = new Map();
        /**
         * Maps mutant ids to the modal that shows the code of the mutant's killing test.
         * @type {Map<number, Modal>}
         */
        this._testModals = new Map();


        /**
         * Maps category ids to the datatable that displays the category.
         * @type {Map<number, DataTable>}
         */
        this._dataTablesByCategory = new Map();


        this._init();
    }

    /**
     * Creates a modal to display the given mutant and shows it.
     * References to created models are cached in a map so they don't need to be generated again.
     * @param {MutantDTO} mutant The mutant DTO to display.
     * @private
     */
    async _viewMutantModal (mutant) {
        let modal = this._mutantModals.get(mutant.id);
        if (modal !== undefined) {
            modal.controls.show();
            return;
        }

        /* Create a new modal. */
        modal = new Modal();
        modal.title.innerText = `Mutant ${mutant.id} (by ${mutant.creator.name})`;
        modal.body.innerHTML =
                `<div class="card">
                    <div class="card-body p-0 codemirror-expand codemirror-mutant-modal-size">
                        <pre class="m-0"><textarea></textarea></pre>
                    </div>
                </div>`;
        modal.dialog.classList.add('modal-dialog-responsive');
        modal.body.classList.add('loading', 'loading-bg-gray', 'loading-size-200');
        this._mutantModals.set(mutant.id, modal);

        /* Initialize the editor. */
        const textarea = modal.body.querySelector('textarea');
        const editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: 'text/x-diff',
            readOnly: true,
            autoRefresh: true
        });
        editor.getWrapperElement().classList.add('codemirror-readonly');

        modal.controls.show();

        await InfoApi.setMutantEditorValue(editor, mutant.id);
        LoadingAnimation.hideAnimation(modal.body);
    };

    /**
     * Creates a modal to display the given mutant's killing tests and kill message.
     * References to created models are cached in a map so they don't need to be generated again.
     * @param {MutantDTO} mutant The mutant DTO for which to display the test.
     * @private
     */
    async _viewTestModal (mutant) {
        let modal = this._testModals.get(mutant.id);
        if (modal !== undefined) {
            modal.controls.show();
            return;
        }

        /* Create a new modal. */
        modal = new Modal();
        modal.title.innerText =`Test ${mutant.killedByTestId} (by ${mutant.killedBy.name})`;
        modal.body.innerHTML =
                `<div class="card mb-3">
                    <div class="card-body p-0 codemirror-expand codemirror-test-modal-size">
                        <pre class="m-0"><textarea></textarea></pre>
                    </div>
                </div>
                <pre class="m-0 terminal-pre"></pre>`;
        modal.dialog.classList.add('modal-dialog-responsive');
        modal.body.classList.add('loading', 'loading-bg-gray', 'loading-size-200');
        this._testModals.set(mutant.killedByTestId, modal);

        /* Set the kill message. */
        const killMessageElement = modal.body.querySelector('.terminal-pre');
        killMessageElement.innerText = mutant.killMessage;

        /* Initialize the editor. */
        const textarea = modal.body.querySelector('textarea');
        const editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: 'text/x-java',
            readOnly: true,
            autoRefresh: true
        });
        editor.getWrapperElement().classList.add('codemirror-readonly');

        modal.controls.show();

        await InfoApi.setTestEditorValue(editor, mutant.killedByTestId);
        LoadingAnimation.hideAnimation(modal.body);
    };

    /** @private */
    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Loop through the categories and create a mutant table for each one. */
        for (const category of this._categories) {
            const rows = category.mutantIds
                    .sort()
                    .map(this._mutants.get, this._mutants);

            /* Create the DataTable. */
            const tableElement = document.getElementById(`ma-table-${category.id}`);
            const dataTable = new DataTable(tableElement, {
                data: rows,
                columns: [
                    {data: null, title: '', defaultContent: ''},
                    {data: this._renderIcon.bind(this), title: ''},
                    {data: this._renderId.bind(this), title: ''},
                    {data: this._renderLines.bind(this), title: ''},
                    {data: this._renderPoints.bind(this), title: ''},
                    {data: this._renderViewButton.bind(this), title: ''},
                    {data: this._renderAdditionalButton.bind(this), title: ''}
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
                    row.querySelector('.ma-mutant-link').addEventListener('click', async function (event) {
                        const editor = (await Promise.race([
                                objects.await('classViewer'),
                                objects.await('mutantEditor')
                        ])).editor;
                        editor.getWrapperElement().scrollIntoView();
                        editor.scrollIntoView(
                                {line: data.lines[0] - 1, char: 0},
                                editor.getScrollInfo().clientHeight / 2 - 10);
                    });
                }
            });

            this._dataTablesByCategory.set(category.id, dataTable);
        }

        this._initFilters();

        LoadingAnimation.hideAnimation(document.getElementById('mutants-accordion'));
    }

    /**
     * Initializes the filter radio to filter mutants by equivalence status.
     * @private
     */
    _initFilters () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Setup filter functionality for mutant-accordion. */
        document.getElementById('ma-filter')
                .addEventListener('change', function(event) {
                    const selectedCategory = event.target.value;
                    self.filterMutants(selectedCategory);
                });
    }

    /**
     * Filter displayed mutants according to the given equivalence state / category.
     * @param {string} state The equivalence state / category to display.
     */
    filterMutants (state) {
        const searchFunction = (settings, renderedData, index, data, counter) => {
            /* Let this only affect mutant accordion tables. */
            if (!settings.nTable.id.startsWith('ma-table-')) {
                return true;
            }

            return state === 'ALL'
                    || data.state === state;
        }

        DataTable.ext.search.push(searchFunction);

        for (const category of this._categories) {
            this._dataTablesByCategory.get(category.id).draw();

            const filteredMutants = category.mutantIds
                    .map(this._mutants.get, this._mutants)
                    .filter(mutant => state === 'ALL'
                            || mutant.state === state);

            document.getElementById('ma-count-' + category.id).innerText = filteredMutants.length;
        }

        /* Remove search function again after tables have been filtered. */
        DataTable.ext.search.splice(DataTable.ext.search.indexOf(searchFunction), 1);
    };

    /** @private */
    _renderId (data) {
        const killedByText =  data.killedBy
                ? `<span class="ma-column-name mx-2">killed by</span>${data.killedBy.name}`
                : '';
        return `<span class="ma-mutant-link">Mutant ${data.id}</span>
            <span class="ma-column-name mx-2">by</span>${data.creator.name}
            ${killedByText}`;
    }

    /** @private */
    _renderPoints (data) {
        return `<span class="ma-column-name">Points:</span> ${data.points}`;
    }

    /** @private */
    _renderLines (data) {
       return data.description;
    }

    /** @private */
    _renderIcon (data) {
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

    /** @private */
    _renderViewButton (data) {
        return data.canView
                ? '<button class="ma-view-button btn btn-primary btn-xs pull-right">View</button>'
                : '';
    }

    /** @private */
    _renderAdditionalButton (data) {
        switch (data.state) {
            case "ALIVE":
                if (data.canMarkEquivalent) {
                    if (data.covered) {
                        return `
                            <form id="equiv" action="equivalence-duels" method="post"
                            onsubmit="return confirm('This will mark all player-created mutants on line(s) ${data.lineString} as equivalent. Are you sure?');">
                                <input type="hidden" name="formType" value="claimEquivalent">
                                <input type="hidden" name="equivLines" value="${data.lineString}">
                                <input type="hidden" name="gameId" value="${this._gameId}">
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


export default MutantAccordion;
