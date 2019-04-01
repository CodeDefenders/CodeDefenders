<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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

    This code setups a web socket for the page. It ensures that each request is validated against
    an automatically generated ticket which it matched with the user. Components wishing to receive push events
    must register proper handlers with the notifications channel.
    
--%>
<%@ page import="org.codedefenders.notification.web.TicketingFilter"  %>

<script language="javascript" type="text/javascript">
var wsUri = "ws://localhost:8080/notifications/<%=request.getAttribute(TicketingFilter.TICKET_REQUEST_ATTRIBUTE_NAME)%>/<%=session.getAttribute("uid")%>";
console.log("Setting up websocket at", wsUri)

//Global Scope
notificationMessageHandlers = []
websocket = new WebSocket(wsUri);
websocket.onmessage = function(evt) {
    // Iterate over the registered handlers and pass the evt to them
    // This is brutal...
    // https://stackoverflow.com/questions/7116035/parse-json-received-with-websocket-results-in-error
    /* console.log("Got notification: " + evt.data ) */
    message = JSON.parse(evt.data.replace(/[\s\0]/g, ' '))
    for (var key in notificationMessageHandlers) {
    	notificationMessageHandlers[key](message);
    }
};

// TODO Honestly I do not know this is the correct way, to just ref to websocket directly...
function sendMessage(msg){
    // Wait until the state of the socket is not ready and send the message when it is...
    waitForSocketConnection(websocket, function(){
        websocket.send(msg);
    });
}

// Make the function wait until the connection is made...
function waitForSocketConnection(socket, callback){
    setTimeout(
        function () {
            if (socket.readyState === 1) {
                console.log("Connection is made")
                if(callback != null){
                    callback();
                }
                return;

            } else {
                console.log("wait for connection...")
                waitForSocketConnection(socket, callback);
            }

        }, 50); // wait 5 milisecond for the connection...
}
</script>