import DataTable from '../thirdparty/datatables';
import {InfoApi, LoadingAnimation, Modal, objects} from '../main';
import {Popover} from "../thirdparty/bootstrap";

/**
 * @typedef {object} KillMapAccordionCategory
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

class KillMapAccordion {

    /**
     * @param {object[]} categories
     *      Given by [JSON.parse('${killMapAccordion.categoriesAsJSON()}']
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
         * @type {KillMapAccordionCategory[]}
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
    }

    /**
     * @protected
     * @abstract
     */
    _init() {
    }

    /**
     * Sets up a popover trigger on the given element.
     * @param {HTMLElement} triggerElement A DOM element to be used as the popover trigger.
     * @param {object} data The data of the row, as given by datatables.
     * @param {function} renderTitle A function to render the heading of the popover with.
     * @param {function} renderContent A function to render the body of the popover with.
     * @protected
     */
    _setupPopover(triggerElement, data, renderTitle, renderContent) {
        new Popover(triggerElement, {
            container: document.body,
            template:
                `<div class="popover" role="tooltip">
                    <div class="popover-arrow"></div>
                    <h3 class="popover-header"></h3>
                    <div class="popover-body px-3 py-2" style="max-width: 250px;"></div>
                </div>`,
            placement: 'top',
            trigger: 'hover',
            html: true,
            title: () => renderTitle(data),
            content: () => renderContent(data)
        });
    };

    /**
     * Creates a modal to display the given mutant and shows it.
     * References to created models are cached in a map, so they don't need to be generated again.
     * @param {MutantDTO} mutant The mutant DTO to display.
     * @protected
     */
    async _viewMutantModal(mutant) {
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
     * Creates a modal to display the given test and shows it.
     * References to created models are cached, so they don't need to be generated again.
     * @param {TestDTO} test The test DTO to display.
     * @protected
     */
    async _viewTestModal(test) {
        let modal = this._testModals.get(test.id);
        if (modal !== undefined) {
            modal.controls.show();
            return;
        }

        /* Create a new modal. */
        modal = new Modal();
        modal.title.innerText = `Test ${test.id} (by ${test.creator.name})`;
        modal.body.innerHTML =
            `<div class="card">
                    <div class="card-body p-0 codemirror-expand codemirror-test-modal-size">
                        <pre class="m-0"><textarea></textarea></pre>
                    </div>
                </div>`;
        modal.dialog.classList.add('modal-dialog-responsive');
        modal.body.classList.add('loading', 'loading-bg-gray', 'loading-size-200');
        this._testModals.set(test.id, modal);

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

        await InfoApi.setTestEditorValue(editor, test.id);
        LoadingAnimation.hideAnimation(modal.body);
    };

    /**
     * Initializes the filter radio to filter mutants/tests by kill-map status.
     * @protected
     * @abstract
     */
    _initFilters() {
    }

    _renderKillMapResult(data) {
        switch (data.killMapResult) {
            case 'KILL':
                return '<span class="ka-kill-map-result ka-kill-map-result-kill">Kill</span>';
            case 'NO_KILL':
                return '<span class="ka-kill-map-result ka-kill-map-result-no-kill">No Kill</span>';
            case 'NO_COVERAGE':
                return '<span class="ka-kill-map-result ka-kill-map-result-no-coverage">No Coverage</span>';
            case 'ERROR':
                return '<span class="ka-kill-map-result ka-kill-map-result-error">Error</span>';
            case 'UNKNOWN':
            default:
                return '<span class="ka-kill-map-result ka-kill-map-result-unknown">?</span>';
        }
    }

    /** @protected */
    _renderCreator(data) {
        return data.creator.name;
    }

    /** @protected */
    _renderPoints(data) {
        return `<span class="ka-column-name">Points:</span> ${data.points}`;
    }

}


export default KillMapAccordion;
