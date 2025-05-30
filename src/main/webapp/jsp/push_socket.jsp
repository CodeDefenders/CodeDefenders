<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>

<script type="module">
    import {objects, PushSocket} from '${url.forPath("/js/codedefenders_main.mjs")}';

    const baseWsUri = '${url.getAbsoluteURLForPath("/")}'
            .replace(/^http/, 'ws')
            .replace(/\/$/, '');
    let ticket = '${requestScope[TicketingFilter.TICKET_REQUEST_ATTRIBUTE_NAME]}';
    const userId = '${login.userIdOrMinusOne}';
    if (userId !== '-1') {
        if (ticket === '') {
            ticket = userId;
        }
        const wsUri = `\${baseWsUri}/notifications/\${ticket}/\${userId}`;


        const pushSocket = new PushSocket(wsUri);
        objects.register('pushSocket', pushSocket);
    }
</script>
