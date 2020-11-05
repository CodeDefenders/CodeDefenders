<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="gameChat" type="org.codedefenders.beans.game.GameChatBean"--%>
<%--@elvariable id="eventNames" type="org.codedefenders.beans.notification.EventNamesBean"--%>

<c:if test="${gameChat.chatEnabled}">

<style>
    #chat .chat-message {
        padding-top: 2px;
        padding-bottom: 2px;
    }

    /* Message decorations. */
    #chat .chat-message-all .chat-message-name::before {
        content: "[ALL]";
        padding-right: .25em;
    }
    #chat .chat-message .chat-message-name::after {
        content: ":";
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

<div id="chat" style="position: fixed; left: 0; bottom: 0; z-index: 11;">
    <div class="panel panel-default" style="margin: 0;">
        <div id="chat-handle" class="panel-heading">
            <c:if test="${gameChat.showTabs}">
                <button type="button" data-tab="ALL" class="chat-tab-button btn btn-xs btn-default active"
                    title="Show all message.">All</button>
                <button type="button" data-tab="ATTACKERS" class="chat-tab-button btn btn-xs btn-danger"
                    title="Show messages from the perspective of the attacker team.">Attackers</button>
                <button type="button" data-tab="DEFENDERS" class="chat-tab-button btn btn-xs btn-primary"
                    title="Show messages from the perspective of the defender team.">Defenders</button>
            </c:if>
            <button id="chat-close" type="button" class="close" style="margin-top: -.5em; margin-right: -.5em;">×</button>
        </div>
        <div class="panel-body" style="padding: 0px;">
            <div id="chat-messages-container" style="height: 30em; width: 25em; overflow-y: scroll;">
                <div id="chat-messages" style="word-wrap: break-word; padding: .5em .75em .5em .75em;"></div>
            </div>
        </div>
        <div class="panel-footer">
            <form class="form-inline" style="margin: 0;">
                <div class="form-group" style="width: 100%; margin: 0;">
                    <!-- Change this to type="text" once we get rid of base.css. -->
                    <div class="input-group" style="width: 100%;">
                        <div id="chat-channel-container" class="input-group-addon" style="cursor: pointer; width: 4.5em;">
                            <span id="chat-channel" title="Switch between sending messages to your own team or all players.">
                                Team
                            </span>
                        </div>
                        <textarea id="chat-input" class="form-control" maxlength="${gameChat.maxMessageLength}" placeholder="Message" style="width: 100%; resize: none;"></textarea>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>

<div id="chat-indicator" style="position: fixed; left: 0; bottom: -1px; z-index: 10; cursor: pointer;">
    <div class="panel panel-default" style="margin: 0; border-top-left-radius: 0; border-bottom: none;">
        <div class="panel-heading" style="padding: .5em .7em .4em .5em; border-bottom: none;">
            Chat&nbsp;&nbsp;<span id="chat-count" class="label label-default" style="padding-top: .5em;">0</span>
        </div>
    </div>
</div>

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
            if (messages.length > Messages.MESSAGE_LIMIT) {
                this.messages = messages.slice(messages.length - Messages.MESSAGE_LIMIT, messages.length);
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
            if (this.messages.length > Messages.MESSAGE_LIMIT) {
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
         * The maximum number of messages to display.
         */
        static get MESSAGE_LIMIT () {
            return 1000;
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
                    || message.role !== 'DEFENDER';
        }

        /**
         * Filter to show messages from the perspective of the defender team.
         */
        static get FILTER_DEFENDERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== 'ATTACKER';
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
                this.countElement.classList.remove('label-default');
                this.countElement.classList.add('label-warning');
            } else {
                this.countElement.classList.remove('label-warning');
                this.countElement.classList.add('label-default');
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
            this.$inputElement = $(inputElement);
            this.init()
        }

        /**
         * Initializes the empty height and height offset of the text area.
         * In order for this method to work, the text area has to be ready and visible.
         */
        init () {
            const textarea = this.inputElement;
            const $textarea = this.$inputElement;

            textarea.value = '';
            this.offset = textarea.clientHeight - $textarea.height();

            $textarea.height(1);
            $textarea.height(textarea.scrollHeight - this.offset);
            this.emptyHeight = $textarea.height();
        }

        /**
         *  Resizes the text area to its text. (from stackoverflow.com/a/36958094/9360382).
         */
        resize () {
            const textarea = this.inputElement;
            const $textarea = this.$inputElement;

            /* Shrink the field and then re-set it to the scroll height in case it needs to shrink. */
            if (textarea.clientHeight >= textarea.scrollHeight) {
                $textarea.height(1);
            }

            /* Grow the field. */
            const height = Math.max(textarea.scrollHeight - this.offset, this.emptyHeight);
            $textarea.height(height);
            $textarea.css('margin-top', this.emptyHeight - height);
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

        // TODO: Create a bean for enum constants.
        static get COMMAND_ALL () {
            return 'all';
        }

        // TODO: Create a bean for enum constants.
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
        const $chat = $(chat);

        if (showChat) {
            if (chatPos !== null) {
                chat.style.bottom = null;
                chat.style.right = null;
                chat.style.top = chatPos.top;
                chat.style.left = chatPos.left;
            }
        } else {
            $chat.hide();
        }
    }

    $(document).ready(function() {
        const input = new ChatInput(
                document.getElementById('chat-input'));

        /* Load settings as early as possible, but after initializing the text area,
        because the text area has to be visible to get correct height values from it. */
        loadSettings();

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
                if (command === ChatInput.COMMAND_ALL) {
                    channel.overrideAllChat(true);
                    return
                } else if (command === ChatInput.COMMAND_TEAM) {
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
        $('#chat-channel-container').on('click', function () {
            channel.setAllChat(!channel.isAllChat());
        });

        /* Filter messages based on the active tab. */
        $('#chat').on('click', '.chat-tab-button', function () {
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
            const chat = document.getElementById('chat');
            if ($(chat).is(':visible')) {
                $(chat).hide();
                localStorage.setItem('showChat', JSON.stringify(false))
            } else {
                chat.style.top = null;
                chat.style.right = null;
                chat.style.bottom = '0px';
                chat.style.left = '0px';
                messageCount.setCount(0);
                $(chat).show();
                messages.scrollToBottom();
                localStorage.setItem('showChat', JSON.stringify(true))
            }
            localStorage.removeItem('chatPos');
        });

        /* Close chat when the X on the chat window is clicked. */
        $("#chat-close").on('click', function () {
            $("#chat").hide();
            localStorage.setItem('showChat', JSON.stringify(false))
        });

        /* Make the chat window draggable. */
        $("#chat").draggable({
            /* Reset bottom and right properties, because they
             * mess up the draggable, which uses top and left. */
            start: event => {
                event.target.style.bottom = null;
                event.target.style.right = null;
            },
            stop: event => {
                console.log(event);
                console.log(event.target.style.bottom);
                console.log(event.target.style.right);
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
            if (!$('#chat').is(':visible')) {
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
