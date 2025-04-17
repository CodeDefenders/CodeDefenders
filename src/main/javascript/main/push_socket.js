/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * Sets up a web socket for the page. It ensures that each request is validated against
 * an automatically generated ticket which it matched with the user. Components wishing to receive push events
 * must register proper handlers with the notifications channel.
 *
 * <ul>
 * <li>
 *     "subscribe" and "unsubscribe" are used to subscribe to events from the server.
 *      When called, a registration event is sent to the server.
 *      One registration event may encompass multiple event types on the JavaScript side.
 * </li>
 * <li>
 *      "register" and "unregister" are used to register callbacks for certain server events
 *      or WebSocket events ('open', 'error', 'close', 'message').
 * </li>
 * <li>
 *      "send" is used to send events to the server.
 * </li>
 * <li>
 *      For "register", "unregister" and "send", the event types are given by the EventNames class.
 * </li>
 * </ul>
 */
class PushSocket {
    constructor (url) {
        console.log(`Setting up WebSocket at ${url}`);

        /**
         * The URL to connect the WebSocket to.
         */
        this._url = url;

        /**
         * Event handlers by the event name they handle.
         * @type {Map<string, function[]>}
         */
        this._handlers = new Map();

        /**
         * A queue to buffer messages and send them once the connection has been established.
         * @type {PushSocketMessage[]}
         */
        this._queue = [];

        this._initWebSocket();
    }

    _initWebSocket () {
        this.websocket = new WebSocket(this._url);

        this.websocket.onopen = evt => {
            console.log('WebSocket connection established.');
            this.dispatch(PushSocket.WSEventType.OPEN, evt);
            for (const event of this._queue) {
                this.websocket.send(JSON.stringify(event));
            }
            this._queue = [];
        };

        this.websocket.onerror = evt => {
            console.error('WebSocket error occurred.');
            this.dispatch(PushSocket.WSEventType.ERROR, evt);
        };

        this.websocket.onclose = evt => {
            console.log('WebSocket connection closed.');
            this.dispatch(PushSocket.WSEventType.CLOSE, evt);
        };

        this.websocket.onmessage = evt => {
            // https://stackoverflow.com/questions/7116035/parse-json-received-with-websocket-results-in-error
            this.dispatch(PushSocket.WSEventType.MESSAGE, evt);
            const {type, data} = JSON.parse(evt.data.replace(/[\s\0]/g, ' '));
            this.dispatch(type, data);
        };
    }

    /**
     * Closes the WebSocket, if not already closed, and reconnects it.
     */
    reconnect () {
        this.websocket.close(1000, 'Reconnecting');
        this._initWebSocket();
    }

    /**
     * Registers a callback for a server event or WebSocket event ('open', 'error', 'close', 'message').
     * @param {string} type The type to register a callback for.
     *                 Use the EventNames class to get the type for server events.
     * @param {function} callback The callback to register.
     */
    register (type, callback) {
        let callbacks = this._handlers.get(type);

        if (callbacks === undefined) {
            callbacks = [];
            this._handlers.set(type, callbacks);
        }

        callbacks.push(callback);
    }

    /**
     * Unregisters a callback for a server event or WebSocket event ('open', 'error', 'close', 'message').
     * @param {string} type The type of callback to unregister.
     *                 Use the EventNames class to get the type for server events.
     * @param {function} callback The callback to register.
     */
    unregister (type, callback) {
        const callbacks = this._handlers.get(type);
        if (callbacks === undefined || callbacks.length === 0) {
            console.error(`Tried to unregister callback for type '${type}', `
                    + 'but no callback is registered for the type.');
            return;
        }

        const index = callbacks.indexOf(callback);
        if (index === -1) {
            console.error(`Tried to unregister callback for type '${type}', `
                    + 'but given callback was not registered.');
            return;
        }

        callbacks.splice(index, 1);
    }

    /**
     * Subscribes at the server to receive events for the given type of event.
     * If the connection is not established, the registration message will be sent when it is.
     * @param {string} type The type of event to subscribe to.
     *                 Use the EventNames class to get the type for server events.
     * @param {object} params Additional parameters to send to the server for registration.
     */
    subscribe(type, params = {}) {
        this.send(type, {
            action: 'REGISTER',
            ...params
        });
    }

    /**
     * Unsubscribes at the server to stop receiving events for the given type of event.
     * If the connection is not established, the registration message will be sent when it is.
     * @param {string} type The type of event to unsubscribe from.
     *                      Use the EventNames class to get the type for server events.
     * @param {object} params Additional parameters to send to the server for registration.
     */
    unsubscribe(type, params = {}) {
        this.send(type, {
            action: 'UNREGISTER',
            ...params
        })
    }

    /**
     * Sends a event to the server. If the connection is not established, the message will be sent when it is.
     * @param {string} type The type of the event.
     *                      Use the EventNames class to get the type for client events.
     * @param {object} data The data of the event.
     */
    send (type, data) {
        const event = {type, data};
        if (this.readyState === WebSocket.OPEN) {
            this.websocket.send(JSON.stringify(event));
        } else if (this.readyState === WebSocket.CONNECTING) {
            this._queue.push(event)
        } else {
            console.error('Tried to send WebSocket event when the connection is closed.');
        }
    }

    /**
     * Dispatches an event to the registered handlers for the type.
     * @param {string} type The type of the event.
     *                      Use the EventNames class to get the type for client events.
     * @param {object} data The data of the event.
     */
    dispatch (type, data) {
        const callbacks = this._handlers.get(type);
        if (callbacks !== undefined) {
            for (const callback of callbacks) {
                callback(data);
            }
        }
    }

    get readyState () {
        return this.websocket.readyState;
    }

    static WSEventType = {
        OPEN: 'open',
        CLOSE: 'close',
        ERROR: 'error',
        MESSAGE: 'message'
    };
}


export default PushSocket;
