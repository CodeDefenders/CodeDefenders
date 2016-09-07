<%@ page import="org.apache.commons.lang.ArrayUtils" %>
<%@ include file="/jsp/header_base.jsp" %>
<div class="menu-top bg-light-blue .minus-2 text-white" style="padding: 5px;">
    <div class="full-width" style="padding-top: 3px;">
        <div class="ws-12 container" style="text-align: right; clear:
        both; margin: 0px; padding: 0px; width: 100%;">
            <button type="button"
                    class="navbar-toggle tex-white buton tab-link bg-minus-1" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                Menu <span class="glyphicon glyphicon-plus"></span>
            </button>
            <ul class="navbar navbar-nav collapse navbar-collapse"
               id="bs-example-navbar-collapse-1"
                 style="z-index: 1000; text-align: center; list-style:none;
                 width: 100%;">
                <li><a class="text-white button tab-link bg-minus-1"
                    href="games/user" style="width:100%;">My Games</a></li>
                <li><a class="text-white button tab-link bg-minus-1"
                       href="games/open" style="width:100%;">Open Games</a></li>
                <li><a class="text-white button tab-link bg-minus-1" href="games/upload" style="width:100%;">Upload Class</a></li>
                <li><a class="text-white button tab-link bg-minus-1" href="games/history" style="width:100%;">History</a></li>
                <li><a class="text-white button tab-link bg-minus-1" href="leaderboards" style="width: 100%;>Leaderboards</a></li>
                <li><a class="text-white button tab-link bg-minus-1" href="help" style="width:100%;">Help</a></li>
                <li><a href="/logout" class="text-white button tab-link bg-plus-2" style="width:100%;">
                        <span class="glyphicon glyphicon-user" aria-hidden="true"></span>
                        <%=request.getSession().getAttribute("username")%> [Logout]
                </a></li>
                </ul>
        </div>
    </div>
</div>

<form id="logout" action="login" method="post">
    <input type="hidden" name="formType" value="logOut">
</form>

    <%
	ArrayList<String> messages = (ArrayList<String>) request.getSession().getAttribute("messages");
	request.getSession().removeAttribute("messages");
	if (messages != null && ! messages.isEmpty()) {
%>
<div class="alert alert-info" id="messages-div">
    <a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><br />
    <%
        boolean fadeOut = true;
        for (String m : messages) { %>
    <pre><strong><%=m%></strong></pre>
    <%
            if (m.equals(Constants.MUTANT_UNCOMPILABLE_MESSAGE)
                    || m.equals(Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE)
                    || m.equals(Constants.TEST_DID_NOT_COMPILE_MESSAGE)) {
                fadeOut = false;
            }
        }
        if (fadeOut) {
    %>
    <script> $('#messages-div').delay(10000).fadeOut(); </script>
    <% } %>
</div>
<%	} %>
<div class="nest">
    <div class="full-width">
        <div class="bg-plus-2" style="padding:2px 0;">
        </div>
        <% if (pageTitle != null) { %>
            <h2 class="full-width page-title"><%= pageTitle %></h2>
        <% } %>
        <div class="nest">
            <div class="crow fly no-gutter">
                <div class="crow fly no-gutter">