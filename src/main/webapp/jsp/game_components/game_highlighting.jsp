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
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Mutant.Equivalence" %>

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

<%--
    TODO This implementation not very pretty and will be refactored in game-highlighting-rework-2.
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
    /* Map each line to the tests that cover it. */
    Map<Integer, List<Test>> testsOnLine = new HashMap<>();
    for (Test test : testsTODORENAME) {
        for (Integer line : test.getLineCoverage().getLinesCovered().stream().distinct().collect(Collectors.toList())) {
            List<Test> list = testsOnLine.computeIfAbsent(line, key -> new ArrayList<>());
            list.add(test);
        }
    }

    /* Map each line to the mutants on it. */
    Map<Integer, List<Mutant>> mutantsOnLine = new HashMap<>();
    for (Mutant mutant : mutantsTODORENAME) {
        for (Integer line : mutant.getLines()) {
            List<Mutant> list = mutantsOnLine.computeIfAbsent(line, key -> new ArrayList<>());
            list.add(mutant);
        }
    }

    int numTests = testsTODORENAME.size();

    /* Build a javascript map that maps lines to the percentage of tests that cover it. */
    StringBuilder jsTests = new StringBuilder();
    jsTests.append("["); // outer list
    for (Map.Entry<Integer, List<Test>> entry : testsOnLine.entrySet()) {
        jsTests.append("[") // element
               .append(entry.getKey())
               .append(",")
               .append((entry.getValue().size() * 100) / numTests)
               .append("],"); // element
    }
    jsTests.append("]"); // outer list

    /*
     * Build a javascript map that maps lines to the mutants on the line.
     * Mutants are represented as
     * {
     *    id: number,
     *    status: 'alive' | 'killed' | 'flagged' | 'equivalent',
     *    creatorName: string,
     *    points: number
     * }
     */
    StringBuilder jsMutants = new StringBuilder();
    jsMutants.append("["); // outer list
    for (Map.Entry<Integer, List<Mutant>> entry : mutantsOnLine.entrySet()) {
        jsMutants.append("[") // element
                 .append(entry.getKey())
                 .append(",");

        jsMutants.append("["); // inner list
        for (Mutant mutant : entry.getValue()) {
            Mutant.Equivalence eq = mutant.getEquivalent();
            String status;
            if (eq == Equivalence.DECLARED_YES || eq == Equivalence.ASSUMED_YES) {
                status = "equivalent";
            } else if (eq == Equivalence.PENDING_TEST) {
                status = "flagged";
            } else if (mutant.isAlive()) {
                status = "alive";
            } else {
                status = "killed";
            }

            jsMutants.append("{")
                     .append("id:")
                     .append(mutant.getId())
                     .append(",status:")
                     .append("'")
                     .append(status)
                     .append("',")
                     .append("creatorName:")
                     .append("'")
                     .append(mutant.getCreatorName())
                     .append("',")
                     .append("points:")
                     .append(mutant.getScore())
                     .append("},");
        }
        jsMutants.append("]"); // inner list

        jsMutants.append("],"); // element
    }
    jsMutants.append("]"); // outer list
%>

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {
        const mutantsOnLine = new Map(<%=jsMutants%>);
        const coverageOnLine = new Map(<%=jsTests%>);

        const MutantStatus = {
            alive: 'alive',
            killed: 'killed',
            flagged: 'flagged',
            equivalent: 'equivalent'
        };

        const MutantNames = {
            alive: 'Alive Mutants',
            killed: 'Killed Mutants',
            flagged: 'Flagged Mutants',
            equivalent: 'Equivalent Mutants'
        };

        const MutantIcons = {
            alive: '<%=request.getContextPath()%>/images/mutant.png',
            killed: '<%=request.getContextPath()%>/images/mutantKilled.png',
            flagged: '<%=request.getContextPath()%>/images/mutantFlagged.png',
            equivalent: '<%=request.getContextPath()%>/images/mutantEquiv.png'
        };

        /*
         * We use timeouts to hide the pop-over after the icon or pop-over is not hovered for a certain time.
         * These timeouts are saved, so they can be cleared when pop-overs are forced to hide.
         */
        let timeouts = [];
        const addTimeout = (callback, time) => timeouts.push(setTimeout(callback, time));
        const clearTimeouts = () => { timeouts = timeouts.filter(clearTimeout); };

        const createMutantIcons = function (line, mutants) {
            const sortedMutants = {};
            for (const mutantStatus in MutantStatus) {
                const mutantsInCategory = mutants.filter(m => m.status === mutantStatus);
                if (mutantsInCategory.length) {
                    sortedMutants[mutantStatus] = mutantsInCategory;
                }
            }

            let icons = '';
            for (const mutantStatus in sortedMutants) {
                const numMutants = sortedMutants[mutantStatus].length;
                /* Must be in one line, because it's in a pre. */
                icons += '<div class="mutant-icon" mutant-status="' + mutantStatus + '"  mutant-line="' + line + '"><img src="' + MutantIcons[mutantStatus] + '" class="mutant-icon-image"><span class="mutant-icon-count">' + numMutants + '</span></div>';
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
            const mutants = mutantsOnLine.get(line).filter(mutant => mutant.status === status);

            const head =
                `<thead>
                     <tr>
                         <td>Creator</td>
                         <td align="right">ID</td>
                         <td align="right">Points</td>
                     </tr>
                 </thead>`;

            const rows = [];
            for (const mutant of mutants) {
                rows.push(
                    `<tr>
                         <td>` + mutant.creatorName + `</td>
                         <td align="right">` + mutant.id + `</td>
                         <td align="right">` + mutant.points + `</td>
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
                            `<form id="equiv" action="duelgame" method="post" onsubmit="return window.confirm('This will mark mutant ` + mutants[0].id + ` as equivalent. Are you sure?')">
                                <input type="hidden" name="formType" value="claimEquivalent">
                                <input type="hidden" name="mutantId" value="` + mutants[0].id + `">
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
            for (const [line, percentage] of coverageOnLine) {
                codeMirror.addLineClass(line - 1, 'background', 'coverage-' + percentage);
            }
        };

        const highlightMutants = function (codeMirror) {
            for (const [line, mutants] of mutantsOnLine) {
                codeMirror.setGutterMarker(line - 1, 'CodeMirror-mutantIcons', createMutantIcons(line, mutants));
            }
        };

        const codeMirror = $('<%=codeDivSelector%>').find('.CodeMirror')[0].CodeMirror;
        highlightCoverage(codeMirror);
        highlightMutants(codeMirror);
    }());
</script>

<% } %>
