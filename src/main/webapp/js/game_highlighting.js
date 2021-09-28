/* Wrap in a function to avoid polluting the global scope. */
(function () {

class GameHighlighting {

    /**
     *
     * @param {object} data
     *      Given by [JSON.parse('${gameHighlighting.JSON}')]
     * @param {boolean} enableFlagging
     *      Given by [Boolean(${gameHighlighting.enableFlagging})]
     * @param {string} flaggingUrl
     *      Given by ['${pageContext.request.contextPath}${Paths.EQUIVALENCE_DUELS_GAME}']
     * @param {number} gameId
     *      Given by [${gameHighlighting.gameId}]
     */
    constructor(data, enableFlagging, flaggingUrl, gameId) {
        this.mutantIdsPerLine = new Map(data.mutantIdsPerLine);
        this.testIdsPerLine = new Map(data.testIdsPerLine);

        this.mutants = new Map(data.mutants);
        this.tests = new Map(data.tests);

        this.alternativeTests = new Map(data.alternativeTests);
        this.alternativeTestIdsPerLine = new Map(data.alternativeTestIdsPerLine);

        this.enableFlagging = enableFlagging;
        this.flaggingUrl = flaggingUrl;
        this.gameId = gameId;

        /** We use timeouts to hide the pop-over after the icon or pop-over is not hovered for a certain time.
         * These timeouts are saved, so they can be cleared when pop-overs are forced to hide. */
        this.popoverTimeouts = [];

        this.editor = null;
        if (CodeDefenders.objects.hasOwnProperty('classViewer')) {
            this.editor = CodeDefenders.objects.classViewer.editor;
        } else if (CodeDefenders.objects.hasOwnProperty('mutantEditor')) {
            this.editor = CodeDefenders.objects.mutantEditor.editor;
        }
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

    addTimeout (callback, time) {
        this.popoverTimeouts.push(setTimeout(callback, time));
    }

    clearTimeouts () {
        this.popoverTimeouts = this.popoverTimeouts.filter(clearTimeout);
    }

    /**
     * Creates the HTML element that displays the mutant icons for one line.
     * @param {number} line The line number (starting at 1).
     * @param {object[]} mutantsOnLine The mutants that modify the line.
     * @return {HTMLElement} The mutant icons.
     */
    createMutantIcons (line, mutantsOnLine) {

        /* Split the mutants list by the mutant status.
         * {ALIVE: [...], KILLED: [...], ...} */
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

            /* Dataset entries are "converted" to kebap-case DOM attributes. */
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
     */
    addPopoverTriggerToMutantIcons (mutantIcons) {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        for (const mutantIcon of mutantIcons.querySelectorAll('.gh-mutant-icon')) {

            mutantIcon.popover = new bootstrap.Popover(mutantIcon, {
                /* Append to body instead of the element itself, so that the icons don't overlap modals. */
                container: document.body,
                placement: 'right',
                trigger: 'manual',
                html: true,
                customClass: 'popover-fluid',
                title: this.createPopoverTitle.bind(this, mutantIcon),
                content: this.createPopoverContent.bind(this, mutantIcon)
            });

            /* Activate popovers manually so we can make them stay as long as they are hovered. */
            mutantIcon.addEventListener('mouseenter', function (event) {
                self.clearTimeouts();

                /* Hide all other pop-overs. */
                for (const otherMutantIcon of document.querySelectorAll('.gh-mutant-icon')) {
                    if (mutantIcon !== otherMutantIcon) {
                        otherMutantIcon.popover.hide();
                    }
                }

                mutantIcon.popover.show();

                /* Clear timeouts when pop-over is hovered so it won't be hidden by the timeout. */
                for (const popoverElement of document.getElementsByClassName('popover')) {
                    popoverElement.addEventListener('mouseenter', function (event) {
                        self.clearTimeouts();
                    });
                    popoverElement.addEventListener('mouseleave', function (event) {
                        self.addTimeout(() => {
                            mutantIcon.popover.hide();
                        }, 500);
                    });
                }
            });
            mutantIcon.addEventListener('mouseleave', function (event) {
                self.addTimeout(() => {
                    mutantIcon.popover.hide();
                }, 500);
            });
        }
    }

    /**
     * Creates the title for a popover.
     * @param {HTMLElement} mutantIcon The icon element.
     * @returns {string} The popover title.
     */
    createPopoverTitle (mutantIcon) {
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
     */
    createPopoverContent (mutantIcon) {
        const status = mutantIcon.dataset.mutantStatus;
        const canClaim = Boolean(JSON.parse(mutantIcon.dataset.canClaim));
        const line = Number(mutantIcon.dataset.line);

        const mutantsOnLine = this.mutantIdsPerLine.get(line)
                .map(id => this.mutants.get(id))
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
        if (this.enableFlagging
                && status === GameHighlighting.MutantStatuses.ALIVE
                && canClaim) {
            if (this.testIdsPerLine.get(line)) {
                button = this.createEquivalenceButton(line);
            } else {
                button = this.createUncoveredEquivalenceButton(line);
            }
        }

        const content = document.createElement('div');
        content.classList.add('mutant-popover-body');
        content.innerHTML = table + button;

        return content;
    }

    /**
     * Creates the button with which to flag mutants as equivalent.
     * @param line The line number.
     * @return {string} The equivalence button.
     */
    createEquivalenceButton (line) {
        return '' +
                `<form class="mt-3" id="equiv" action="${this.flaggingUrl}" method="post"
                    onsubmit="return window.confirm('This will mark all player-created mutants on line ${line} as equivalent. Are you sure?')">
                    <input type="hidden" name="formType" value="claimEquivalent">
                    <input type="hidden" name="equivLines" value="${line}">
                    <input type="hidden" name="gameId" value="${this.gameId}">
                    <button class="btn btn-danger btn-sm w-100 d-flex justify-content-center align-items-center gap-2">
                        <div class="${GameHighlighting.IconClasses.FLAG.join(' ')}"></div>
                        <span>Claim Equivalent</span>
                    </button>
                </form>`;
    }

    /**
     * Creates a disabled equivalence button with a tooltip explaining that the line needs to be covered first.
     * @param line The line number.
     * @return {string} The equivalence button.
     */
    createUncoveredEquivalenceButton (line) {
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
        for (const [line, testIds] of this.testIdsPerLine) {
            const coveragePercent = (testIds.length * 100 / this.tests.size).toFixed(0);
            this.editor.addLineClass(line - 1, 'background', 'coverage-' + coveragePercent);
        }
    }

    highlightAlternativeCoverage () {
        for (const [line, testIds] of this.alternativeTestIdsPerLine) {
            const coveragePercent = (testIds.length * 100 / this.alternativeTests.size).toFixed(0);
            this.editor.addLineClass(line - 1, 'background', 'coverage-' + coveragePercent);
        }
    }

    /**
     * Displays mutant icons on the given CodeMirror instance.
     */
    highlightMutants () {
        for (const [line, mutantIds] of this.mutantIdsPerLine) {
            const mutantsOnLine = mutantIds.map(id => this.mutants.get(id));
            const marker = this.createMutantIcons(line, mutantsOnLine);
            this.addPopoverTriggerToMutantIcons(marker);
            this.editor.setGutterMarker(line - 1, 'CodeMirror-mutantIcons', marker);
        }
    }

    /**
     * Completely removes the coverage highlighting on the given CodeMirror instance.
     * If any lines were added and deleted in the editor, then clearing and re-applying the highlighting will
     * place the highlighting onto the wrong lines.
     */
    clearCoverage () {
        this.editor.eachLine(line => {
            const bgClasses = line.bgClass ?? '';

            /* Match all coverage classes. */
            for (const match of (bgClasses.match(/coverage-\d+/g) ?? [])) {
                this.editor.removeLineClass(line, 'background', match);
            }
        });
    }

    /**
     * Completely removes the mutant icons on the given CodeMirror instance.
     * If any lines were added and deleted in the editor, then clearing and re-applying the highlighting will
     * place the highlighting onto the wrong lines.
     */
    clearMutants () {
        this.editor.clearGutter('CodeMirror-mutantIcons');
    }

    /**
     * Hides the mutant icons on the given CodeMirror instance.
     * The mutant icons are only hidden, and not cleared. They can be shown hidden and shown again correctly even if
     * lines were added or deleted in the editor.
     */
    hideMutants () {
        $(this.editor.getWrapperElement()).find('.gh-mutant-icons').hide();
    }

    /**
     * Shows the mutant icons on the given CodeMirror instance.
     */
    showMutants () {
        $(this.editor.getWrapperElement()).find('.gh-mutant-icons').show();
    }
}

CodeDefenders.classes.GameHighlighting = GameHighlighting;

})();
