import {Collapse} from '../thirdparty/bootstrap';
import DataTable from '../thirdparty/datatables';
import {LoadingAnimation} from '../main';
import KillMapAccordion from "./killmap_accordion";

class KillMapMutantAccordion extends KillMapAccordion {

    /**
     * @inheritDoc
     */
    constructor(categories, mutants, tests, killMap, gameId) {
        super(categories, mutants, tests, killMap, gameId);

        /**
         * Maps category ids to the mutants with their datatable that displays the tests of the category.
         * @type {Map<string, Map<number, DataTable>>}
         */
        this._dataTablesByCategoryAndMutant = new Map();


        this._init();
    }

    /**
     * @private
     * @override
     */
    _init() {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Loop through the categories and mutants and create a test table for each one. */
        for (const category of this._categories) {
            const categoryAccordion = document.querySelector(`#kma-collapse-${category.id}`);
            this._dataTablesByCategoryAndMutant.set(category.id, new Map());
            for (const mutantId of category.mutantIds) {
                const identifier = `category-${category.id}-mutant-${mutantId}`;
                const mutant = self._mutants.get(mutantId);

                /* Init "View mutant" / "View killing test" buttons and accordion trigger. */
                const headingElem = categoryAccordion.querySelector(`#kma-heading-${identifier}`);
                const collapseElem = categoryAccordion.querySelector(`#kma-collapse-${identifier}`);
                headingElem.addEventListener('click', function (event) {
                    if (event.target.classList.contains('kta-view-mutant-button')) {
                        self._viewMutantModal(mutant);
                    } else if (event.target.classList.contains('kta-view-killing-test-button')) {
                        const killingTest = self._tests.get(mutant.killedByTestId);
                        self._viewMutantTestModal(mutant, killingTest);
                    } else {
                        Collapse.getOrCreateInstance(collapseElem).toggle();
                    }
                });

                /* Create the DataTable. */
                const tableElement = categoryAccordion.querySelector(`#kma-table-${identifier}`);
                const rows = category.testIds
                    .sort((a, b) => a - b)
                    .map(testId => ({...this._tests.get(testId), killMapResult: this._killMapAt(mutantId, testId)}));

                const dataTable = new DataTable(tableElement, {
                    data: rows,
                    columns: [
                        {data: this._renderKillMapResult.bind(this), title: ''},
                        {data: this._renderId.bind(this), title: ''},
                        {data: this._renderCreator.bind(this), title: ''},
                        {data: this._renderCoveredMutants.bind(this), title: ''},
                        {data: this._renderKilledMutants.bind(this), title: ''},
                        {data: this._renderPoints.bind(this), title: ''},
                        {data: this._renderSmells.bind(this), title: ''},
                        {data: this._renderViewButton.bind(this), title: ''}
                    ],
                    scrollY: '400px',
                    scrollCollapse: true,
                    paging: false,
                    dom: 't',
                    language: {
                        emptyTable: category.id === 'all'
                            ? 'No tests.'
                            : "This mutant isn't covered by any tests",
                        zeroRecords: 'No tests match the selected category and filter.'
                    },
                    createdRow: function (row, data, index) {
                        self._setupPopover(
                            row.querySelector('.killMapImage'),
                            data,
                            self._renderKillMapImagePopoverTitle.bind(self),
                            self._renderKillMapImagePopoverBody.bind(self)
                        );

                        self._setupPopover(
                            row.querySelector('.ta-covered-link'),
                            data,
                            self._renderCoveredMutantsPopoverTitle.bind(self),
                            self._renderCoveredMutantsPopoverBody.bind(self)
                        );

                        self._setupPopover(
                            row.querySelector('.ta-killed-link'),
                            data,
                            self._renderKilledMutantsPopoverTitle.bind(self),
                            self._renderKilledMutantsPopoverBody.bind(self)
                        );

                        self._setupPopover(
                            row.querySelector('.ta-smells-link'),
                            data,
                            self._renderSmellsPopoverTitle.bind(self),
                            self._renderSmellsPopoverBody.bind(self)
                        );

                        const element = row.querySelector('.ta-view-button');
                        if (element !== null) {
                            element.addEventListener('click', function (event) {
                                self._viewMutantTestModal(mutant, data);
                            });
                        }
                    }
                });

                this._dataTablesByCategoryAndMutant.get(category.id).set(mutantId, dataTable);
            }
        }

        this._initFilters();

        LoadingAnimation.hideAnimation(document.getElementById('kill-map-mutant-accordion'));
    }

