/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
import {InfoApi, LoadingAnimation, Modal} from '../main';
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

/**
 * @typedef {'KILL'|'NO_KILL'|'NO_COVERAGE'|'ERROR'|'UNKNOWN'} KillMapEntryStatus
 * @typedef {Record<number, Record<number, KillMapEntryStatus>>} KillMapDTO
 */

class KillMapAccordion {

    /**
     * @param {object[]} categories
     *      Given by [JSON.parse('${killMapAccordion.categoriesAsJSON()}']
     * @param {Map<number, object>} mutants
     *      Given by [new Map(JSON.parse('${killMapAccordion.mutantsAsJSON()}'))]
     * @param {Map<number, object>} tests
     *     Given by [new Map(JSON.parse('${killMapAccordion.testsAsJSON()}'))]
     * @param {KillMapDTO} killMap
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
         * @type {KillMapDTO}
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
         * Maps mutant and test ids to the modal that shows the code of the mutant and the test side by side.
         * @type {Map<number, Map<number, Modal>>}
         */
        this._mutantTestModals = new Map();
    }

    /**
     * @protected
     * @param {Number} mutantId
     * @param {Number} testId
     * @return {KillMapEntryStatus} the result of the kill map run for the given mutant and test.
     */
    _killMapAt(mutantId, testId) {
        if (this._killMap[mutantId] === undefined || this._killMap[mutantId][testId] === undefined) {
            return 'UNKNOWN';
        } else {
            return this._killMap[mutantId][testId];
        }
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
     * Creates a modal to display the given mutant and shows it.
     * References to created models are cached in a map, so they don't need to be generated again.
     * @param {MutantDTO} mutant The mutant DTO to display.
     * @param {TestDTO} test The test DTO to display.
     * @protected
     */
    async _viewMutantTestModal(mutant, test) {
        let modal = this._mutantTestModals.get(mutant.id)?.get(test.id);
        if (modal !== undefined) {
            modal.controls.show();
            return;
        }

        /* Create a new modal. */
        modal = new Modal();
        modal.title.innerHTML =
            `<span>Mutant ${mutant.id} (by ${mutant.creator.name})</span>
            <span>Test ${test.id} (by ${test.creator.name})</span>`;
        modal.body.innerHTML =
            `<div class="card mutant">
                 <div class="card-body p-0 codemirror-expand codemirror-mutant-modal-size">
                     <pre class="m-0"><textarea></textarea></pre>
                 </div>
             </div>
             <div class="card test">
                 <div class="card-body p-0 codemirror-expand codemirror-mutant-modal-size">
                     <pre class="m-0"><textarea></textarea></pre>
                 </div>
             </div>`;
        modal.dialog.classList.add('modal-dialog-responsive', 'mutant-test-modal');
        modal.body.classList.add('loading', 'loading-bg-gray', 'loading-size-200');
        if (!this._mutantTestModals.has(mutant.id)) {
            this._mutantTestModals.set(mutant.id, new Map());
        }
        this._mutantTestModals.get(mutant.id).set(test.id, modal);

        /* Initialize the editor. */
        const textareaMutant = modal.body.querySelector('.mutant textarea');
        const editorMutant = CodeMirror.fromTextArea(textareaMutant, {
            lineNumbers: true,
            matchBrackets: true,
            mode: 'text/x-diff',
            readOnly: true,
            autoRefresh: true
        });
        editorMutant.getWrapperElement().classList.add('codemirror-readonly');

        const textareaTest = modal.body.querySelector('.test textarea');
        const editorTest = CodeMirror.fromTextArea(textareaTest, {
            lineNumbers: true,
            matchBrackets: true,
            mode: 'text/x-java',
            readOnly: true,
            autoRefresh: true
        });
        editorTest.getWrapperElement().classList.add('codemirror-readonly');

        modal.controls.show();

        await InfoApi.setMutantEditorValue(editorMutant, mutant.id);
        await InfoApi.setTestEditorValue(editorTest, test.id);
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
                return '<span class="killMapImage killMapImageKill" aria-label="Kill"></span>';
            case 'NO_KILL':
                return '<span class="killMapImage killMapImageNoKill" aria-label="No Kill"></span>';
            case 'NO_COVERAGE':
                return '<span class="killMapImage killMapImageNoCoverage" aria-label="No Coverage"></span>';
            case 'ERROR':
                return '<span class="killMapImage killMapImageError" aria-label="Error"></span>';
            case 'UNKNOWN':
            default:
                return '<span class="killMapImage killMapImageUnknown" aria-label="Unknown"></span>';
        }
    }

    _renderKillMapImagePopoverTitle(data) {
        switch (data.killMapResult) {
            case 'KILL':
                return 'Kill';
            case 'NO_KILL':
                return 'No kill';
            case 'NO_COVERAGE':
                return 'No coverage';
            case 'ERROR':
                return 'Error';
            case 'UNKNOWN':
            default:
                return '';
        }
    }

    _renderKillMapImagePopoverBody(data) {
        switch (data.killMapResult) {
            case 'KILL':
                return 'The mutant was killed by the test.';
            case 'NO_KILL':
                return 'The test covered the mutant, but did not kill it.';
            case 'NO_COVERAGE':
                return 'The mutant was not covered by this test.';
            case 'ERROR':
                return 'The execution resulted in an error, killing the mutant.';
            case 'UNKNOWN':
            default:
                return 'Unknown';
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
