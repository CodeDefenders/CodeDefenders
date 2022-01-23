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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.beans.game.HistoryBean" %>

<jsp:useBean id="history" class="org.codedefenders.beans.game.HistoryBean" scope="request"/>
<%
    // Those return the PlayerID not the UserID
    final List<HistoryBean.HistoryBeanEventDTO> events = history.getEvents();
%>

<%--@elvariable id="history" type="org.codedefenders.beans.game.HistoryBean"--%>

<link href="${pageContext.request.contextPath}/css/specific/timeline.css" rel="stylesheet">

<div id="history" class="modal fade" tabindex="-1">
    <div class="modal-dialog" style="max-width: 900px">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">History</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body" style="background: #eee; overflow-y: auto; max-height: 55vh">
                <div class="timeline-centered timeline-sm">
                    <c:forEach items="${history.events}" var="event">
                        <article class="timeline-entry ${event.alignment}-aligned">
                            <div class="timeline-entry-inner">
                                <time datetime="${event.format}" class="timeline-time">
                                    <span>${event.time}</span>
                                    <span>${event.date}</span>
                                </time>

                                <div class="timeline-icon bg-${event.colour}"><i class="fa fa-group"></i>
                                </div>
                                <div class="timeline-label bg-${event.colour}">
                                    <span class="h5 timeline-title">${event.userMessage}</span>
                                    <%--
                                        If events ever have a body message:
                                        <c:if test="${not empty event.message}">
                                            <p class="mt-2"><!-- Body message here --></p>
                                        </c:if>
                                    --%>
                                </div>
                            </div>
                        </article>
                    </c:forEach>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