    /**
     * Initializes the filter radio to filter mutants by equivalence status.
     * @private
     * @override
     */
    _initFilters() {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Setup filter functionality for mutant-accordion */
        document.getElementById('kma-filter')
            .addEventListener('change', function (event) {
                const selectedKillMapResult = event.target.value;

                const searchFunction = (settings, renderedData, index, data, counter) => {
                    /* Let this only affect kill-map mutant accordion tables. */
                    if (!settings.nTable.id.startsWith('kma-table-')) {
                        return true;
                    }
                    return selectedKillMapResult === 'ALL' || selectedKillMapResult === data.killMapResult;
                }

                DataTable.ext.search.push(searchFunction);

                for (const category of self._categories) {
                    for (const mutantId of category.mutantIds) {
                        self._dataTablesByCategoryAndMutant.get(category.id).get(mutantId).draw();
                    }
                }

                /* Remove search function again after tables have been filtered. */
                DataTable.ext.search.splice(DataTable.ext.search.indexOf(searchFunction), 1);
            })
    }

    /** @private */
    _renderId(data) {
        return `Test ${data.id}`;
    }

    /** @private */
    _renderCoveredMutants(data) {
        return `<span class="ta-covered-link"><span class="ta-column-name">Covered:</span> ${data.coveredMutantIds.length}</span>`;
    }

    /** @private */
    _renderKilledMutants(data) {
        return `<span class="ta-killed-link"><span class="ta-column-name">Killed:</span> ${data.killedMutantIds.length}</span>`;
    }

    /** @private */
    _renderViewButton(data) {
        return data.canView
            ? '<button class="ta-view-button btn btn-xs btn-primary">View</button>'
            : '';
    }

    /** @private */
    _renderSmells(data) {
        const numSmells = data.smells.length;
        let smellLevel;
        let smellColor;
        if (numSmells >= 3) {
            smellLevel = 'Bad';
            smellColor = 'btn-danger';
        } else if (numSmells >= 1) {
            smellLevel = 'Fishy';
            smellColor = 'btn-warning';
        } else {
            smellLevel = 'Good';
            smellColor = 'btn-success';
        }
        return `<a class="ta-smells-link btn btn-xs ${smellColor}">${smellLevel}</a>`;
    }

    /** @private */
    _renderCoveredMutantsPopoverTitle(data) {
        return data.coveredMutantIds.length > 0
            ? 'Covered Mutants'
            : '';
    }

    /** @private */
    _renderCoveredMutantsPopoverBody(data) {
        return data.coveredMutantIds.length > 0
            ? data.coveredMutantIds.join(', ')
            : 'No mutants are covered by this test.';
    }

    /** @private */
    _renderKilledMutantsPopoverTitle(data) {
        return data.killedMutantIds.length > 0
            ? 'Killed Mutants'
            : '';
    }

    /** @private */
    _renderKilledMutantsPopoverBody(data) {
        return data.killedMutantIds.length > 0
            ? data.killedMutantIds.join(', ')
            : 'No mutants were killed by this test.';
    }

    /** @private */
    _renderSmellsPopoverTitle(data) {
        return data.smells.length > 0
            ? 'Test Smells'
            : '';
    }

    /** @private */
    _renderSmellsPopoverBody(data) {
        return data.smells.length > 0
            ? data.smells.join('<br>')
            : 'This test does not have any smells.'
    }

}


export default KillMapMutantAccordion;
