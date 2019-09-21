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
<%@page import="org.codedefenders.database.UserDAO"%>
<%@page import="org.codedefenders.model.User"%>
<%@page import="org.codedefenders.game.Test"%>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameMode" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>

<%--
    Displays three tabs with a list of alive, killed and equivalent mutants respectively.

    @param Boolean markEquivalent
        Enable marking mutants as equivalent.
    @param Boolean viewDiff
        Enable viewing of the mutants diffs.
    @param List<Mutant> mutantsAlive
        The list of alive Mutants to display.
    @param List<Mutant> mutantsKilled
        The list of killed Mutants to display.
    @param List<Mutant> mutantsEquivalent
        The list of equivalent Mutants to display.
    @param GameMode gameType
        Type of the game. Used for the "Claim Equivalent" URLs and dialog message.
        TODO find a better solution for this
    @param int gameId
        The game id of this currently played game. Used for URL parameters.
--%>

<% { %>

<%
    List<Mutant> mutantsAliveTODORENAME = (List<Mutant>) request.getAttribute("mutantsAlive");
    List<Mutant> mutantsKilledTODORENAME = (List<Mutant>) request.getAttribute("mutantsKilled");
    List<Mutant> mutantsMarkedEquivalentTODORENAME = (List<Mutant>) request.getAttribute("mutantsMarkedEquivalent");
    List<Mutant> mutantsEquivalentTODORENAME = (List<Mutant>) request.getAttribute("mutantsEquivalent");
    Boolean markEquivalent = (Boolean) request.getAttribute("markEquivalent");
    Boolean viewDiff = (Boolean) request.getAttribute("viewDiff");
    GameMode gameType = (GameMode) request.getAttribute("gameType");
    int gameId = (Integer) request.getAttribute("gameId");
%>

<div class="tabs bg-minus-3" role="tablist">
    <div class="crow fly no-gutter down">
        <div>
            <a class="tab-link button text-black" href="#mutalivetab" role="tab" data-toggle="tab">Alive (<%= mutantsAliveTODORENAME.size() %>)</a>
        </div>

        <div>
            <a class="tab-link button text-black" href="#mutkilledtab" role="tab" data-toggle="tab">Killed(<%= mutantsKilledTODORENAME.size() %>)</a>
        </div>

        <div>
            <a class="tab-link button text-black" href="#mutmarkedequivtab" role="tab" data-toggle="tab">Flagged(<%= mutantsMarkedEquivalentTODORENAME.size() %>)</a>
        </div>

        <div>
            <a class="tab-link button text-black" href="#mutequivtab" role="tab" data-toggle="tab">Equivalent(<%= mutantsEquivalentTODORENAME.size() %>)</a>
        </div>
    </div>

    <div class="tab-content">
        <div class="tab-pane fade active in" id="mutalivetab">
            <% if (! mutantsAliveTODORENAME.isEmpty()) { %>
                <table id="alive-mutants" class="mutant-table display dataTable table table-striped table-hover table-responsive table-paragraphs bg-white">
                    <thead>  <%-- needed for datatable apparently --%>
                        <tr>
                            <th></th>
                            <th></th>
                            <th></th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>

                    <%
                        // Sorting mutants
                        List<Mutant> sortedMutants = new ArrayList<>(mutantsAliveTODORENAME);
                        sortedMutants.sort(Mutant.sortByLineNumberAscending());
                        for (Mutant m : sortedMutants) {
                    %>
                        <tr>
                            <td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= m.getCreatorName() %> (uid <%= m.getCreatorId() %>)</h4>
                                <% for (String change : m.getHTMLReadout()) { %>
                                    <p><%=change%><p>
                                <% } %>
                            </td>
                            <td class="col-sm-1"></td>
                            <td class="col-sm-1">
                                <h4>points: <%=m.getScore()%></h4>
                            </td>
                            <td class="col-sm-1">
                                <%
                                    if (markEquivalent
                                        && m.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                                        && m.isCovered()
                                        && m.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                                        && gameType == GameMode.PARTY
                                        && m.getLines().size() >= 1) {
                                                String lineString = String.join(",", m.getLines().stream().map(String::valueOf).collect(Collectors.toList()));
                                %>
                                <form id="equiv" action="<%=request.getContextPath() + Paths.BATTLEGROUND_GAME%>" method="post" onsubmit="return confirm('This will mark all player-created mutants on line(s) <%= lineString %> as equivalent. Are you sure?');">
                                    <input type="hidden" name="formType" value="claimEquivalent">
                                    <input type="hidden" name="equivLines" value="<%=lineString%>">
                                    <input type="hidden" name="gameId" value="<%=gameId%>">
                                    <button type="submit" class="btn btn-default btn-right">Claim Equivalent</button>
                                </form>
                                <%
                                    }
                                    if (m.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)){
                                %>
                                    <span>Flagged Equivalent</span>
                                <%  } %>

                                <% if (viewDiff){ %>
                                <a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>"
                                   data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
                                    <div id="modalMut<%=m.getId()%>" class="modal mutant-modal fade" role="dialog"
                                         style="z-index: 10000;">
                                        <div class="modal-dialog">
                                            <!-- Modal content-->
                                            <div class="modal-content">
                                                <div class="modal-header">
                                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                                    <h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
                                                </div>
                                                <div class="modal-body">
                                                    <pre class="readonly-pre"><textarea
                                                            class="mutdiff" title="mutdiff"
                                                            id="diff<%=m.getId()%>"><%=m.getHTMLEscapedPatchString()%></textarea></pre>
                                                </div>
                                                <div class="modal-footer">
                                                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                <% } %>
                            </td>
                        </tr>
                    <% } %>

                    </tbody>
                </table>
            <% } else { %>
                <div class="panel panel-default" style="background: white">
                    <div class="panel-body" style="    color: gray;    text-align: center;">
                        No mutants alive.
                    </div>
                </div>
            <% } %>
        </div>

        <div class="tab-pane fade" id="mutkilledtab">
            <% if (! mutantsKilledTODORENAME.isEmpty()) { %>
                <table id="killed-mutants" class="mutant-table display dataTable table table-striped table-responsive table-paragraphs bg-white">
                    <thead>  <%-- needed for datatable apparently --%>
                        <tr>
                            <th></th>
                            <th></th>
                            <th></th>
                            <th></th>
                        </tr>
                    </thead>

                    <tbody>

                    <%
                        // Sorting mutants
                        List<Mutant> sortedKilledMutants = new ArrayList<>(mutantsKilledTODORENAME);
                        sortedKilledMutants.sort(Mutant.sortByLineNumberAscending());
                        for (Mutant m : sortedKilledMutants) {
                            
                            Test killingTest = m.getKillingTest();
                            User creator = UserDAO.getUserForPlayer(killingTest.getPlayerId());
                            int killingTestID = killingTest.getId();
                            String ownerOfKillingTest = creator.getUsername();
                            
                    %>
                        <tr>
                            <td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= m.getCreatorName() %> (uid <%= m.getCreatorId() %>)</h4>
                                <% for (String change : m.getHTMLReadout()) { %>
                                    <p><%=change %><p>
                                <% } %>
                            </td>
                            <td class="col-sm-1"></td>
                            <td class="col-sm-1">
                                <h4>points: <%=m.getScore()%></h4>
                            </td>
                            <td class="col-sm-1">
                             <%-- Force buttons to be one on top the other. Maybe we can use one single button to "show details" --%>
                                <table>
                                    <tr role="row">
                                       <td class="col-sm-1"><a href="#"
                                        class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>"
                                        data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View
                                            Diff</a>
                                            <div id="modalMut<%=m.getId()%>"
                                                class="modal mutant-modal fade" role="dialog"
                                                style="z-index: 10000;">
                                                <div class="modal-dialog">
                                                    <!-- Modal content-->
                                                    <div class="modal-content">
                                                        <div class="modal-header">
                                                            <button type="button" class="close" data-dismiss="modal">&times;</button>
                                                            <h4 class="modal-title">
                                                                Mutant
                                                                <%=m.getId()%>
                                                                - Diff
                                                            </h4>
                                                        </div>
                                                        <div class="modal-body">
                                                            <pre class="readonly-pre">
                                                                <textarea class="mutdiff" title="mutdiff"
                                                                    id="diff<%=m.getId()%>"><%=m.getHTMLEscapedPatchString()%></textarea>
                                                            </pre>
                                                        </div>
                                                        <div class="modal-footer">
                                                            <button type="button" class="btn btn-default"
                                                                data-dismiss="modal">Close</button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr role="row">
                                        <td class="col-sm-1"><a href="#"
                                            class="btn btn-default btn-diff"
                                            id="btnMutKillMessage<%=m.getId()%>" data-toggle="modal"
                                            data-target="#modalMutKillMessage<%=m.getId()%>">View Killing Test</a>
                                            <div id="modalMutKillMessage<%=m.getId()%>"
                                                class="modal mutant-modal fade" role="dialog"
                                                style="z-index: 10000;">
                                                <div class="modal-dialog modal-lg">
                                                    <!-- Modal content is LARGE (modal-lg) -->
                                                    <div class="modal-content">
                                                        <div class="modal-header">
                                                            <button type="button" class="close" data-dismiss="modal">&times;</button>
                                                            <h4 class="modal-title">
                                                                Mutant <%=m.getId()%> was killed by test <%=killingTestID%> submitted by <%=ownerOfKillingTest%>
                                                            </h4>
                                                        </div>
                                                        <div class="modal-body">
                                                                <pre class="readonly-pre"><textarea class="killingTest" title="killingTest" cols="20" rows="10"><%=m.getKillingTest().getAsHTMLEscapedString()%></textarea></pre>
                                                                <pre class="readonly-pre build-trace"><%=m.getHTMLEscapedKillMessage()%></pre>
                                                        </div>
                                                        <div class="modal-footer">
                                                            <button type="button" class="btn btn-default"
                                                                data-dismiss="modal">Close</button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                </table>
                             </td> 
                        </tr>
                    <% } %>
                    </tbody>
                </table>
                <%-- Enable syntax highlighting for the killing test textarea --%>
                <script>
				    var x = document.getElementsByClassName("killingTest");
				    for (var i = 0; i < x.length; i++) {
				        CodeMirror.fromTextArea(x[i], {
				            lineNumbers: true,
				            matchBrackets: true,
				            mode: "text/x-java",
				            readOnly: true
				        });
				    }
				</script>
            <% } else {%>
                <div class="panel panel-default" style="background: white">
                    <div class="panel-body" style="    color: gray;    text-align: center;">
                        No mutants killed.
                    </div>
                </div>
            <% } %>
        </div>

        <div class="tab-pane fade" id="mutmarkedequivtab">
            <% if (! mutantsMarkedEquivalentTODORENAME.isEmpty()) { %>
            <table id="killed-mutants" class="mutant-table display dataTable table table-striped table-responsive table-paragraphs bg-white">
                <thead>  <%-- needed for datatable apparently --%>
                <tr>
                    <th></th>
                    <th></th>
                    <th></th>
                    <th></th>
                </tr>
                </thead>

                <tbody>

                <%
                    // Sorting mutants
                    List<Mutant> sortedKilledMutants = new ArrayList<>(mutantsMarkedEquivalentTODORENAME);
                    sortedKilledMutants.sort(Mutant.sortByLineNumberAscending());
                    for (Mutant m : sortedKilledMutants) {
                %>
                <tr>
                    <td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= m.getCreatorName() %> (uid <%= m.getCreatorId() %>)</h4>
                        <% for (String change : m.getHTMLReadout()) { %>
                        <p><%=change %><p>
                                <% } %>
                    </td>
                    <td class="col-sm-1"></td>
                    <td class="col-sm-1">
                        <h4>points: <%=m.getScore()%></h4>
                    </td>
                    <td class="col-sm-1">
                        <% if (viewDiff){ %>
                        <a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
                        <div id="modalMut<%=m.getId()%>" class="modal mutant-modal fade" role="dialog"
                             style="z-index: 10000;">
                            <div class="modal-dialog">
                                <!-- Modal content-->
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <button type="button" class="close" data-dismiss="modal">&times;</button>
                                        <h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
                                    </div>
                                    <div class="modal-body">
                                                    <pre class="readonly-pre"><textarea
                                                            class="mutdiff" title="mutdiff"
                                                            id="diff<%=m.getId()%>"><%=m.getHTMLEscapedPatchString()%></textarea></pre>
                                    </div>
                                    <div class="modal-footer">
                                        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <% } %>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <% } else {%>
            <div class="panel panel-default" style="background: white">
                <div class="panel-body" style="    color: gray;    text-align: center;">
                    No mutants killed.
                </div>
            </div>
            <% } %>
        </div>

        <div class="tab-pane fade" id="mutequivtab">
            <% if (! mutantsEquivalentTODORENAME.isEmpty()) { %>
            <table id="equiv-mutants" class="mutant-table display dataTable table table-striped table-hover table-responsive table-paragraphs bg-white">

                <thead>  <%-- needed for datatable apparently --%>
                    <tr>
                        <th></th>
                        <th></th>
                        <th></th>
                        <th></th>
                    </tr>
                </thead>

                <tbody>

                    <%
                        // Sorting mutants
                        List<Mutant> sortedMutantsEquiv = new ArrayList<>(mutantsEquivalentTODORENAME);
                        sortedMutantsEquiv.sort(Mutant.sortByLineNumberAscending());
                        for (Mutant m : sortedMutantsEquiv) {
                    %>
                        <tr>
                            <td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= m.getCreatorName() %> [UID: <%= m.getCreatorId() %>]</h4>
                                <% for (String change : m.getHTMLReadout()) { %>
                                    <p><%=change%><p>
                                <% } %>
                            </td>
                            <td class="col-sm-1"></td>
                            <td class="col-sm-1">
                                <h4>points: <%=m.getScore()%></h4>
                            </td>
                            <td class="col-sm-1">
                                <a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
                                <div id="modalMut<%=m.getId()%>" class="modal mutant-modal fade" role="dialog"
                                     style="z-index: 10000;">
                                    <div class="modal-dialog">
                                        <!-- Modal content-->
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <button type="button" class="close" data-dismiss="modal">&times;</button>
                                                <h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
                                            </div>
                                            <div class="modal-body">
                                                    <pre class="readonly-pre"><textarea
                                                            class="mutdiff" title="mutdiff"
                                                            id="diff<%=m.getId()%>"><%=m.getHTMLEscapedPatchString()%></textarea></pre>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
            <% } else {%>
            <div class="panel panel-default" style="background: white">
                <div class="panel-body" style="    color: gray;    text-align: center;">
                    No mutants equivalent.
                </div>
            </div>
            <% } %>
        </div>
    </div>
</div>

<script>
    $('.mutant-modal').on('shown.bs.modal', function() {
        var codeMirrorContainer = $(this).find(".CodeMirror")[0];
        if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
            codeMirrorContainer.CodeMirror.refresh();
        } else {
            var editorDiff = CodeMirror.fromTextArea($(this).find('textarea')[0], {
                lineNumbers: false,
                mode: "text/x-diff",
                readOnly: true /* onCursorActivity: null */
            });
            editorDiff.setSize("100%", 500);
        }
    });
</script>

<% } %>
