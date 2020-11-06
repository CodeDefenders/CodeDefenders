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
<%@page import="org.codedefenders.game.AbstractGame"%>
<%@page import="org.codedefenders.util.Paths"%>
<%@ page import="org.codedefenders.model.NotificationType"%>
<%@ page import="org.codedefenders.util.Paths" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean"
	scope="request" />

<%
		AbstractGame game = (AbstractGame) request.getAttribute("game");
        int gameId = game.getId();
%>

<script type="text/javascript">
    const updateMessages = function (url) {
        $.getJSON(url, function (r) {
            $(r).each(function (index) {

                // Skip messages that belong to the current user
                if (r[index].userId === ${login.userId}) {
                    return;
                }
                // Create the Div to host events if that's not there
                if (document.getElementById("push-events-div") == null) {
                    const div = document.createElement('div');
                    div.setAttribute('class', 'alert alert-info');
                    div.setAttribute('id', 'push-events-div');
                    // This is fine, but then it closes it and no messaged can be shown anymore !
                    div.innerHTML = '<button type="button" class="close" data-dismiss="alert" aria-label="Close">&times;</button><br/>';
                    const form = document.getElementById('logout');
                    form.parentNode.insertBefore(div, form.nextSibling);
                }

                const msgId = '_' + Math.random().toString(36).substr(2, 9);
                const msg = document.createElement('pre');
                msg.setAttribute('id', msgId);
                msg.innerHTML = "<strong>" + r[index].message + "</strong>";
                document.getElementById("push-events-div").appendChild(msg);
                // Fade out and remove the message
                $('#' + msgId).delay(10000).fadeOut("normal", function () {
                    $(this).remove();
                    // Check how many elements are left, in case no more messages are there, remove the bar as well...
                    const div = document.getElementById("push-events-div");
                    if (div != null) {
                        if (div.getElementsByTagName('*').length <= 2) { // There's <a> and <br>
                            document.getElementById("push-events-div").remove();
                        }
                    }
                });
            });
        });
    };

    $(document).ready(function () {
        //notifications written here:
        // refreshed every 5 seconds
        const interval = 5000;
        setInterval(function () {
            const url = "<%=request.getContextPath() + Paths.API_NOTIFICATION%>?type=<%=NotificationType.PUSHEVENT%>&gameId=" + <%=gameId%> +"&timestamp=" + (new Date().getTime() - interval);
            updateMessages(url);
        }, interval)
    });
</script>
