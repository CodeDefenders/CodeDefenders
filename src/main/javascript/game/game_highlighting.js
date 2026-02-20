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
import {Popover} from '../thirdparty/bootstrap';
import {objects} from '../main';


class GameHighlighting {

    /**
     * @param {object} data
     *      Given by [JSON.parse('${gameHighlighting.JSON}')]
     * @param {boolean} enableFlagging
     *      Given by [Boolean(${gameHighlighting.enableFlagging})]
     * @param {number} gameId
     *      Given by [${gameHighlighting.gameId}]
     */
    constructor(data, enableFlagging, gameId) {
        /**
         * Maps line numbers (1-indexed) to ids of mutants with changes on the line.
         * @type {Map<number, number[]>}
         * @private
         */
        this._mutantIdsPerLine = new Map(data.mutantIdsPerLine);
        /**
         * Maps line numbers (1-indexed) to ids of tests covering on the line.
         * @type {Map<number, number[]>}
         * @private
         */
        this._testIdsPerLine = new Map(data.testIdsPerLine);


        /**
         * Maps mutant ids to their mutant DTO.
         * @type {Map<number, GHMutantDTO>}
         * @private
         */
        this._mutants = new Map(data.mutants);
        /**
         * Maps tests ids to their test DTO.
         * @type {Map<number, GHTestDTO>}
         * @private
         */
        this._tests = new Map(data.tests);


        /**
         * Maps line numbers (1-indexed) to ids of alternative (probably enemy-written) tests covering the line.
         * @type {Map<number, object>}
         * @private
         */
        this._alternativeTestIdsPerLine = new Map(data.alternativeTestIdsPerLine);
        /**
         * Maps tests ids their test DTO for the alternative (probably enemy-written) test.
         * @type {Map<number, object>}
         * @private
         */
        this._alternativeTests = new Map(data.alternativeTests);


        /**
         * Whether to enable flagging equivalent mutants in the popovers.
         * @type {boolean}
         * @private
         */
        this._enableFlagging = enableFlagging;
        /**
         * Game id of the current game.
         * @type {number}
         * @private
         */
        this._gameId = gameId;


        /**
         * We use a timeout to hide the popover after the icon or popover is not hovered for a certain time.
         * The timeout id is saved so it can be cleared if necessary.
         * @type {?number}
         * @private
         */
        this._popoverTimeout = null;
        /**
         * The currently active popover.
         * This is saved to enable hiding the currently active popover and to avoid showing the same popover twice.
         * @type {?Popover}
         * @private
         */
        this._activePopover = null;
    }

    async initAsync () {
        /**
         * The CodeMirror editor to provide highlighting on.
         * @type {CodeMirror}
         */
        this._editor = (await Promise.race([
            objects.await('classViewer'),
            objects.await('mutantEditor')
        ])).editor;

        return this;
    }

    static MutantStatuses = {
        ALIVE: 'ALIVE',
        KILLED: 'KILLED',
        FLAGGED: 'FLAGGED',
        EQUIVALENT: 'EQUIVALENT'
    }

    static MutantNames = {
        ALIVE: 'Alive Mutants',
        KILLED: 'Killed Mutants',
        FLAGGED: 'Flagged Mutants',
        EQUIVALENT: 'Equivalent Mutants'
    }

    static IconClasses = {
        ALIVE: ['mutantCUTImage', 'mutantImageAlive'],
        KILLED: ['mutantCUTImage', 'mutantImageKilled'],
        FLAGGED: ['mutantCUTImage', 'mutantImageFlagged'],
        EQUIVALENT: ['mutantCUTImage', 'mutantImageEquiv'],
        FLAG: ['mutantCUTImage', 'mutantImageFlagAction']
    }

    /**
     * Creates the HTML element that displays the mutant icons for one line.
     * @param {number} line The line number (starting at 1).
     * @param {GHMutantDTO[]} mutantsOnLine The mutants that modify the line.
     * @return {HTMLElement} The mutant icons.
     * @private
     */
    _createMutantIcons (line, mutantsOnLine) {
        /* Split the mutants list by the mutant status: {ALIVE: [...], KILLED: [...], ...} */
        const sortedMutants = {};
        for (const mutantStatus in GameHighlighting.MutantStatuses) {
            const mutantsInCategory = mutantsOnLine.filter(m => m.status === mutantStatus);
            if (mutantsInCategory.length > 0) {
                sortedMutants[mutantStatus] = mutantsInCategory;
            }
        }

        /* Create the icons for each mutant type on the line. */
        const mutantIcons = document.createElement('div');
        mutantIcons.classList.add('gh-mutant-icons');
        for (const mutantStatus in sortedMutants) {
            const mutantIcon = document.createElement('div');
            mutantIcon.classList.add('gh-mutant-icon');
            mutantIcon.classList.add(...GameHighlighting.IconClasses[mutantStatus]);

            /* Dataset entries are "converted" to kebab-case DOM attributes. */
            mutantIcon.dataset.mutantStatus = mutantStatus;
            mutantIcon.dataset.line = String(line);
            mutantIcon.dataset.count = sortedMutants[mutantStatus].length;
            mutantIcon.dataset.canClaim = String(mutantsOnLine.some(element => element.canClaim));

            mutantIcons.appendChild(mutantIcon);
        }

        return mutantIcons;
    }

