<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ tag import="org.codedefenders.game.Role" %>
<%@ tag import="org.codedefenders.game.ChatCommand" %>

<%--@elvariable id="gameChat" type="org.codedefenders.beans.game.GameChatBean"--%>
<%--@elvariable id="eventNames" type="org.codedefenders.beans.notification.EventNamesBean"--%>

<c:if test="${gameChat.chatEnabled}">

<style>
    #chat .chat-message {
        padding-top: 2px;
        padding-bottom: 2px;
    }

    #chat .chat-message .chat-message-name::after {
        content: ":";
        padding-right: .25em;
    }

    /* Message prefixes. */
    #chat .chat-message-all .chat-message-name::before {
        content: "[All]";
        padding-right: .25em;
    }
    #chat .chat-message-team.chat-message-attacker .chat-message-name::before {
        content: "[Attacker]";
        padding-right: .25em;
    }
    #chat .chat-message-team.chat-message-defender .chat-message-name::before {
        content: "[Defender]";
        padding-right: .25em;
    }
    #chat .chat-message-team.chat-message-player .chat-message-name::before {
        content: "[Player]";
        padding-right: .25em;
    }
    #chat .chat-message-team.chat-message-observer .chat-message-name::before {
        content: "[Observer]";
        padding-right: .25em;
    }

    /* Role colors. */
    #chat .chat-message-attacker .chat-message-name {
        color: #bf0035;
    }
    #chat .chat-message-defender .chat-message-name {
        color: #0041db;
    }
    #chat .chat-message-player .chat-message-name {
        color: #1e9f02;
    }
    #chat .chat-message-observer .chat-message-name {
        color: #ff8300;
        font-weight: bold;
    }
    #chat .chat-message-system {
        color: gray;
    }
</style>

<div id="chat" style="position: fixed; left: 0; bottom: 0; z-index: 11;" hidden>
    <div class="card m-0">
        <c:choose>
            <c:when test="${gameChat.showTabs}">
                <div id="chat-handle" class="card-header p-1 d-flex align-items-center">
                    <button type="button" data-tab="ALL" class="chat-tab-button btn btn-xs btn-outline-success ms-1 active"
                            title="Show all message.">All</button>
                    <button type="button" data-tab="ATTACKERS" class="chat-tab-button btn btn-xs btn-outline-danger ms-1"
                            title="Show messages from the perspective of the attacker team.">Attackers</button>
                    <button type="button" data-tab="DEFENDERS" class="chat-tab-button btn btn-xs btn-outline-primary ms-1"
                            title="Show messages from the perspective of the defender team.">Defenders</button>
                    <button id="chat-close" type="button" class="btn-close m-1 ms-auto"></button>
                </div>
            </c:when>
            <c:otherwise>
                <div id="chat-handle" class="card-header p-0 d-flex align-items-center">
                    <button id="chat-close" type="button" class="btn-close m-1 ms-auto"></button>
                </div>
            </c:otherwise>
        </c:choose>
        <div class="card-body p-0">
            <div id="chat-messages-container" style="height: 30em; width: 25em; overflow-y: scroll;">
                <div id="chat-messages" style="word-wrap: break-word; padding: .5em .75em .5em .75em;"></div>
            </div>
        </div>
        <div class="card-footer">
            <c:choose>
                <c:when test="${gameChat.showChannel}">
                    <div class="input-group">
                        <div id="chat-channel" class="input-group-text d-flex justify-content-center"
                             style="min-width: 4.5rem; cursor: pointer;"
                             title="Switch between sending messages to your own team or all players.">
                            Team
                        </div>
                        <label class="visually-hidden" for="chat-input">Message</label>
                        <textarea type="text" id="chat-input" class="form-control"
                                  maxlength="${gameChat.maxMessageLength}"
                                  placeholder="Message"
                                  style="resize: none;"></textarea>
                    </div>
                </c:when>
                <c:otherwise>
                    <div>
                        <div id="chat-channel" hidden>
                            Team
                        </div>
                        <label class="visually-hidden" for="chat-input">Message</label>
                        <textarea type="text" id="chat-input" class="form-control"
                                  maxlength="${gameChat.maxMessageLength}"
                                  placeholder="Message"
                                  style="resize: none;"></textarea>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<button type="button" id="chat-indicator" class="btn btn-sm btn-outline-secondary">
    <i class="fa fa-comments"></i>
    Chat
    <span id="chat-count" class="badge bg-secondary">0</span>
