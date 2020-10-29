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
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.database.TestDAO" %>
<%@ page import="org.codedefenders.database.MutantDAO" %>
<%@ page import="org.codedefenders.game.multiplayer.PlayerScore" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.model.Event" %>
<%@ page import="org.codedefenders.beans.game.HistoryBean" %>

<jsp:useBean id="history" class="org.codedefenders.beans.game.HistoryBean" scope="request"/>
<%
    Map<Integer, PlayerScore> mutantScores = history.getMutantsScores();
    Map<Integer, PlayerScore> testScores = history.getTestScores();

    // Those return the PlayerID not the UserID
    final List<Player> attackers = history.getAttackers();
    final List<Player> defenders = history.getDefenders();
    final List<HistoryBean.HistoryBeanEventDTO> events = history.getEvents();

%>

<div id="history" class="modal fade" role="dialog" style="z-index: 10000; position: absolute;">
    <div class="modal-dialog" style="width: 900px">
        <!-- Modal content-->
        <div class="modal-content" style="z-index: 10000; position: absolute; width: 100%; left:0%;">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">History</h4>
            </div>
            <div class="modal-body" style="background: #eee">
                <link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css"
                      rel="stylesheet">
                <div class="container bootstrap snippets bootdeys">
                    <div class="col-md-9">
                        <div class="timeline-centered timeline-sm">
                            <%
                                for (HistoryBean.HistoryBeanEventDTO event : events) {

                            %>
                            <article class="timeline-entry "<%= event.getAlignment()%>>
                                <div class="timeline-entry-inner">
                                    <time datetime=<%=event.getFormat()%> class="timeline-time">
                                        <span><%= event.getTime() %></span>
                                        <span><%= event.getDate() %></span>
                                    </time>

                                    <div class="timeline-icon bg-<%= event.getColour() %>"><i class="fa fa-group"></i></div>
                                    <div class="timeline-label bg-<%= event.getColour() %>"><h4
                                            class="timeline-title"><%= event.getUserMessage() %>
                                    </h4>
                                        <p><%-- Body message here--%></p></div>
                                </div>
                            </article>
                            <%
                                }
                            %>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
