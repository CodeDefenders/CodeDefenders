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

<%--
    @param String notification-ticket (TicketingFilter.TICKET_REQUEST_ATTRIBUTE_NAME)
        The ticket used for the WebSocket connection.

    See (server-side) PushSocket's JavaDoc or (client-side) PushSocket's JSDoc for more information.
--%>

<%@ page import="org.codedefenders.notification.web.TicketingFilter"  %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<script type="text/javascript" src="js/push_socket.js"></script>

<script>
    /* Wrap in a function to avoid polluting the global scope. */
    (function () {
        const baseWsUri = document.baseURI
                .replace(/^http/, 'ws')
                .replace(/\/$/, '');
        const ticket = '${requestScope[TicketingFilter.TICKET_REQUEST_ATTRIBUTE_NAME]}';
        const userId = '${login.userId}';
        const wsUri = `\${baseWsUri}/notifications/\${ticket}/\${userId}`;
        CodeDefenders.objects.pushSocket = new CodeDefenders.classes.PushSocket(wsUri);
    })();
</script>
