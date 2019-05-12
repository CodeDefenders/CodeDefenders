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

<script type="text/javascript">

    class PushSocket {
        constructor (url) {
            this.handlers = new Map();
            this.websocket = null;

            this.promise = new Promise((resolve, reject) => {
                console.log('Setting up WebSocket at ' + url + '.');
                this.websocket = new WebSocket(url);

                this.websocket.onopen  = evt => {
                    this.dispatch(PushSocket.EventType.OPEN,  evt);
                    console.log('WebSocket connection established.');
                    resolve();
                };

                this.websocket.onerror = evt => {
                    this.dispatch(PushSocket.EventType.ERROR, evt);
                    console.log('WebSocket error occurred.');
                };

                this.websocket.onclose = evt => {
                    this.dispatch(PushSocket.EventType.CLOSE, evt);
                    console.log('WebSocket connection closed.');
                };

                this.websocket.onmessage = evt => {
                    // https://stackoverflow.com/questions/7116035/parse-json-received-with-websocket-results-in-error
                    const {type, message} = JSON.parse(evt.data.replace(/[\s\0]/g, ' '));
                    this.dispatch(type, message);

                    console.log(evt); // TODO: remove later
                };
            });
        }

        /* Register callback for event type. */
        register (type, callback) {
            let list = this.handlers.get(type);
            if (list === undefined) {
                list = [];
                this.handlers.set(type, list);
            }

            list.push(callback);
        }

        /* Unregister callback for event type. */
        unregister (type, callback) {
            const list = this.handlers.get(type);
            if (list === undefined) return;

            const index = list.indexOf(callback);
            if (index === undefined) return;

            list.splice(index, 1);
        }

        /* Send a message to the server. */
        send (type, message) {
            const data = JSON.stringify({type, message});
            this.promise.then(() => {
                this.websocket.send(data);
            });
        }

        /* Dispatch a message to the registered handlers for the type. */
        dispatch (type, message) {
            const list = this.handlers.get(type);
            if (list !== undefined) {
                for (const callback of this.handlers.get(type)) {
                    callback(message);
                }
            }
        }

        get readyState () {
            return this.websocket.readyState;
        }

        static get EventType () {
            /* Enum for event types to prevent typos. */
            return {
                OPEN: 'OPEN',
                ERROR: 'ERROR',
                CLOSE: 'CLOSE',

                GAME: 'GAME',
                CHAT: 'CHAT',
                PROGRESSBAR: 'PROGRESSBAR'
                /* ... */
            };
        }
    }

    window.PushSocket = PushSocket;

    <%
        String name = request.getServerName();
        int port = request.getServerPort();
        String context = request.getContextPath();
        String ticket = (String) request.getAttribute(TicketingFilter.TICKET_REQUEST_ATTRIBUTE_NAME);
        int uid = (Integer) session.getAttribute("uid");
    %>

    const wsUri = "ws://<%=name%>:<%=port%><%=context%>/notifications/<%=ticket%>/<%=uid%>";
    window.pushSocket = new PushSocket(wsUri);

    <%--
    // TODO: is there a better way to do this?
    console.log("Setting up websocket at", wsUri);

    //Global Scope
    notificationMessageHandlers = [];
    websocket = new WebSocket(wsUri);
    websocket.onmessage = function(evt) {
        // Iterate over the registered handlers and pass the evt to them
        // This is brutal...
        // https://stackoverflow.com/questions/7116035/parse-json-received-with-websocket-results-in-error
        /* console.log("Got notification: " + evt.data ) */
        message = JSON.parse(evt.data.replace(/[\s\0]/g, ' '));
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
    function waitForSocketConnection(socket, callback) {
        setTimeout(
            function () {
                if (socket.readyState === 1) {
                    console.log("Connection is made");
                    if (callback != null) {
                        callback();
                    }

                } else {
                    console.log("wait for connection...");
                    waitForSocketConnection(socket, callback);
                }

            }, 50); // wait 50 milliseconds for the connection...
    }
    --%>
</script>