</button>

<script>
(function () {

    /**
     * Manages the stored messages and displays them.
     */
    class Messages {
        /**
         * @param {HTMLDivElement} messagesEl The div containing the message elements.
         * @param {HTMLDivElement} containerEl The scrollable container containing the messagesEl element.
         */
        constructor (messagesEl, containerEl) {
            this.messages = [];
            this.filter = Messages.FILTER_ALL;
            this.messagesEl = messagesEl;
            this.containerEl = containerEl;
        }

        /**
         * Filters the messages according to the given filter.
         * @param {Function<object, boolean>} filter The filter.
         */
        setFilter (filter) {
            this.filter = filter;
            this.redraw();
        }

        /**
         * Replaces the stored messages with the given messages and displays
         * the messages passing the currently set filter.
         * @param {object[]} messages The new messages.
         */
        setMessages (messages) {
            if (messages.length > ${gameChat.messageLimit}) {
                this.messages = messages.slice(messages.length - ${gameChat.messageLimit}, messages.length);
            } else {
                this.messages = [...messages];
            }
            this.redraw();
        }

        /**
         * Adds a new message and displays it, if it passes the currently set filter.
         * @param {object} message The new message.
         */
        addMessage (message) {
            this.messages.push(message);
            if (this.messages.length > ${gameChat.messageLimit}) {
                this.messages.shift();
            }

            if (this.filter(message)) {
                const wasScrolledToBottom = this.isScrolledToBottom();
                this.messagesEl.appendChild(this.renderMessage(message));
                if (wasScrolledToBottom) {
                    this.scrollToBottom();
                }
            }
        }

        /**
         * Clears the displayed messages and redraws them, filtering them according to the set filter.
         */
        redraw () {
            this.messagesEl.innerHTML = '';

            const messages = this.messages.filter(this.filter);
            for (const message of messages) {
                this.messagesEl.appendChild(this.renderMessage(message));
            }

            this.scrollToBottom();
        }

        /**
         * Checks whether the container element is scrolled all the way to the bottom.
         */
        isScrolledToBottom () {
            return this.containerEl.scrollTop === this.containerEl.scrollHeight - this.containerEl.clientHeight;
        }

        /**
         * Scrolls the container element all the way to the bottom.
         */
        scrollToBottom () {
            this.containerEl.scrollTop = this.containerEl.scrollHeight;
        }

        /**
         * Creates a DOM element for a message and caches it. Returns the cached element if present.
         * @param {object} message The message.
         * @return {HTMLSpanElement} The rendered message.
         */
        renderMessage (message) {
            if (message._cache) {
                return message._cache;
            }

            const msgDiv = document.createElement('div');
            msgDiv.classList.add('chat-message');
            if (message.system) {
                msgDiv.classList.add('chat-message-system');
            } else {
                msgDiv.classList.add('chat-message-' + message.role.toLowerCase());
                msgDiv.classList.add(message.isAllChat ? 'chat-message-all' : 'chat-message-team');
            }

            if (!message.system) {
                const msgName = document.createElement('span');
                msgName.classList.add('chat-message-name');
                msgName.textContent = message.senderName;
                msgDiv.appendChild(msgName);
            }

            const msgText = document.createElement('span');
            msgText.classList.add('chat-message-text');
            msgText.textContent = message.message;
            msgDiv.appendChild(msgText);

            message._cache = msgDiv;
            return msgDiv;
        };

        /**
         * Fetches the messages for the game from the API and replaces any stored messages with them.
         */
        fetch () {
            $.getJSON('${gameChat.chatApiUrl}')
                    .done(json => this.setMessages(json))
                    .fail(() => this.setMessages([Messages.SYSTEM_MESSAGE_FAILED_LOAD]));
        }

        /**
         * Filter to show all message.
         */
        static get FILTER_ALL () {
            return _ => true;
        }

        /**
         * Filter to show messages from the perspective of the attacker team.
         */
        static get FILTER_ATTACKERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== '${Role.DEFENDER}';
        }

        /**
         * Filter to show messages from the perspective of the defender team.
         */
        static get FILTER_DEFENDERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== '${Role.ATTACKER}';
        }

        /**
         * Message to show on WebSocket connect.
         */
        static get SYSTEM_MESSAGE_CONNECT () {
            return {system: true, message: 'Connected to chat.'};
        }

        /**
         * Message to show on WebSocket disconnect.
         */
        static get SYSTEM_MESSAGE_DISCONNECT () {
            return {system: true, message: 'Disconnected from chat.'};
        }

        /**
         * Message to show on failing to fetch the existing messages from the API.
         */
        static get SYSTEM_MESSAGE_FAILED_LOAD () {
            return {system: true, message: 'Could not load chat messages.'};
        }
    }

    /**
     * Stores the currently active message channel (team / all) and displays it.
     * The active message channel determines if chat messages are sent to the team or all players.
     */
    class Channel {
        /**
         * @param {HTMLSpanElement} channelElement The element displaying the channel
         */
        constructor (channelElement) {
            this.channelElement = channelElement;
            this.allChat = false;
            this.override = false;
            this.overrideValue = false;
        }

        /**
         * Set all chat or team chat as the active channel. Does nothing if override is enabled.
         * @param {boolean} isAllChat Whether to enable or disable all chat.
         */
        setAllChat (isAllChat) {
            const isAllChatBefore = this.isAllChat();
            if (!this.override) {
                this.allChat = isAllChat;
            }

            if (isAllChatBefore !== this.isAllChat()) {
                this.updateButton();
            }
        }

        /**
         * Override the message channel. Ignoring if all chat would otherwise be enabled or not.
         * @param {boolean} isAllChat Whether to override with all chat enabled or disabled.
         */
        overrideAllChat (isAllChat) {
            const isAllChatBefore = this.isAllChat();

            this.override = true;
            this.overrideValue = isAllChat;

            if (isAllChatBefore !== this.isAllChat()) {
                this.updateButton();
            }
        }

        /**
         * Removes the message channel override, returning to the previously set message channel.
         */
        removeOverride () {
            const isAllChatBefore = this.isAllChat();

            this.override = false;

            if (isAllChatBefore !== this.isAllChat()) {
                this.updateButton();
            }
        }

        /**
         * Updates the element displaying the message channel.
         */
        updateButton () {
            if (this.isAllChat()) {
                this.channelElement.textContent = Channel.CHANNEL_ALL;
            } else {
                this.channelElement.textContent = Channel.CHANNEL_TEAM;
            }
        }

        /**
         * Returns whether the message channel is set to all chat or team chat.
         * @return {boolean} Whether the message channel is set to all chat (true) or team chat (false).
         */
        isAllChat () {
            return this.override
                    ? this.overrideValue
                    : this.allChat;
        }

        static get CHANNEL_ALL () {
            return 'All';
        }

        static get CHANNEL_TEAM () {
            return 'Team';
        }
    }

    /**
     * Stores and displays the count of unread messages when the chat is hidden.
     */
    class MessageCount {
        /**
         * @param {HTMLSpanElement} countElement The element conaining the message count.
         */
        constructor(countElement) {
            this.countElement = countElement;
            this.count = 0;
        }

        /**
         * Sets the count to the given number and displays it.
         * @param {number} count The count to set.
         */
        setCount (count) {
            this.count = count;
            if (count > 0) {
                this.countElement.classList.remove('bg-secondary');
                this.countElement.classList.add('bg-warning');
            } else {
                this.countElement.classList.remove('bg-warning');
                this.countElement.classList.add('bg-secondary');
            }
            this.countElement.textContent = String(count);
        }

        getCount () {
            return this.count;
        }
    }

    /**
     * Manages the chat input textarea.
     */
    class ChatInput  {
        /**
         * @param {HTMLTextAreaElement} inputElement The text area serving as chat input.
         */
        constructor(inputElement) {
            this.inputElement = inputElement;
        }

        /**
         * Initializes the height and margin of the text area.
         * In order for this method to work, the text area has to be ready and visible.
         */
        init () {
            this.inputElement.value = '';
            this.resize();
        }

        /**
         *  Resizes the text area to its text. (adopted from https://stackoverflow.com/a/36958094/9360382).
         */
        resize () {
            const textarea = this.inputElement;

            /* Shrink the text area to one line. */
            textarea.style['height'] = '0px';

            /* Grow the text area. */
            const style = window.getComputedStyle(this.inputElement);
            const newHeight = textarea.scrollHeight         /* text height incl. padding */
                    + parseFloat(style.borderTopWidth)
                    + parseFloat(style.borderBottomWidth);
            const newMargin = textarea.clientHeight         /* actual height (1 line) incl. padding */
                    - textarea.scrollHeight                 /* text height incl. padding */;
            textarea.style['height'] = newHeight + 'px';
            textarea.style['margin-top'] = newMargin + 'px';
        }

        getText () {
            return this.inputElement.value;
        }

        setText (text) {
            this.inputElement.value = text;
            this.resize();
        }

        /**
         * If the text area's text starts with a command (a word prefixed with a '/'),
         * returns the word making up the command.
         */
        getCommand () {
            const text = this.getText().trimStart();
            const match = text.match(/^\/([a-zA-Z]+)/);
            if (match !== null) {
                return match[1];
            } else {
                return null;
            }
        }

        // TODO: Get the command string from the enum.
        static get COMMAND_ALL () {
            return 'all';
        }

        // TODO: Get the command string from the enum.
        static get COMMAND_TEAM () {
            return 'team';
        }
    }

    /**
     * Restores the previous visibility and position of the chat.
     */
    function loadSettings () {
        let showChat = JSON.parse(localStorage.getItem('showChat')) || false;
        let chatPos = JSON.parse(localStorage.getItem('chatPos'));

        const chat = document.getElementById('chat');

        if (showChat) {
            if (chatPos !== null) {
                chat.style.bottom = null;
                chat.style.right = null;
                chat.style.top = chatPos.top;
                chat.style.left = chatPos.left;
            }
            chat.removeAttribute('hidden');
        }
    }

    $(document).ready(function() {
        const chatElement = document.getElementById('chat');

        const input = new ChatInput(
                document.getElementById('chat-input'));

        /* Initialize the textarea heights needed for the resizing once the textarea is shown. */
        new MutationObserver((mutations, observer) => {
            for (const mutation of mutations) {
                if (mutation.type === 'attributes' && mutation.attributeName === 'hidden') {
                    setTimeout(input.init.bind(input), 0);
                    observer.disconnect();
                }
            }
        }).observe(chatElement, {attributes: true});

        const messages = new Messages(
                document.getElementById('chat-messages'),
                document.getElementById('chat-messages-container'));
        messages.fetch();

        const messageCount = new MessageCount(
                document.getElementById('chat-count'));
        messageCount.setCount(0);

        const channel = new Channel(
                document.getElementById('chat-channel'));
        channel.setAllChat(false);

        /* Resize after typing. Override message channel when "/all " or "/team " is entered. */
        $(input.inputElement).on('paste input', function() {
            input.resize();

            const command = input.getCommand();
            if (command !== null) {
                if (command === '${ChatCommand.ALL.commandString}') {
                    channel.overrideAllChat(true);
                    return
                } else if (command === '${ChatCommand.TEAM.commandString}') {
                    channel.overrideAllChat(false);
                    return;
                }
            }
            channel.removeOverride();
        });

        /* Submit message on enter. */
        $(input.inputElement).on('keypress', function(e) {
            if (e.keyCode === 13 && !e.shiftKey && !e.ctrlKey) {
                e.preventDefault();
                const message = input.getText().trim();
                if (message.length > 0) {
                    sendMessage(message, channel.isAllChat());
                    input.setText('');
                    channel.removeOverride();
                    messages.scrollToBottom();
                }
            }
        });

        /* Toggle message channel (all / team). */
        $('#chat-channel').on('click', function () {
            channel.setAllChat(!channel.isAllChat());
        });

        /* Filter messages based on the active tab. */
        $(chatElement).on('click', '.chat-tab-button', function () {
            $('#chat .chat-tab-button').removeClass('active');
            this.classList.add('active');
            switch (this.getAttribute('data-tab')) {
                case 'ALL':
                    messages.setFilter(Messages.FILTER_ALL);
                    break;
                case 'ATTACKERS':
                    messages.setFilter(Messages.FILTER_ATTACKERS);
                    break;
                case 'DEFENDERS':
                    messages.setFilter(Messages.FILTER_DEFENDERS);
                    break;
            }
            messages.redraw();
        });

        /* Toggle the chat and reset it's position when the indicator is clicked. */
        $("#chat-indicator").on('click', function () {
            if (!chatElement.hasAttribute('hidden')) {
                chatElement.setAttribute('hidden', '');
                localStorage.setItem('showChat', JSON.stringify(false))
            } else {
                chatElement.style.top = null;
                chatElement.style.right = null;
                chatElement.style.bottom = '0px';
                chatElement.style.left = '0px';
                messageCount.setCount(0);
                chatElement.removeAttribute('hidden');
                messages.scrollToBottom();
                localStorage.setItem('showChat', JSON.stringify(true))
            }
            localStorage.removeItem('chatPos');
        });

        /* Close chat when the X on the chat window is clicked. */
        $("#chat-close").on('click', function () {
            document.getElementById('chat').setAttribute('hidden', '');
            localStorage.setItem('showChat', JSON.stringify(false))
        });

        /* Make the chat window draggable. */
        $(chatElement).draggable({
            /* Reset bottom and right properties, because they
             * mess up the draggable, which uses top and left. */
            start: event => {
                event.target.style.bottom = null;
                event.target.style.right = null;
            },
            stop: event => {
                localStorage.setItem('chatPos',
                    JSON.stringify({
                        top: event.target.style.top,
                        left: event.target.style.left
                    })
                );
            },
            handle: '#chat-handle'
        });

        /**
         * Handles a received chat message.
         * @param {ServerGameChatEvent} serverChatEvent The received message.
         */
        const handleChatMessage = function (serverChatEvent) {
            if (!$(chatElement).is(':visible')) {
                messageCount.setCount(messageCount.getCount() + 1);
            }
            messages.addMessage({
                isAllChat: serverChatEvent.isAllChat,
                role: serverChatEvent.role,
                senderId: serverChatEvent.senderId,
                senderName: serverChatEvent.senderName,
                message: serverChatEvent.message
            });
        };

        /**
         * Handles a received system message.
         * @param {ServerSystemChatEvent} serverChatEvent The received message.
         */
        const handleSystemMessage = function (serverChatEvent) {
            messages.addMessage({
                system: true,
                message: serverChatEvent.message
            });
        };

        /**
         * Sends a message.
         * @param {object} message The message to be sent.
         * @param {boolean} isAllChat Whether the message should be sent to all players or the own team.
         */
        const sendMessage = function (message, isAllChat) {
            pushSocket.send('${eventNames.clientChatEventName}', {
                gameId: ${gameChat.gameId},
                allChat: isAllChat,
                message
            });
        };

        loadSettings();

        /* Register for WebSocket events. */
        pushSocket.subscribe('${eventNames.gameChatRegistrationEventName}', {
            gameId: ${gameChat.gameId}
        });
        pushSocket.register('${eventNames.serverChatEventName}', handleChatMessage);
        pushSocket.register('${eventNames.serverSystemChatEventName}', handleSystemMessage);
        pushSocket.register(PushSocket.WSEventType.CLOSE,
                () => messages.addMessage(Messages.SYSTEM_MESSAGE_DISCONNECT));
        pushSocket.register(PushSocket.WSEventType.OPEN, () =>
                () => messages.addMessage(Messages.SYSTEM_MESSAGE_CONNECT));
    });

})();
</script>

</c:if>
