<%@ page import="org.apache.commons.lang.ArrayUtils" %>
<%@ page import="org.codedefenders.Constants" %>
<%@ page import="java.util.ArrayList" %>
<%@ include file="/jsp/header_base.jsp" %>

<script>
    //If the user is logged in, start receiving notifications
    var updateUserNotifications = function(url) {
        $.get(url, function (r) {

            var notificationCount = 0;

            $(r).each(function (index) {
                $("#userDropDown li:first-child").after(
                    "<li><a " +
                    "href=\"/multiplayer/games?id=" + r[index].gameId +
                    "\" style=\"width:100%;\">" +
                    r[index].parsedMessage +
                    "</a></li>"
                )

                if (r[index].eventStatus == "NEW"){
                    notificationCount += 1;
                }
            });

            var lastNotifCount = parseInt($("#notificationCount").html());

            var notifCount =  notificationCount;

            if (lastNotifCount != null && !isNaN(lastNotifCount)){
                notifCount += lastNotifCount;
            }

            $("#notificationCount").html(notifCount);
        });
    }

    $(document).ready(function() {
            if ($("#userDropDown").length) {
                //notifications written here:
                // refreshed every 5 seconds
                var interval = 5000;
                setInterval(function () {
                    var url = "/game_notifications?userId=" + <%=request.getSession().getAttribute("uid")%> +"&timestamp=" + (new Date().getTime() - interval);

                    updateUserNotifications(url);

                }, interval)

                var url = "/game_notifications?userId=" + <%=request.getSession().getAttribute("uid")%> +"&timestamp=" + 0;

                updateUserNotifications(url);
            }
        }
        );
</script>

<div class="menu-top bg-light-blue .minus-2 text-white" style="padding: 5px;">
    <div class="full-width" style="padding-top: 3px;">
        <div class="ws-12 container" style="text-align: right; clear:
        both; margin: 0px; padding: 0px; width: 100%;">
            <button type="button"
                    class="navbar-toggle tex-white buton tab-link bg-minus-1" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                Menu <span class="glyphicon glyphicon-plus"></span>
            </button>
            <ul
                    class="crow fly no-gutter navbar navbar-nav collapse navbar-collapse"
               id="bs-example-navbar-collapse-1"
                 style="z-index: 1000; text-align: center; list-style:none;
                 width: 80%; float: none; margin: 0 auto;">
                <li style="float: none" class="dropdown"><a
                            class="text-white button tab-link bg-minus-1 dropdown-toggle"
                            href="games/user"
                            style="width:100%;" data-toggle="dropdown" href="#">Games <span class="glyphicon glyphicon-menu-hamburger" style="float: right;"></span></a>
                        <ul class="dropdown-menu" style="background-color:
                        #FFFFFF; border: 1px solid #000000;">
                            <li><a
                                   href="games/user" style="width:100%;">My Games</a></li>
                            <li><a
                                   href="games/open" style="width:100%;">Open
                                Games</a></li>
                            <li><a href="games/tutorial" style="width:100%;">Tutorial</a></li>
                            <li><a href="games/history" style="width:100%;">History</a></li>
                </ul></li>
                <li style="float: none"><a class="text-white button tab-link bg-minus-1" href="games/upload" style="width:100%;">Upload Class</a></li>
                <li style="float: none"><a class="text-white button tab-link bg-minus-1" href="leaderboards" style="width: 100%;">Leaderboard</a></li>
                <li style="float: none"><a class="text-white button tab-link bg-minus-1" href="help" style="width:100%;">Help</a></li>
                <li style="float: none" class="dropdown"><a
                        class="text-white button tab-link bg-minus-1 dropdown-toggle"
                        href="games/user"
                        style="width:100%;" data-toggle="dropdown" href="#"><span class="glyphicon glyphicon-user" aria-hidden="true"></span>
                    <%=request.getSession().getAttribute("username")%>
                    (<span id="notificationCount"></span>)
                    <span class="glyphicon glyphicon-menu-hamburger" style="float: right;"></span></a>
                    <ul id="userDropDown" class="dropdown-menu"
                    style="background-color:
                        #FFFFFF; border: 1px solid #000000;">
                        <li><a
                               href="/logout"
                               style="width:100%;border-bottom:1px solid
                               black">Logout
                        </a></li>
                    </ul></li>
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