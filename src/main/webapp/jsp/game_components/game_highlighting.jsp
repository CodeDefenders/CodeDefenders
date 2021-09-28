<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.GameMode" %>

<%--
    Adds highlighting of coverage (green lines) and mutants (gutter icons) to a CodeMirror editor.

    The game highlighting uses these HTML elements:
        - Mutant Icons:
            <div class="gh-mutant-icons">
                <div class="gh-mutant-icon">
                    ::after
                </div>
            </div>

    The CSS is located in codemirror_customize.css.
--%>

<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {
        const enableFlagging = Boolean(${gameHighlighting.enableFlagging});

        /* Game highlighting data. */
        const gh_data = JSON.parse('${gameHighlighting.JSON}');
        const mutantIdsPerLine = new Map(gh_data.mutantIdsPerLine);
        const testIdsPerLine = new Map(gh_data.testIdsPerLine);
        const mutants = new Map(gh_data.mutants);
        const tests = new Map(gh_data.tests);
        const alternativeTestIdsPerLine = new Map(gh_data.alternativeTestIdsPerLine);
        const alternativeTests = new Map(gh_data.alternativeTests);

        const MutantStatuses = {
            ALIVE: 'ALIVE',
            KILLED: 'KILLED',
            FLAGGED: 'FLAGGED',
            EQUIVALENT: 'EQUIVALENT'
        };

        const MutantNames = {
            ALIVE: 'Alive Mutants',
            KILLED: 'Killed Mutants',
            FLAGGED: 'Flagged Mutants',
            EQUIVALENT: 'Equivalent Mutants'
        };

        const IconClasses = {
            ALIVE: ['mutantCUTImage', 'mutantImageAlive'],
            KILLED: ['mutantCUTImage', 'mutantImageKilled'],
            FLAGGED: ['mutantCUTImage', 'mutantImageFlagged'],
            EQUIVALENT: ['mutantCUTImage', 'mutantImageEquiv'],
            FLAG: ['mutantCUTImage', 'mutantImageFlagAction']
        };

        /*
         * We use timeouts to hide the pop-over after the icon or pop-over is not hovered for a certain time.
         * These timeouts are saved, so they can be cleared when pop-overs are forced to hide.
         */
        let timeouts = [];
        const addTimeout = function (callback, time) {
            timeouts.push(setTimeout(callback, time));
        };
        const clearTimeouts = function () {
            timeouts = timeouts.filter(clearTimeout);
        };

        /**
         * Creates the HTML element that displays the mutant icons for one line.
         * @param {number} line The line number (starting at 1).
         * @param {List<object>} mutantsOnLine The mutants that modify the line.
         * @return {HTMLElement} The mutant icons.
         */
        const createMutantIcons = function (line, mutantsOnLine) {

            /* Split the mutants list by the mutant status.
             * {ALIVE: [...], KILLED: [...], ...} */
            const sortedMutants = {};
            for (const mutantStatus in MutantStatuses) {
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
                mutantIcon.classList.add(...IconClasses[mutantStatus]);

                /* Dataset entries are "converted" to kebap-case DOM attributes. */
                mutantIcon.dataset.mutantStatus = mutantStatus;
                mutantIcon.dataset.line = line;
                mutantIcon.dataset.count = sortedMutants[mutantStatus].length;
                mutantIcon.dataset.canClaim = mutantsOnLine.some((element) => element.canClaim);

                mutantIcons.appendChild(mutantIcon);
            }

            return mutantIcons;
        };


        /**
         * Adds a trigger to the mutant icons, which opens the popover and closes all other open popovers.
         * @param {HTMLElement} mutantIcons The mutant icons.
         */
        const addPopoverTriggerToMutantIcons = function (mutantIcons) {
            for (const mutantIcon of mutantIcons.querySelectorAll('.gh-mutant-icon')) {

                mutantIcon.popover = new bootstrap.Popover(mutantIcon, {
                    /* Append to body instead of the element itself, so that the icons don't overlap modals. */
                    container: document.body,
                    placement: 'right',
                    trigger: 'manual',
                    html: true,
                    customClass: 'popover-fluid',
                    title: createPopoverTitle,
                    content: createPopoverContent
                });

                /* Activate popovers manually so we can make them stay as long as they are hovered. */
                mutantIcon.addEventListener('mouseenter', function (event) {
                    clearTimeouts();

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
                            clearTimeouts();
                        });
                        popoverElement.addEventListener('mouseleave', function (event) {
                            addTimeout(() => {
                                mutantIcon.popover.hide();
                            }, 500);
                        });
                    }
                });
                mutantIcon.addEventListener('mouseleave', function (event) {
                    addTimeout(() => {
                        mutantIcon.popover.hide();
                    }, 500);
                });
            }
        };

        /**
         * Creates the title for a popover.
         * "this" will point to the ".gh-mutant-icon" div.
         * @returns {string} The popover title.
         */
        const createPopoverTitle = function () {
            const status = this.dataset.mutantStatus;
            const line = Number(this.dataset.line);

            return '' +
                `<div class="d-flex align-items-center gap-2">
                    <div class="\${IconClasses[status].join(' ')}"></div>
                    \${MutantNames[status]} (Line \${line})
                </div>`;
        };

        /**
         * Creates the body for a popover.
         * "this" will point to the ".gh-mutant-icon" div.
         * @returns {HTMLElement} The popover body.
         */
        const createPopoverContent = function () {
            const status = this.dataset.mutantStatus;
            const canClaim = this.dataset.canClaim;
            const line = Number(this.dataset.line);

            const mutantsOnLine = mutantIdsPerLine.get(line)
                .map(id => mutants.get(id))
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
                        <td>\${mutant.creatorName}</td>
                        <td class="text-end">\${mutant.id}</td>
                        <td class="text-end">\${mutant.score}</td>
                        <td class="text-end">\${mutant.lines}</td>
                    </tr>`
                );
            }

            const table =
                `<div>
                    <table class="table table-sm table-no-last-border m-0">
                        \${head}
                        <tbody>
                             \${rows.join('\n')}
                        </tbody>
                     </table>
                </div>`;

            /* Create the button if it is supposed to be shown. */
            let button = '';
            if (enableFlagging
                && status === MutantStatuses.ALIVE
                && (canClaim === "true")
                && testIdsPerLine.get(line)) {
                button = createEquivalenceButton(line);
            }

            const content = document.createElement('div');
            content.classList.add('mutant-popover-body');
            content.innerHTML = table + button;

            return content;
        };

        /**
         * Creates the button with which to flag mutants as equivalent.
         * @param line The line number.
         * @return {string} The equivalence button.
         */
        const createEquivalenceButton = function (line) {
            return '' +
                `<form class="mt-3" id="equiv" action="<%=request.getContextPath() + Paths.EQUIVALENCE_DUELS_GAME%>" method="post"
                    onsubmit="return window.confirm('This will mark all player-created mutants on line \${line} as equivalent. Are you sure?')">
                    <input type="hidden" name="formType" value="claimEquivalent">
                    <input type="hidden" name="equivLines" value="\${line}">
                    <input type="hidden" name="gameId" value="${gameHighlighting.gameId}">
                    <button class="btn btn-danger btn-sm w-100 d-flex justify-content-center align-items-center gap-2">
                        <div class="\${IconClasses.FLAG.join(' ')}"></div>
                        <span>Claim Equivalent</span>
                    </button>
                </form>`;
        };

        /**
         * Highlights coverage on the given CodeMirror instance.
         * @param {object} codeMirror The CodeMirror instance.
         */
        const highlightCoverage = function (codeMirror) {
            for (const [line, testIds] of testIdsPerLine) {
                const coveragePercent = (testIds.length * 100 / tests.size).toFixed(0);
                codeMirror.addLineClass(line - 1, 'background', 'coverage-' + coveragePercent);
            }
        };

        const highlightAlternativeCoverage = function (codeMirror) {
            for (const [line, testIds] of alternativeTestIdsPerLine) {
                const coveragePercent = (testIds.length * 100 / alternativeTests.size).toFixed(0);
                codeMirror.addLineClass(line - 1, 'background', 'coverage-' + coveragePercent);
            }
        };

        /**
         * Displays mutant icons on the given CodeMirror instance.
         * @param {object} codeMirror The CodeMirror instance.
         */
        const highlightMutants = function (codeMirror) {
            for (const [line, mutantIds] of mutantIdsPerLine) {
                const mutantsOnLine = mutantIds.map(id => mutants.get(id));
                const marker = createMutantIcons(line, mutantsOnLine);
                addPopoverTriggerToMutantIcons(marker);
                codeMirror.setGutterMarker(line - 1, 'CodeMirror-mutantIcons', marker);
            }
        };

        /**
         * Completely removes the coverage highlighting on the given CodeMirror instance.
         * If any lines were added and deleted in the editor, then clearing and re-applying the highlighting will
         * place the highlighting onto the wrong lines.
         * @param {object} codeMirror The CodeMirror instance.
         */
        const clearCoverage = function (codeMirror) {
            codeMirror.eachLine(line => {
                if (line.bgClass) {
                    /* Match all coverage classes. */
                    const coverageMatches = line.bgClass.match(/coverage-\d+/g);
                    if (coverageMatches !== null) {
                        for (const match of coverageMatches) {
                            codeMirror.removeLineClass(line, 'background', match);
                        }
                    }
                }
            });
        };

        <%--
        /**
         * Completely removes the mutant icons on the given CodeMirror instance.
         * If any lines were added and deleted in the editor, then clearing and re-applying the highlighting will
         * place the highlighting onto the wrong lines.
         * @param {object} codeMirror The CodeMirror instance.
         */
        const clearMutants = function (codeMirror) {
            codeMirror.clearGutter('CodeMirror-mutantIcons');
        };
        --%>

        /**
         * Hides the mutant icons on the given CodeMirror instance.
         * The mutant icons are only hidden, and not cleared. They can be shown hidden and shown again correctly even if
         * lines were added or deleted in the editor.
         * @param {object} codeMirror The CodeMirror instance.
         */
        const hideMutants = function (codeMirror) {
            $(codeMirror.getWrapperElement()).find('.gh-mutant-icons').hide();
        };

        /**
         * Shows the mutant icons on the given CodeMirror instance.
         * @param {object} codeMirror The CodeMirror instance.
         */
        const showMutants = function (codeMirror) {
            $(codeMirror.getWrapperElement()).find('.gh-mutant-icons').show();
        };

        const codeMirror = $('${gameHighlighting.codeDivSelector}').find('.CodeMirror')[0].CodeMirror;
        codeMirror.highlightCoverage = function () { highlightCoverage(this) };
        codeMirror.highlightAlternativeCoverage = function () { highlightAlternativeCoverage(this) };
        codeMirror.highlightMutants = function () { highlightMutants(this) };
        codeMirror.clearCoverage = function () { clearCoverage(this) };
        <%--codeMirror.clearMutants = function () { clearMutants(this) };--%>
        codeMirror.hideMutants = function () { hideMutants(this) };
        codeMirror.showMutants = function () { showMutants(this) };
        codeMirror.highlightCoverage();
        codeMirror.highlightMutants();
    })();
</script>

