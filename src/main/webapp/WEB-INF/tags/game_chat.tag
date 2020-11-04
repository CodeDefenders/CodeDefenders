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
                <button type="button" data-tab="ALL" class="chat-tab-button btn btn-xs btn-default active">All</button>
                <button type="button" data-tab="ATTACKERS" class="chat-tab-button btn btn-xs btn-danger">Attackers</button>
                <button type="button" data-tab="DEFENDERS" class="chat-tab-button btn btn-xs btn-primary">Defenders</button>
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
                            <span id="chat-channel">
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

    class Messages {
        constructor (messagesEl, containerEl) {
            this.messages = [];
            this.filter = Messages.FILTER_ALL;
            this.messagesEl = messagesEl;
            this.containerEl = containerEl;
        }

        setFilter (filter) {
            this.filter = filter;
            this.redraw();
        }

        setMessages (messages) {
            if (messages.length > Messages.MESSAGE_LIMIT) {
                this.messages = messages.slice(messages.length - Messages.MESSAGE_LIMIT, messages.length);
            } else {
                this.messages = [...messages];
            }
            this.redraw();
        }

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

        redraw () {
            this.messagesEl.innerHTML = '';

            const messages = this.messages.filter(this.filter);
            for (const message of messages) {
                this.messagesEl.appendChild(this.renderMessage(message));
            }

            this.scrollToBottom();
        }

        isScrolledToBottom () {
            return this.containerEl.scrollTop === this.containerEl.scrollHeight - this.containerEl.clientHeight;
        }

        scrollToBottom () {
            this.containerEl.scrollTop = this.containerEl.scrollHeight;
        }

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

        fetch () {
            $.getJSON('${gameChat.chatApiUrl}')
                    .done(json => this.setMessages(json))
                    .fail(() => this.setMessages([Messages.SYSTEM_MESSAGE_FAILED_LOAD]));
        }

        static get MESSAGE_LIMIT () {
            return 1000;
        }

        static get FILTER_ALL () {
            return _ => true;
        }

        static get FILTER_ATTACKERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== 'DEFENDER';
        }

        static get FILTER_DEFENDERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== 'ATTACKER';
        }

        static get SYSTEM_MESSAGE_CONNECT () {
            return {system: true, message: 'Connected to chat.'};
        }

        static get SYSTEM_MESSAGE_DISCONNECT () {
            return {system: true, message: 'Disconnected from chat.'};
        }

        static get SYSTEM_MESSAGE_FAILED_LOAD () {
            return {system: true, message: 'Could not load chat messages.'};
        }
    }

    class Channel {
        constructor (channelElement) {
            this.channelElement = channelElement;
            this.allChat = false;
            this.override = false;
            this.overrideValue = false;
        }

        setAllChat (isAllChat) {
            const isAllChatBefore = this.isAllChat();
            if (!this.override) {
                this.allChat = isAllChat;
            }

            if (isAllChatBefore !== this.isAllChat()) {
                this.updateButton();
            }
        }

        overrideAllChat (isAllChat) {
            const isAllChatBefore = this.isAllChat();

            this.override = true;
            this.overrideValue = isAllChat;

            if (isAllChatBefore !== this.isAllChat()) {
                this.updateButton();
            }
        }

        removeOverride () {
            const isAllChatBefore = this.isAllChat();

            this.override = false;

            if (isAllChatBefore !== this.isAllChat()) {
                this.updateButton();
            }
        }

        updateButton () {
            if (this.isAllChat()) {
                this.channelElement.textContent = Channel.CHANNEL_ALL;
            } else {
                this.channelElement.textContent = Channel.CHANNEL_TEAM;
            }
        }

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

    class MessageCount {
        constructor(countElement) {
            this.countElement = countElement;
            this.count = 0;
        }

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

    class ChatInput  {
        constructor(inputElement) {
            this.inputElement = inputElement;
            this.$inputElement = $(inputElement);
            this.init()
        }

        init () {
            const textarea = this.inputElement;
            const $textarea = this.$inputElement;

            textarea.value = '';
            this.offset = textarea.clientHeight - $textarea.height();

            $textarea.height(1);
            $textarea.height(textarea.scrollHeight - this.offset);
            this.emptyHeight = $textarea.height();
        }

        /* Resize text area as text is typed or deleted (from stackoverflow.com/a/36958094/9360382). */
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

        getCommand () {
            const text = this.getText().trimStart();
            const match = text.match(/^\/([a-zA-Z]+)/);
            if (match !== null) {
                return match[1];
            } else {
                return null;
            }
        }

        static get COMMAND_ALL () {
            return 'all';
        }

        static get COMMAND_TEAM () {
            return 'team';
        }
    }

    $(document).ready(function() {
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

        const input = new ChatInput(
                document.getElementById('chat-input'));

        /* Resize after typing. Override message channel when "/all " or "/team " is entered. */
        $(input.inputElement).on('paste input', function() {
            input.resize();

            const command = input.getCommand();
            if (command !== null) {
                if (command === ChatInput.COMMAND_ALL) {
                    channel.overrideAllChat(true);
                } else if (command === ChatInput.COMMAND_TEAM) {
                    channel.overrideAllChat(false);
                }
            } else {
                channel.removeOverride();
            }
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
            $(chat).toggle();
            chat.style.left = '0px';
            chat.style.right = null;
            chat.style.top = null;
            chat.style.bottom = '0px';
            messageCount.setCount(0);
            messages.scrollToBottom();
        });

        /* Close chat when the X on the chat window is clicked. */
        $("#chat-close").on('click', function () {
            $("#chat").hide();
        });

        /* Make the chat window draggable. */
        $("#chat").draggable({
            /* Reset bottom and right properties, because they
             * mess up the draggable, which uses top and left. */
            start: event => {
                event.target.style.bottom = null;
                event.target.style.right = null;
            },
            handle: '#chat-handle'
        });

        const handleMessage = function (serverChatEvent) {
            if (!$('#chat').is(':visible')) {
                messageCount.setCount(messageCount.getCount());
            }
            messages.addMessage({
                isAllChat: serverChatEvent.isAllChat,
                role: serverChatEvent.role,
                senderId: serverChatEvent.senderId,
                senderName: serverChatEvent.senderName,
                message: serverChatEvent.message
            });
        };

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
        pushSocket.register('${eventNames.serverChatEventName}', handleMessage);
        pushSocket.register(PushSocket.WSEventType.CLOSE,
                () => messages.addMessage(Messages.SYSTEM_MESSAGE_DISCONNECT));
        pushSocket.register(PushSocket.WSEventType.OPEN, () =>
                () => messages.addMessage(Messages.SYSTEM_MESSAGE_CONNECT));
    });

})();
</script>

</c:if>
