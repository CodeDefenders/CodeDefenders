<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ page import="org.codedefenders.game.puzzle.Puzzle" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.GameMode" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.puzzle.PuzzleType" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<%
    Puzzle puzzle = (Puzzle) request.getAttribute("puzzle");
    @SuppressWarnings("unchecked")
    List<Mutant> mutants = (List<Mutant>) request.getAttribute("mutants");
    @SuppressWarnings("unchecked")
    List<Test> tests = (List<Test>) request.getAttribute("tests");
    mutantExplanation.setCodeValidatorLevel(puzzle.getMutantValidatorLevel());
    pageContext.setAttribute("puzzle", puzzle);
%>

<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>
<%
    gameHighlighting.setGameData(mutants, tests);
    gameHighlighting.setFlaggingData(GameMode.PUZZLE, -1);
    gameHighlighting.setEnableFlagging(false);
%>

<%--@elvariable id="puzzleTitle" type="String"--%>
<%--@elvariable id="puzzleDescription" type="String"--%>

<p:base_page title="${i18n.tr('Puzzle Preview')}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <style>
            .modal-backdrop {
                opacity: 0.2 !important;
            }
            .game-component-header {
                margin-top: 0 !important;
            }
        </style>

        <div class="d-flex flex-column gap-4 p-4">
            <div>
                <div class="game-component-header">
                    <h3>${i18n.tr('Description')}</h3>
                </div>
                <div>
                    <c:choose>
                        <c:when test="${puzzleDescription == null || puzzleDescription.blank}">
                            (${i18n.tr('no description')})
                        </c:when>
                        <c:otherwise>
                            ${puzzleDescription}
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <t:mutant_accordion/>

            <div id="tests-div">
                <div class="game-component-header"><h3>${i18n.tr('JUnit Tests')}</h3></div>
                <t:test_accordion/>
            </div>

            <div>
                <div class="game-component-header"><h3>${i18n.tr('Class Under Test')}</h3></div>
                <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
                <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
            </div>

            <div>
                <div class="game-component-header">
                    <h3>${i18n.tr('Properties')}</h3>
                </div>
                <table class="table table-bordered">
                    <tbody>
                        <tr>
                            <td>${i18n.tr('Title')}</td>
                            <td>${puzzleTitle}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Description')}</td>
                            <td>${puzzleDescription}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Type')}</td>
                            <td>${puzzle.type}</td>
                        </tr>
                        <tr>
                            <td class="pe-3">${i18n.tr('Mutant Equivalent')}</td>
                            <c:choose>
                                <c:when test="${puzzle.type == PuzzleType.EQUIVALENCE}">
                                    <td>${puzzle.equivalent ? i18n.tr('yes') : i18n.tr('no')}</td>
                                </c:when>
                                <c:otherwise>
                                    <td>${i18n.tr('N/A')}</td>
                                </c:otherwise>
                            </c:choose>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Level')}</td>
                            <td>${i18n.tr(puzzle.level.formattedString)}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Editable lines start')}</td>
                            <td>${puzzle.editableLinesStart != null ? puzzle.editableLinesStart : i18n.tr('N/A')}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Editable lines end')}</td>
                            <td>${puzzle.editableLinesEnd != null ? puzzle.editableLinesEnd : i18n.tr('N/A')}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Max. assertions per test')}</td>
                            <td>${puzzle.maxAssertionsPerTest}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Mutant validator level')}</td>
                            <td>${puzzle.mutantValidatorLevel}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Puzzle ID')}</td>
                            <td>${puzzle.puzzleId}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Class ID')}</td>
                            <td>${puzzle.classId}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Chapter ID')}</td>
                            <td>${puzzle.chapterId}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr('Position')}</td>
                            <td>${puzzle.position}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <script type="module">
            import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';

            const startLine = ${puzzle.editableLinesStart == null ? 'null' : puzzle.editableLinesStart};
            const endLine = ${puzzle.editableLinesEnd == null ? 'null' : puzzle.editableLinesEnd};

            const classViewer = await objects.await('classViewer');
            const editor = classViewer.editor;
            const lineCount = editor.lineCount();

            function greyOutReadOnlyLines () {
                if (startLine != null) {
                    for (let lineNum = 0; lineNum < startLine - 1; lineNum++) {
                        editor.addLineClass(lineNum, 'text', 'readonly-line');
                    }
                }

                if (endLine != null) {
                    for (let lineNum = endLine; lineNum < lineCount; lineNum++) {
                        editor.addLineClass(lineNum, 'text', 'readonly-line');
                    }
                }
            }

            function createMarker(iconClass, title) {
                const marker = document.createElement('div');
                marker.style.color = "#002cae";
                marker.classList.add('text-end');
                marker.title = title;

                const icon = document.createElement('i');
                icon.classList.add('marker', 'fa', iconClass);
                marker.appendChild(icon);

                return marker;
            }

            greyOutReadOnlyLines();
            if (startLine !== null && startLine >= 2) {
                // one line before startLine (1-indexed)
                editor.setGutterMarker(startLine - 2, 'CodeMirror-linenumbers',
                    createMarker('fa-arrow-down', "${i18n.tr('Editable lines start below')}"));
            }
            if (startLine !== null && endLine <= lineCount - 1) {
                // one line after endLine (1-indexed)
                const marker = createMarker();
                marker.querySelector('.fa').classList.add('fa-arrow-down');
                marker.title = "${i18n.tr('Editable lines start below')}";
                editor.setGutterMarker(endLine, 'CodeMirror-linenumbers',
                    createMarker('fa-arrow-up', "${i18n.tr('Editable lines end above')}"));
            }
        </script>
    </jsp:body>
</p:base_page>