    /**
     * Adds a trigger to the mutant icons, which opens the popover and closes all other open popovers.
     * @param {HTMLElement} mutantIcons The element containing the mutant icons.
     * @private
     */
    _addPopoverTriggerToMutantIcons (mutantIcons) {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Sets a timeout to hide the given popover. */
        const setPopoverTimeout = function (popover) {
            self._popoverTimeout = setTimeout(function () {
                popover.hide();
                self._activePopover = null;
                self._popoverTimeout = null;
            }, 500);
        };

        /* Clears (cancels) the current popover timeout, if any is set. */
        const clearPopoverTimeout = function () {
            clearTimeout(self._popoverTimeout);
            self._popoverTimeout = null;
        };

        for (const mutantIcon of mutantIcons.querySelectorAll('.gh-mutant-icon')) {
            const popover = new Popover(mutantIcon, {
                /* Append to body instead of the element itself, so that the icons don't overlap modals. */
                container: document.body,
                placement: 'right',
                trigger: 'manual',
                html: true,
                customClass: 'popover-fluid',
                title: this._createPopoverTitle.bind(this, mutantIcon),
                content: this._createPopoverContent.bind(this, mutantIcon)
            });

            /* Activate popovers manually when an icon is hovered. This way, we can make the popover stay open as long
             * as either the icon or the popover itself is hovered. */
            mutantIcon.addEventListener('mouseenter', function (event) {
                clearPopoverTimeout();

                /* Do nothing if the popover is already active. */
                if (self._activePopover !== popover) {

                    /* Hide active popover */
                    if (self._activePopover != null) {
                        self._activePopover.hide();
                        self._activePopover = null;
                    }

                    /* Show new popover. */
                    popover.show();
                    self._activePopover = popover;
                }
            });

            mutantIcon.addEventListener('mouseleave', setPopoverTimeout.bind(null, popover));

            mutantIcon.addEventListener('inserted.bs.popover', function (event) {
                popover.tip.addEventListener('mouseenter', clearPopoverTimeout);
                popover.tip.addEventListener('mouseleave', setPopoverTimeout.bind(null, popover));
            });
        }
    }

    /**
     * Creates the title for a popover.
     * @param {HTMLElement} mutantIcon The icon element.
     * @returns {string} The popover title.
     * @private
     */
    _createPopoverTitle (mutantIcon) {
        const status = mutantIcon.dataset.mutantStatus;
        const line = Number(mutantIcon.dataset.line);

        return '' +
                `<div class="d-flex align-items-center gap-2">
                    <div class="${GameHighlighting.IconClasses[status].join(' ')}"></div>
                    ${GameHighlighting.MutantNames[status]} (Line ${line})
                </div>`;
    }

    /**
     * Creates the body for a popover.
     * @param {HTMLElement} mutantIcon The icon element.
     * @returns {HTMLElement} The popover body.
     * @private
     */
    _createPopoverContent (mutantIcon) {
        const status = mutantIcon.dataset.mutantStatus;
        const canClaim = Boolean(JSON.parse(mutantIcon.dataset.canClaim));
        const line = Number(mutantIcon.dataset.line);

        const mutantsOnLine = this._mutantIdsPerLine.get(line)
                .map(id => this._mutants.get(id))
                .filter(mutant => mutant.status === status);

        const head =
                `<thead>
                    <tr>
                        <td>Creator</td>
                        <td class="text-end">ID</td>
                        <td class="text-end">Score</td>
                        <td class="text-end">Changed Lines</td>
                    </tr>
                </thead>`;

        const rows = [];
        for (const mutant of mutantsOnLine) {
            rows.push(
                    `<tr>
                        <td>${mutant.creatorName}</td>
                        <td class="text-end">${mutant.id}</td>
                        <td class="text-end">${mutant.score}</td>
                        <td class="text-end">${mutant.lines}</td>
                    </tr>`
            );
        }

        const table =
                `<div>
                    <table class="table table-sm table-no-last-border m-0">
                        ${head}
                        <tbody>
                             ${rows.join('\n')}
                        </tbody>
                     </table>
                </div>`;

        /* Create the button if it is supposed to be shown. */
        let button = '';
        if (this._enableFlagging
                && status === GameHighlighting.MutantStatuses.ALIVE
                && canClaim) {
            if (this._testIdsPerLine.get(line)) {
                button = this._createEquivalenceButton(line);
            } else {
                button = this._createUncoveredEquivalenceButton(line);
            }
        }

        const content = document.createElement('div');
        content.classList.add('mutant-popover-body');
        content.innerHTML = table + button;

        return content;
    }

