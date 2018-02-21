<%@ page import="org.codedefenders.*" %>
<% String pageTitle = null; %>
<%@ include file="/jsp/header.jsp" %>

<div class="full-width">
    <ul class="nav nav-tabs">
        <li><a href="<%=request.getContextPath()%>/admin/games"> Manage Games</a></li>
        <li class="active"><a href="#">Manage Users</a></li>
    </ul>

    <h3>Users</h3>
    <h3>Create Accounts</h3>
</div>
<%@ include file="/jsp/footer.jsp" %>
