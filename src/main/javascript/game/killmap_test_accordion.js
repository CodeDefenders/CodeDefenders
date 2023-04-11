import {Collapse} from '../thirdparty/bootstrap';
import DataTable from '../thirdparty/datatables';
import {InfoApi, LoadingAnimation, Modal, objects} from '../main';
import {Popover} from "../thirdparty/bootstrap";
import KillMapAccordion from "./killmap_accordion";


class KillMapTestAccordion extends KillMapAccordion {

    /**
     * @inheritDoc
     */
    constructor(categories, mutants, tests, killMap, gameId) {
        super(categories, mutants, tests, killMap, gameId);

        /**
         * Maps category ids to the tests with their datatable that displays the mutants of the category.
         * @type {Map<string, Map<number, DataTable>>}
         */
        this._dataTablesByCategoryAndTest = new Map();

        this._init();
    }

    /** @private */
    _init() {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Loop through the categories and mutants and create a test table for each one. */
        for (const category of this._categories) {
            const categoryAccordion = document.querySelector(`#kta-collapse-${category.id}`);
            this._dataTablesByCategoryAndTest.set(category.id, new Map());
            for (const testId of category.testIds) {
                const identifier = `category-${category.id}-test-${testId}`;
                const test = self._tests.get(testId);

                /* Init "View test" button and accordion trigger. */
                const headingElem = categoryAccordion.querySelector(`#kta-heading-${identifier}`);
                const collapseElem = categoryAccordion.querySelector(`#kta-collapse-${identifier}`);
                headingElem.addEventListener('click', function (event) {
                    if (event.target.classList.contains('kta-view-test-button')) {
                        self._viewTestModal(test);
                    } else {
                        Collapse.getOrCreateInstance(collapseElem).toggle();
                    }
                });

                /* Create the DataTable. */
                const tableElement = categoryAccordion.querySelector(`#kta-table-${identifier}`);
                const rows = category.mutantIds
                    .sort((a, b) => a - b)
                    .map(mutantId => ({
                        ...this._mutants.get(mutantId),
                        killMapResult: this._killMapAt(mutantId, testId)
                    }));

                const dataTable = new DataTable(tableElement, {
                    data: rows,
                    columns: [
                        {data: null, title: '', defaultContent: ''},
                        // {data: this._renderIcon.bind(this), title: ''},
                        {data: this._renderKillMapResult.bind(this), title: ''},
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
                        zeroRecords: 'No mutants match the selected category and filter.'
                    },
                    createdRow: function (row, data, index) {
                        self._setupPopover(
                            row.querySelector('.killMapImage'),
                            data,
                            self._renderKillMapImagePopoverTitle.bind(self),
                            self._renderKillMapImagePopoverBody.bind(self)
                        );

                        /* Assign function to the "View" buttons. */
                        let element = row.querySelector('.ma-view-button');
                        if (element !== null) {
                            element.addEventListener('click', function (event) {
                                self._viewMutantTestModal(data, test);
                            })
                        }

                        /* Assign function to the "View killing test" buttons. */
                        element = row.querySelector('.ma-view-test-button');
                        if (element !== null) {
                            element.addEventListener('click', function (event) {
                                self._viewMutantTestModal(data, self._tests.get(data.killedByTestId));
                            })
                        }
                    }
                });

                this._dataTablesByCategoryAndTest.get(category.id).set(testId, dataTable);
            }
        }

        this._initFilters();

        LoadingAnimation.hideAnimation(document.getElementById('kill-map-test-accordion'));
    }

    /**
     * Initializes the filter radio to filter mutants by equivalence status.
     * @private
     */
    _initFilters() {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        document.getElementById('kta-filter')
            .addEventListener('change', function (event) {
                const selectedKillMapResult = event.target.value;

                const searchFunction = (settings, renderedData, index, data, counter) => {
                    /* Let this only affect kill-map test accordion tables. */
                    if (!settings.nTable.id.startsWith('kta-table-')) {
                        return true;
                    }
                    return selectedKillMapResult === 'ALL' || selectedKillMapResult === data.killMapResult;
                }

                DataTable.ext.search.push(searchFunction);

                for (const category of self._categories) {
                    for (const testId of category.testIds) {
                        self._dataTablesByCategoryAndTest.get(category.id).get(testId).draw();
                    }
                }

                /* Remove search function again after tables have been filtered. */
                DataTable.ext.search.splice(DataTable.ext.search.indexOf(searchFunction), 1);
            })
    }

    /** @private */
    _renderId(data) {
        const killedByText = data.killedBy
            ? `<span class="kta-column-name mx-2">killed by</span>${data.killedBy.name}`
            : '';
        return `<span class="ma-mutant-link">Mutant ${data.id}</span>
            <span class="kta-column-name mx-2">by</span>${data.creator.name}
            ${killedByText}`;
    }

    /** @private */
    _renderLines(data) {
        return data.description;
    }

    /** @private */
    _renderIcon(data) {
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
    _renderViewButton(data) {
        return data.canView
            ? '<button class="ma-view-button btn btn-primary btn-xs pull-right">View</button>'
            : '';
    }

    /** @private */
    _renderAdditionalButton(data) {
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


export default KillMapTestAccordion;