    /**
     * Creates the button with which to flag mutants as equivalent.
     * @param {number} line The line number.
     * @return {string} The equivalence button.
     * @private
     */
    _createEquivalenceButton (line) {
        return '' +
                `<form class="mt-3" id="equiv" action="equivalence-duels" method="post"
                    onsubmit="return window.confirm('This will mark all player-created mutants on line ${line} as equivalent. Are you sure?')">
                    <input type="hidden" name="formType" value="claimEquivalent">
                    <input type="hidden" name="equivLines" value="${line}">
                    <input type="hidden" name="gameId" value="${this._gameId}">
                    <button class="btn btn-danger btn-sm w-100 d-flex justify-content-center align-items-center gap-2">
                        <div class="${GameHighlighting.IconClasses.FLAG.join(' ')}"></div>
                        <span>Claim Equivalent</span>
                    </button>
                </form>`;
    }

    /**
     * Creates a disabled equivalence button with a tooltip explaining that the line needs to be covered first.
     * @param {number} line The line number.
     * @return {string} The equivalence button.
     * @private
     */
    _createUncoveredEquivalenceButton (line) {
        return '' +
                `<span class="d-inline-block w-100 mt-3" tabindex="0" title="Cover this mutant with a test to be able to claim it as equivalent.">
                    <button class="btn btn-danger btn-sm w-100 d-flex justify-content-center align-items-center gap-2" disabled>
                        <div class="${GameHighlighting.IconClasses.FLAG.join(' ')}"></div>
                        <span>Claim Equivalent</span>
                    </button>
                </span>`;
    }

    /**
     * Highlights coverage on the given CodeMirror instance.
     */
    highlightCoverage () {
        for (const [line, testIds] of this._testIdsPerLine) {
            const coveragePercent = (testIds.length * 100 / this._tests.size).toFixed(0);
            this._editor.addLineClass(line - 1, 'background', 'coverage-' + coveragePercent);
        }
    }

    /**
     * Highlights coverage for alternative (probably enemy-written) tests on the given CodeMirror instance.
     */
    highlightAlternativeCoverage () {
        for (const [line, testIds] of this._alternativeTestIdsPerLine) {
            const coveragePercent = (testIds.length * 100 / this._alternativeTests.size).toFixed(0);
            this._editor.addLineClass(line - 1, 'background', 'coverage-' + coveragePercent);
        }
    }

    /**
     * Displays mutant icons on the given CodeMirror instance.
     */
    highlightMutants () {
        for (const [line, mutantIds] of this._mutantIdsPerLine) {
            const mutantsOnLine = mutantIds.map(id => this._mutants.get(id));
            const marker = this._createMutantIcons(line, mutantsOnLine);
            this._addPopoverTriggerToMutantIcons(marker);
            this._editor.setGutterMarker(line - 1, 'CodeMirror-mutantIcons', marker);
        }
    }

    /**
     * Completely removes the coverage highlighting on the given CodeMirror instance.
     * If any lines were added and deleted in the editor, then clearing and re-applying the highlighting will
     * place the highlighting onto the wrong lines.
     */
    clearCoverage () {
        this._editor.eachLine(line => {
            const bgClasses = line.bgClass ?? '';

            /* Match all coverage classes. */
            for (const match of (bgClasses.match(/coverage-\d+/g) ?? [])) {
                this._editor.removeLineClass(line, 'background', match);
            }
        });
    }

    /**
     * Completely removes the mutant icons on the given CodeMirror instance.
     * If any lines were added and deleted in the editor, then clearing and re-applying the highlighting will
     * place the highlighting onto the wrong lines.
     */
    clearMutants () {
        this._editor.clearGutter('CodeMirror-mutantIcons');
    }
}


export default GameHighlighting;
