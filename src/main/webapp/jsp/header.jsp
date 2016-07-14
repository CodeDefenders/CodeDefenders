<%@ include file="/jsp/header_base.jsp" %>
<div class="menu-top bg-light-blue .minus-2 text-white" style="padding: 5px;">
    <div class="full-width" style="padding-top: 3px;">
        <div class="nest">
            <nav class="crow fly collapse navbar-collapse">
                <div class="ws-12 container" style="text-align: left">
                    <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                        <span class="sr-only">Toggle navigation</span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </button>
                    <div class="inline crow fly">
                        <a class="text-white button tab-link bg-minus-1" href="games/user">My Games</a>
                        <a class="text-white button tab-link bg-minus-1" href="games/open">Open Games</a>
                        <a class="text-white button tab-link bg-minus-1" href="games/upload">Upload Class</a>
                        <a class="text-white button tab-link bg-minus-1" href="games/history">History</a>
                        <a href="/logout" class="text-white button tab-link bg-plus-2">
                                <span class="glyphicon glyphicon-user" aria-hidden="true"></span>
                                <%=request.getSession().getAttribute("username")%> [Logout]
                        </a>
                    </div>
                </div>
            </nav>
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
    <% for (String m : messages) { %>
    <pre><strong><%=m%></strong></pre>
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