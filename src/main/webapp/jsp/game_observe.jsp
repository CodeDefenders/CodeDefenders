<% String pageTitle = "Timeline CodeDefenders"; %>
<%
    Object uid = request.getSession().getAttribute("uid");
    Object username = request.getSession().getAttribute("username");
    if (uid != null && username != null){
%>
<%@ include file="/jsp/header.jsp" %>
<%} else {%>
<%@ include file="/jsp/header_logout.jsp" %>
<%}%>

 <%-- Visualize the events as timeline. Requires specific parameters --%>
<%@include file="/jsp/game_components/event_timeline.jsp"%>

<%@ include file="/jsp/footer.jsp" %>