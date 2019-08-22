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
    Sets up a web socket for the page. It ensures that each request is validated against
    an automatically generated ticket which it matched with the user. Components wishing to receive push events
    must register proper handlers with the notifications channel.

    Usage:
        After running this script, the socket with the connection already established (or trying to establish a
        connection if not possible) will be available under "window.pushSocket". The PushSocket class will be avalilable
        under "window.PushSocket".

        The socket automatically tries to reconnect to the server if the connection can't be established or is lost.
        Messages sent while the connection is lost will be sent when the connection is established again.

        "subscribe" and "unsubscribe" are used to subscribe to events on the server side.
        Here, the event types are given by RegistrationEvent.EventType,
        and may encompass multiple event types on the JavaScript side.

        "register" and "unregister" are used to register callbacks for certain event types received by the websocket.
        Here, the event types are given by the simple class names in the events/server package.

        "send" is used to send regular events / messages to the server.
        Here, the event types are given by the simple class names in the events/client package.

        "registerWS" and "unregisterWS" are used to register to websocket-specific events.
        Here, the event types are given by PushSocket.WSEventType (the JavaScript class).

    See PushSocket's JavaDoc for more information.

    @param String notification-ticket (TicketingFilter.TICKET_REQUEST_ATTRIBUTE_NAME)
        The ticket used for the WebSocket connection.
--%>

<%@ page import="org.codedefenders.notification.web.TicketingFilter"  %>
<%@ page import="org.codedefenders.notification.events.client.RegistrationEvent" %>

<script src="js/reconnecting-websocket-iife.min.js"></script>

<%
    String name = request.getServerName();
    int port = request.getServerPort();
    String context = request.getContextPath();
    String ticket = (String) request.getAttribute(TicketingFilter.TICKET_REQUEST_ATTRIBUTE_NAME);
    int uid = (Integer) session.getAttribute("uid");
%>

<script type="text/javascript">
    class PushSocket {
        constructor (url) {
            console.log('Setting up WebSocket at ' + url + '.');

            this.handlers = new Map();
            this.websocket = new ReconnectingWebSocket(url);

            this.websocket.onopen  = evt => console.log('WebSocket connection established.');
            this.websocket.onerror = evt => console.log('WebSocket error occurred.');
            this.websocket.onclose = evt => console.log('WebSocket connection closed.');
            this.websocket.onmessage = evt => {
                // https://stackoverflow.com/questions/7116035/parse-json-received-with-websocket-results-in-error
                const {type, data} = JSON.parse(evt.data.replace(/[\s\0]/g, ' '));
                this.dispatch(type, data);
                console.log({type, data});
            };
        }

        /**
         * Registers a callback for a server event.
         * This will send a registration message to the server if the event type is not registered on the server.
         * @param {string} type The type to register a callback for.
         * @param {function} callback The callback to register.
         */
        register (type, callback) {
            let callbacks = this.handlers.get(type);

            if (callbacks === undefined) {
                callbacks = [];
                this.handlers.set(type, callbacks);
            }

            callbacks.push(callback);
        }

        /**
         * Unregisters a callback for a server event.
         * This will send a registration message to the server if the .
         * @param {string} type The type to register a callback for.
         * @param {function} callback The callback to register.
         */
        unregister (type, callback) {
            const callbacks = this.handlers.get(type);
            if (callbacks === undefined || callbacks.length === 0) {
                console.error('Tried to unregister callback for type "' + type + "', "
                    + 'but no callback is registered for the type.');
                return;
            }

            const index = callbacks.indexOf(callback);
            if (index === -1) {
                console.error('Tried to unregister callback for type "' + type + "', "
                    + 'but given callback was not registered.');
                return;
            }

            callbacks.splice(index, 1);
        }

        /**
         * Subscribe at the server to receive events for the given type of Event.
         * @param {string} type The type of event to subscribe to.
         * @param {object} params Additional parameters to send to the server for registration.
         */
        subscribe(type, params = {}) {
            this.send('<%=RegistrationEvent.class.getSimpleName()%>', {
                type,
                action: '<%=RegistrationEvent.Action.REGISTER.toString()%>',
                ...params
            });
        }

        /**
         * Unregister at the server to stop receiving events for the given type of event.
         * @param {string} type The type of event to unsubscribe from.
         */
        unsubscribe(type) {
            this.send('<%=RegistrationEvent.class.getSimpleName()%>', {
                type,
                action: '<%=RegistrationEvent.Action.UNREGISTER.toString()%>',
            })
        }

        /**
         * Sends a message to the server.
         * @param {string} type The type of the message.
         * @param {object} data The data of the message.
         */
        send (type, data) {
            const message = JSON.stringify({type, data});
            this.websocket.send(message);
        }

        /**
         * Dispatches an event to the registered handlers for the type.
         * @param {string} type The type of the event.
         * @param {object} data The data of the event.
         */
        dispatch (type, data) {
            const callbacks = this.handlers.get(type);
            if (callbacks !== undefined) {
                for (const callback of callbacks) {
                    callback(data);
                }
            }
        }

        /**
         *  Register a callback for a WebSocket event ('open', 'error', 'close', 'message').
         *  @param {string} type The type to register a callback for.
         *  @param {function} callback The callback to register.
         */
        registerWS (type, callback) {
            this.websocket.addEventListener(type, callback)
        }

        /**
         *  Unregister a callback for a WebSocket event ('open', 'error', 'close', 'message').
         *  @param {string} type The type to register a callback for.
         *  @param {function} callback The callback to register.
         */
        unregisterWS (type, callback) {
            this.websocket.removeEventListener(type, callback)
        }

        get readyState () {
            return this.websocket.readyState;
        }

        static get WSEventType () {
            return {
                OPEN: 'open',
                CLOSE: 'close',
                ERROR: 'error',
                MESSAGE: 'message'
            };
        }
    }

    window.PushSocket = PushSocket;
    const wsUri = "ws://<%=name%>:<%=port%><%=context%>/notifications/<%=ticket%>/<%=uid%>";
    window.pushSocket = new PushSocket(wsUri);
</script>
