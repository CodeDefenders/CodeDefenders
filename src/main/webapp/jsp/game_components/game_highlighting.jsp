<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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

<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="com.google.gson.GsonBuilder" %>

<%--
    Adds highlighting of coverage (green lines) and mutants (gutter icons) to a CodeMirror editor.

    @param String codeDivSelector
        Selector for the div the CodeMirror container is in. Should only contain one CodeMirror instance.
    @param Boolean showEquivalenceButtton
        Show a button to flag a selected mutant as equivalent.
    @param List<Test> tests
        The list of (valid) tests in the game.
    @param List<Mutant> mutants
        The list of (valid) mutants in the game.
--%>

<%--
    The game highlighting uses these HTML elements:
        - Mutant Icons:
            <div class="mutant-icons">
                <div class="mutant-icon"
                    <img class="mutant-icon-image">
                    <span class="mutant-icon-count">
                </div>
            </div>
        - Mutant Pop-overs:
            <div class="mutant-popover-body"/>

    The CSS is located in game_highlighting.css.
--%>

<% { %>

<%
    String codeDivSelector = (String) request.getAttribute("codeDivSelector");
    List<Test> testsTODORENAME = (List<Test>) request.getAttribute("tests");
    List<Mutant> mutantsTODORENAME = (List<Mutant>) request.getAttribute("mutants") ;
    Boolean showEquivalenceButton = (Boolean) request.getAttribute("showEquivalenceButton");
    Boolean markUncoveredEquivalent = (Boolean) request.getAttribute("markUncoveredEquivalent");
    String gameType = (String) request.getAttribute("gameType");
%>

<%
    GameHighlightingDTO gh = new GameHighlightingDTO(mutantsTODORENAME, testsTODORENAME);
    Gson gson = new GsonBuilder().registerTypeAdapter(Map.class, new GameHighlightingDTO.MapSerializer()).create();
    String ghString = gson.toJson(gh);
%>

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {

        /* Game highlighting data. */
        const gh_data = JSON.parse(`<%=ghString%>`);
        const mutantIdsPerLine = new Map(gh_data.mutantIdsPerLine);
        const testIdsPerLine = new Map(gh_data.testIdsPerLine);
        const mutants = new Map(gh_data.mutants);
        const tests = new Map(gh_data.tests);

        /* Game highlighting settings. */
        const showEquivalenceButton = Boolean(<%=showEquivalenceButton%>);
        const markUncoveredEquivalent = Boolean(<%=markUncoveredEquivalent%>);
        const gameType = '<%=gameType%>';

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

        const Icons = {
            ALIVE: '<%=request.getContextPath()%>/images/mutant.png',
            KILLED: '<%=request.getContextPath()%>/images/mutantKilled.png',
            FLAGGED: '<%=request.getContextPath()%>/images/mutantFlagged.png',
            EQUIVALENT: '<%=request.getContextPath()%>/images/mutantEquiv.png',
            FLAG: '<%=request.getContextPath()%>/images/flag.png'
        };

        const GameTypes = {
            PARTY: 'PARTY',
            DUEL: 'DUEL'
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
            let icons = [];
            for (const mutantStatus in sortedMutants) {
                /* Must be in one line, because it's in a pre.
                   Can't use template strings because JSP's EL syntax overrides it. */
                icons.push('<div class="mutant-icon" mutant-status="' + mutantStatus + '"  mutant-line="' + line + '">' +
                               '<img class="mutant-icon-image" src="' + Icons[mutantStatus] + '">' +
                               '<span class="mutant-icon-count">' + sortedMutants[mutantStatus].length + '</span>' +
                           '</div>');
            }
            const mutantIcons = document.createElement('div');
            mutantIcons.classList.add('mutant-icons');
            mutantIcons.innerHTML = icons.join("");

            return mutantIcons;
        };


        /**
         * Adds a trigger to the mutant icons, which opens the popover and closes all other open popovers.
         * @param {HTMLElement} mutantIcons The mutant icons.
         */
        const addPopoverTriggerToMutantIcons = function (mutantIcons) {
            $(mutantIcons).find('.mutant-icon').popover({
                /* Append to body instead of the element itself, so that the icons don't overlap modals. */
                container: document.body,
                placement: 'right',
                trigger: 'manual',
                html: true,
                title: createPopoverTitle,
                content: createPopoverContent
            }).on('mouseenter', function () {
                clearTimeouts();

                /* Hide all other pop-overs. */
                $('.mutant-icon').not(this).popover('hide');

                /* Show this pop-over. */
                $(this).popover('show');

                /* Clear timeouts when pop-over is hovered so it won't be hidden by the timeout. */
                $('.popover').on('mouseenter', () => {
                    clearTimeouts();
                }).on('mouseleave', () => {
                    addTimeout(() => {
                        $(this).popover('hide');
                    }, 500);
                });
            }).on('mouseleave', function () {
                addTimeout(() => {
                    $(this).popover('hide');
                }, 500);
            });
        };

        /**
         * Creates the title for a popover.
         * "this" will point to the ".mutant-icon" div.
         * @returns {string} The popover title.
         */
        const createPopoverTitle = function () {
            const status = $(this).attr('mutant-status');
            const line = Number($(this).attr('mutant-line'));

            return '<img src="' + Icons[status] + '" class="mutant-icon-image"> '
                + MutantNames[status] + ' (Line ' + line + ')';
        };

        /**
         * Creates the body for a popover.
         * "this" will point to the ".mutant-icon" div.
         * @returns {HTMLElement} The popover body.
         */
        const createPopoverContent = function () {
            const status = $(this).attr('mutant-status');
            const line = Number($(this).attr('mutant-line'));

            const mutantsOnLine = mutantIdsPerLine.get(line)
                .map(id => mutants.get(id))
                .filter(mutant => mutant.status === status);

            const head =
                `<thead>
                     <tr>
                         <td>Creator</td>
                         <td align="right">ID</td>
                         <td align="right">Score</td>
                     </tr>
                 </thead>`;

            const rows = [];
            for (const mutant of mutantsOnLine) {
                rows.push(
                    `<tr>
                         <td>` + mutant.creatorName + `</td>
                         <td align="right">` + mutant.id + `</td>
                         <td align="right">` + mutant.score + `</td>
                     </tr>`
                );
            }

            const table =
                `<table class="table table-condensed">`
                     + head +
                    `<tbody>`
                         + rows.join('\n') +
                    `</tbody>
                 </table>`;

            /* Create the button if it is supposed to be shown. */
            let button = '';
            if (showEquivalenceButton
                && status === MutantStatuses.ALIVE
                && (markUncoveredEquivalent || testIdsPerLine.get(line))) {
                button = createEquivalenceButton(line, mutantsOnLine);
            }

            const content = document.createElement('div');
            content.classList.add('mutant-popover-body');
            content.innerHTML = table + button;

            return content;
        };

        /**
         * Creates the button with which to flag mutants as equivalent.
         * @param line The line number.
         * @param mutantsOnLine The muitants on the line.
         * @return {string} The equivalence button.
         */
        const createEquivalenceButton = function (line, mutantsOnLine) {
            if (gameType === GameTypes.PARTY) {
                return `<form onsubmit="if (window.confirm('This will mark all player-created mutants on line ` + line + ` as equivalent. Are you sure?')) { window.location.href = \'multiplayer/play?equivLine=` + line + `\'; } return false;">
                            <button class="btn btn-danger btn-sm" style="width: 100%;">
                                <img src="` + Icons.FLAG + `" class="mutant-icon-image"/> Claim Equivalent
                            </button>
                        </form>`;
            } else if (gameType === GameTypes.DUEL) {
                return `<form id="equiv" action="duelgame" method="post" onsubmit="return window.confirm('This will mark mutant ` + mutantsOnLine[0].id + ` as equivalent. Are you sure?')">
                            <input type="hidden" name="formType" value="claimEquivalent">
                            <input type="hidden" name="mutantId" value="` + mutantsOnLine[0].id + `">
                            <button class="btn btn-danger btn-sm" style="width: 100%;">
                                <img src="` + Icons.FLAG + `" class="mutant-icon-image"/> Claim Equivalent
                            </button>
                        </form>`;
            } else {
                console.error('Unknown game type for equivalence button: ' + gameType);
            }
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

        <%--
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
            $(codeMirror.getWrapperElement()).find('.mutant-icons').hide();
        };

        /**
         * Shows the mutant icons on the given CodeMirror instance.
         * @param {object} codeMirror The CodeMirror instance.
         */
        const showMutants = function (codeMirror) {
            $(codeMirror.getWrapperElement()).find('.mutant-icons').show();
        };

        const codeMirror = $('<%=codeDivSelector%>').find('.CodeMirror')[0].CodeMirror;
        codeMirror.highlightCoverage = function () { highlightCoverage(this) };
        codeMirror.highlightMutants = function () { highlightMutants(this) };
        codeMirror.clearCoverage = function () { clearCoverage(this) };
        codeMirror.clearMutants = function () { clearMutants(this) };
        codeMirror.hideMutants = function () { hideMutants(this) };
        codeMirror.showMutants = function () { showMutants(this) };
        codeMirror.highlightCoverage();
        codeMirror.highlightMutants();
    }());
</script>

<% } %>
