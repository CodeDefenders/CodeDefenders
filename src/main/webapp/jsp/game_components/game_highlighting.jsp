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
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="java.util.List" %>
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
        const gh_data = JSON.parse(`<%=ghString%>`);
        const mutantIdsPerLine = new Map(gh_data.mutantIdsPerLine);
        const testIdsPerLine = new Map(gh_data.testIdsPerLine);
        const mutants = new Map(gh_data.mutants);
        const tests = new Map(gh_data.tests);

        const MutantStatus = {
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

        const MutantIcons = {
            ALIVE: '<%=request.getContextPath()%>/images/mutant.png',
            KILLED: '<%=request.getContextPath()%>/images/mutantKilled.png',
            FLAGGED: '<%=request.getContextPath()%>/images/mutantFlagged.png',
            EQUIVALENT: '<%=request.getContextPath()%>/images/mutantEquiv.png'
        };

        /*
         * We use timeouts to hide the pop-over after the icon or pop-over is not hovered for a certain time.
         * These timeouts are saved, so they can be cleared when pop-overs are forced to hide.
         */
        let timeouts = [];
        const addTimeout = (callback, time) => timeouts.push(setTimeout(callback, time));
        const clearTimeouts = () => { timeouts = timeouts.filter(clearTimeout); };

        const createMutantIcons = function (line, mutantsOnLine) {
            const sortedMutants = {};
            for (const mutantStatus in MutantStatus) {
                const mutantsInCategory = mutantsOnLine.filter(m => m.status === mutantStatus);
                if (mutantsInCategory.length > 0) {
                    sortedMutants[mutantStatus] = mutantsInCategory;
                }
            }

            let icons = '';
            for (const mutantStatus in sortedMutants) {
                const numMutants = sortedMutants[mutantStatus].length;
                /* Must be in one line, because it's in a pre. */
                icons += '<div class="mutant-icon" mutant-status="' + mutantStatus + '"  mutant-line="' + line +'"><img src="' + MutantIcons[mutantStatus] + '" class="mutant-icon-image"><span class="mutant-icon-count">' + numMutants + '</span></div>';
            }

            const marker = document.createElement('div');
            marker.classList.add('mutant-icons');
            marker.innerHTML = icons;

            $(marker).find('.mutant-icon').popover({
                /* Append to body instead of the element itself, so that the icons don't overlap modals. */
                container: document.body,
                placement: 'right',
                trigger: 'manual',
                html: true,
                title: getPopoverTitle,
                content: getPopoverContent
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

            return marker;
        };

        const getPopoverTitle = function () {
            const status = $(this).attr('mutant-status');
            const line = $(this).attr('mutant-line');

            return '<img src="' + MutantIcons[status] + '" class="mutant-icon-image"> '
                + MutantNames[status] + ' (Line ' + line + ')';
        };

        const getPopoverContent = function () {
            const line = Number($(this).attr('mutant-line'));
            const status = $(this).attr('mutant-status');
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

            let button = '';

            <% if (showEquivalenceButton) { %>
                if (status === MutantStatus.alive <%=markUncoveredEquivalent ? "" : "&& coverageOnLine.get(line)"%>) {
                    <% if (gameType.equals("PARTY")) { %>
                        button =
                           `<form onsubmit="if (window.confirm('This will mark all player-created mutants on line ` + line + ` as equivalent. Are you sure?')) { window.location.href = \'multiplayer/play?equivLine=` + line + `\'; } return false;">
                                <button class="btn btn-danger btn-sm" style="width: 100%;">
                                    <img src="<%=request.getContextPath()%>/images/flag.png" class="mutant-icon-image"/> Claim Equivalent
                                </button>
                            </form>`;
                    <% } else if (gameType.equals("DUEL")) { %>
                        button =
                            `<form id="equiv" action="duelgame" method="post" onsubmit="return window.confirm('This will mark mutant ` + mutantsOnLine[0].id + ` as equivalent. Are you sure?')">
                                <input type="hidden" name="formType" value="claimEquivalent">
                                <input type="hidden" name="mutantId" value="` + mutantsOnLine[0].id + `">
                                <button class="btn btn-danger btn-sm" style="width: 100%;">
                                    <img src="<%=request.getContextPath()%>/images/flag.png" class="mutant-icon-image"/> Claim Equivalent
                                </button>
                             </form>`;
                    <% } %>
                }
            <% } %>

            const content = document.createElement('div');
            content.classList.add('mutant-popover-body');
            content.innerHTML = table + button;

            return content;
        };

        const highlightCoverage = function (codeMirror) {
            for (const [line, testIds] of testIdsPerLine) {
                const coveragePercent = (testIds.length * 100 / tests.size).toFixed(0);
                codeMirror.addLineClass(line - 1, 'background', 'coverage-' + coveragePercent);
            }
        };

        const highlightMutants = function (codeMirror) {
            for (const [line, mutantIds] of mutantIdsPerLine) {
                const mutantsOnLine = mutantIds.map(id => mutants.get(id));
                codeMirror.setGutterMarker(line - 1, 'CodeMirror-mutantIcons', createMutantIcons(line, mutantsOnLine));
            }
        };

        const codeMirror = $('<%=codeDivSelector%>').find('.CodeMirror')[0].CodeMirror;
        highlightCoverage(codeMirror);
        highlightMutants(codeMirror);
    }());
</script>

<% } %>
