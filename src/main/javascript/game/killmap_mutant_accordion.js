import DataTable from '../thirdparty/datatables';
import {InfoApi, LoadingAnimation, Modal, objects} from '../main';

/**
 * @typedef {object} MutantAccordionCategory
 * @property {string} description
 * @property {number[]} mutantIds
 * @property {number[]} testIds
 * @property {string} id
 */

/**
 * @typedef {object} MutantDTO
 * @property {number} id
 * @property {SimpleUser} creator
 * @property {"ALIVE"|"KILLED"|"FLAGGED"|"EQUIVALENT"} state
 * @property {number} points
 * @property {string} lineString
 * @property {SimpleUser} killedBy
 * @property {boolean} canMarkEquivalent
 * @property {boolean} canView
 * @property {number} killedByTestId
 * @property {string} killMessage
 * @property {string} description
 * @property {boolean} covered
 * @property {number[]} lines
 */

/**
 * @typedef {object} TestDTO
 * @property {number} id
 * @property {SimpleUser} creator
 * @property {number} points
 * @property {boolean} canView
 * @property {number[]} coveredMutantIds
 * @property {number[]} killedMutantIds
 * @property {string[]} smells
 */

/**
 * @typedef {object} SimpleUser
 * @property {number} id
 * @property {string} name
 */

class KillMapMutantAccordion {

    /**
     * @param {object[]} categories
     *      Given by [JSON.parse('${mutantAccordion.jsonFromCategories()}']
     * @param {Map<number, object>} mutants
     *      Given by [new Map(JSON.parse('${killMapAccordion.mutantsAsJSON()}'))]
     * @param {Map<number, object>} tests
     *     Given by [new Map(JSON.parse('${killMapAccordion.testsAsJSON()}'))]
     * @param {Record<number, Record<number, 'KILL'|'NO_KILL'|'NO_COVERAGE'|'ERROR'|'UNKNOWN'>>} killMap
     *    Given by [JSON.parse('${killMapAccordion.killMapForMutantsAsJSON()}')]
     *    It maps mutant ids to an object that maps test ids to the kill-map result.
     * @param {number} gameId
     *      Given by [${mutantAccordion.gameId}]
     */
    constructor(categories, mutants, tests, killMap, gameId) {
        /**
         * The categories of mutants to display, i.e. one category per method + all + outside methods.
         * @type {MutantAccordionCategory[]}
         */
        this._categories = categories;

        /**
         * Maps mutant ids to their mutant DTO.
         * @type {Map<number, MutantDTO>}
         */
        this._mutants = mutants;

        /**
         * Maps test ids to their test DTO.
         * @type {Map<number, TestDTO>}
         */
        this._tests = tests;

        /**
         * Maps mutant ids to a record which maps test ids to the kill-map result.
         * @type {Record<number, Record<number, 'KILL'|'NO_KILL'|'NO_COVERAGE'|'ERROR'|'UNKNOWN'>>}
         */
        this._killMap = killMap;

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
         * Maps test ids to the modal that shows the code of the mutant's killing test.
         * @type {Map<number, Modal>}
         */
        this._testModals = new Map();

        /**
         * Maps category ids to the mutants with their datatable that displays the tests of the category.
         * @type {Map<number, Map<number, DataTable>>}
         */
        this._dataTablesByCategoryAndMutant = new Map();


        this._init();
    }

    /** @private */
    _init() {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Loop through the categories and mutants and create a test table for each one. */
        for (const category of this._categories) {
            console.log(`#ma-collapse-${category.id}`);
            const categoryAccordion = document.querySelector(`#ma-collapse-${category.id}`);
            for (const mutantId of category.mutantIds) {
                /* Create the DataTable. */
                const tableElement = categoryAccordion.querySelector(`#ma-table-mutant-${mutantId}`);
                const rows = category.testIds
                    .sort((a, b) => a - b)
                    .map(testId => {
                        const testDTO = this._tests.get(testId);
                        testDTO.killMapResult = this._killMap[mutantId][testId];
                        return testDTO;
                    });

                console.log(category.description, JSON.stringify(rows));

                const dataTable = new DataTable(tableElement, {
                    data: rows,
                    columns: [
                        {data: null, title: '', defaultContent: ''},
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
                            : 'No tests cover this method.'
                    },
                    createdRow: function (row, data, index) {
                        // self._setupPopover(
                        //     row.querySelector('.ta-covered-link'),
                        //     data,
                        //     self._renderCoveredMutantsPopoverTitle.bind(self),
                        //     self._renderCoveredMutantsPopoverBody.bind(self)
                        // );
                        //
                        // self._setupPopover(
                        //     row.querySelector('.ta-killed-link'),
                        //     data,
                        //     self._renderKilledMutantsPopoverTitle.bind(self),
                        //     self._renderKilledMutantsPopoverBody.bind(self)
                        // );
                        //
                        // self._setupPopover(
                        //     row.querySelector('.ta-smells-link'),
                        //     data,
                        //     self._renderSmellsPopoverTitle.bind(self),
                        //     self._renderSmellsPopoverBody.bind(self)
                        // );
                        //
                        // const element = row.querySelector('.ta-view-button');
                        // if (element !== null) {
                        //     element.addEventListener('click', function (event) {
                        //         self._viewTestModal(data);
                        //     });
                        // }
                    }
                });
            }
        }

        console.log(this)


        // LoadingAnimation.hideAnimation(document.getElementById('mutant-categories-accordion'));
    }

    _renderKillMapResult(data) {
        switch (data.killMapResult) {
            case 'KILL':
                return '<span class="ta-kill-map-result ta-kill-map-result-kill">Kill</span>';
            case 'NO_KILL':
                return '<span class="ta-kill-map-result ta-kill-map-result-no-kill">No Kill</span>';
            case 'NO_COVERAGE':
                return '<span class="ta-kill-map-result ta-kill-map-result-no-coverage">No Coverage</span>';
            case 'ERROR':
                return '<span class="ta-kill-map-result ta-kill-map-result-error">Error</span>';
            case 'UNKNOWN':
            default:
                return '<span class="ta-kill-map-result ta-kill-map-result-unknown">?</span>';
        }
    }

    /** @private */
    _renderId(data) {
        return `Test ${data.id}`;
    }

    /** @private */
    _renderCreator(data) {
        return data.creator.name;
    }

    /** @private */
    _renderPoints(data) {
        return `<span class="ta-column-name">Points:</span> ${data.points}`;
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
