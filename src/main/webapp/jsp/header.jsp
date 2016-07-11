<%@ include file="/jsp/header_base.jsp" %>
<div class="menu-top bg-light-blue .minus-2 text-white" style="padding: 5px;">
    <div class="full-width" style="padding-top: 3px;">
        <div class="nest">
            <div class="crow">
                <div class="ws-9" style="text-align: left">
                    <div class="inline crow fly">
                        <a class="text-white button tab-link bg-minus-1" href="games/user">My Games</a>
                        <a class="text-white button tab-link bg-minus-1" href="games/open">Open Games</a>
                        <a class="text-white button tab-link bg-minus-1" href="games/upload">Upload Class</a>
                        <a class="text-white button tab-link bg-minus-1" href="games/history">History</a>
                    </div>
                </div>
                <div class="ws-3" style="text-align: right;">
                    <div class="tabs-blue-grey ws-12">
                        <ul class="inline">
                            <li>
                                <div class="button tab-link text-white relative bg-minus-1 ws-12" style="min-width: 100px; text-align: center;">
                                    <span class="glyphicon glyphicon-user" aria-hidden="true"></span>
                                    <p style="display: inline; width:70%">
                                        <%=request.getSession().getAttribute("username")%>
                                    </p>
                                    <div class="bg-white relative drop down-left card baseline-padding ws-12">
                                        <ul class="unstyled">
                                            <li>
                                                <a href="/logout" class="text-white list-item">
                                                    <span class="glyphicon glyphicon-remove"></span>
                                                    logout
                                                </a>
                                            </li>
                                        </ul>
                                    </div>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
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