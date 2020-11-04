<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="gameId" type="java.lang.Integer" required="true" %>
<%@ attribute name="registrationEventName" type="java.lang.String" required="true" %>
<%@ attribute name="serverChatEventName" type="java.lang.String" required="true" %>
<%@ attribute name="clientChatEventName" type="java.lang.String" required="true" %>

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

<!-- Change z-index to something reasonable later. -->
<div id="chat" style="position: fixed; left: 0px; bottom: 0px; z-index: 10001;">
    <div class="panel panel-default" style="margin: 0px;">
        <div id="chat-handle" class="panel-heading">
            <button type="button" data-tab="ALL" class="chat-tab-button btn btn-xs btn-default active">All</button>
            <button type="button" data-tab="ATTACKERS" class="chat-tab-button btn btn-xs btn-danger">Atackers</button>
            <button type="button" data-tab="DEFENDERS" class="chat-tab-button btn btn-xs btn-primary">Defenders</button>
            <button id="chat-close" type="button" class="close" style="margin-top: -.5em; margin-right: -.5em;">×</button>
        </div>
        <div class="panel-body" style="padding: 0px;">
            <div id="chat-messages-container" style="height: 30em; width: 25em; overflow-y: scroll;">
                <div id="chat-messages" style="padding: .5em .75em .5em .75em;"></div>
            </div>
        </div>
        <div class="panel-footer">
            <form class="form-inline" style="margin: 0px;">
                <div class="form-group" style="width: 100%; margin: 0px;">
                    <!-- Change this to type="text" once we get rid of base.css. -->
                    <div class="input-group" style="width: 100%;">
                        <div class="input-group-addon" style="cursor: pointer; width: 4.5em;">
                            <span id="chat-channel">
                                Team
                            </span>
                        </div>
                        <textarea id="chat-input" class="form-control" placeholder="Message" style="width: 100%; resize: none;"></textarea>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Change z-index to something reasonable later. -->
<div id="chat-indicator" style="position: fixed; left: 0px; bottom: -1px; z-index: 10000; cursor: pointer;">
    <div class="panel panel-default" style="margin: 0px; border-top-left-radius: 0px; border-bottom: none;">
        <div class="panel-heading" style="padding-top: .5em; padding-bottom: .4em; padding-left: .5em; padding-right: .5em; border-bottom: none;">
            Chat&nbsp;&nbsp;<span id="chat-count" class="label label-default" style="padding-top: .5em;">0</span>
        </div>
    </div>
</div>

<script>
(function () {

    class Messages {
        constructor (messagesEl, containerEl) {
            this.messages = [];
            this.filter = this.FILTER_ALL;
            this.messagesEl = messagesEl;
            this.containerEl = containerEl;
        }

        setFilter (filter) {
            this.filter = filter;
            this.redraw();
        }

        setMessages (messages) {
            if (messages.length > this.MESSAGE_LIMIT) {
                this.messages = messages.slice(messages.length - this.MESSAGE_LIMIT, messages.length);
            } else {
                this.messages = [...messages];
            }
            this.redraw();
        }

        addMessage (message) {
            this.messages.push(message);
            if (this.messages.length > this.MESSAGE_LIMIT) {
                this.messages.shift();
            }

            if (this.filter(message)) {
                const scrolledToBottom = this.containerEl.scrollTop === this.containerEl.scrollHeight - this.containerEl.clientHeight;

                this.messagesEl.appendChild(this.renderMessage(message));

                if (scrolledToBottom) {
                    this.containerEl.scrollTop = this.containerEl.scrollHeight;
                }
            }
        }

        redraw () {
            this.messagesEl.innerHTML = '';

            const messages = this.messages.filter(this.filter);
            for (const message of messages) {
                this.messagesEl.appendChild(this.renderMessage(message));
            }

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

        get MESSAGE_LIMIT () {
            return 1000;
        }

        get FILTER_ALL () {
            return message => true;
        }

        get FILTER_ATTACKERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== 'DEFENDER';
        }

        get FILTER_DEFENDERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== 'ATTACKER';
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

            if (this.isAllChatBefore !== this.isAllChat()) {
                this.setButton();
            }
        }

        overrideAllChat (isAllChat) {
            const isAllChatBefore = this.isAllChat();

            this.override = true;
            this.overrideValue = isAllChat;

            if (this.isAllChatBefore !== this.isAllChat()) {
                this.setButton();
            }
        }

        removeOverride () {
            const isAllChatBefore = this.isAllChat();

            this.override = false;

            if (this.isAllChatBefore !== this.isAllChat()) {
                this.setButton();
            }
        }

        setButton () {
            if (this.isAllChat()) {
                this.channelElement.textContent = this.CHANNEL_ALL;
            } else {
                this.channelElement.textContent = this.CHANNEL_TEAM;
            }
        }

        isAllChat () {
            return this.override
                    ? this.overrideValue
                    : this.allChat;
        }

        get CHANNEL_ALL () {
            return 'All';
        }

        get CHANNEL_TEAM () {
            return 'Team';
        }

        get COMMAND_ALL_PREFIX () {
            return '/all ';
        }

        get COMMAND_TEAM_PREFIX () {
            return '/team ';
        }
    }

    $(document).ready(function() {
        /* Initialize the message list. */
        const messages = new Messages(document.getElementById('chat-messages'), document.getElementById('chat-messages-container'));
        // messages.setMessages(messagesList);

        /* Initialize the channel (all/team). */
        const channel = new Channel(document.getElementById('chat-channel'));
        channel.setAllChat(false);

        /* Initialize text area. */
        const textarea = document.getElementById('chat-input');
        const $textarea = $(textarea);
        textarea.value = '';

        /* Resize text area as text is typed or deleted (from stackoverflow.com/a/36958094/9360382). */
        const offset = textarea.clientHeight - $textarea.height();
        $textarea.height(1);
        $textarea.height(textarea.scrollHeight - offset);
        const emptyHeight = $textarea.height();
        const resizeTextArea = function () {
            console.log('offset', offset,
                    'emptyHeight', emptyHeight,
                    'clientHeight', textarea.clientHeight,
                    'scrollHeight', textarea.scrollHeight,
                    'height', $textarea.height());

            /* Shrink the field and then re-set it to the scroll height in case it needs to shrink. */
            if (textarea.clientHeight >= textarea.scrollHeight) {
                $textarea.height(1);
            }

            /* Grow the field. */
            const height = Math.max(textarea.scrollHeight - offset, emptyHeight);
            $textarea.height(height);
            console.log(emptyHeight - height);
            $textarea.css('margin-top', emptyHeight - height);
            // textarea.style['margin-top'] = emptyHeight - height;
        }
        $(textarea).on('paste input', resizeTextArea);

        /* Submit message on enter. */
        $(textarea).on('keypress', function(e) {
            if (e.keyCode === 13) {
                if (!e.shiftKey && !e.ctrlKey) {
                    e.preventDefault();
                    const message = this.value.trim();
                    if (message.length > 0) {
                        sendMessage(message, channel.isAllChat());
                        this.value = '';
                        channel.removeOverride();
                        resizeTextArea();
                    }
                }
            }
        });

        /* Override message channel when "/all " or "/team " is entered. */
        $(textarea).on('paste input', function() {
            const hasSlashAll = this.value.startsWith(channel.COMMAND_ALL_PREFIX);
            const hasSlashTeam = this.value.startsWith(channel.COMMAND_TEAM_PREFIX);

            if (hasSlashAll) {
                channel.overrideAllChat(true);
            } else if (hasSlashTeam) {
                channel.overrideAllChat(false);
            } else {
                channel.removeOverride();
            }
        });

        /* Toggle message channel when the indicator is clicked. */
        $("#chat-channel").parent().on('click', function () {
            channel.setAllChat(!channel.isAllChat());
        });

        /* Filter messages based on the active tab. */
        $('#chat').on('click', '.chat-tab-button', function () {
            $('#chat .chat-tab-button').removeClass('active');
            this.classList.add('active');

            switch (this.getAttribute('data-tab')) {
                case 'ALL':
                    messages.setFilter(messages.FILTER_ALL);
                    break;
                case 'ATTACKERS':
                    messages.setFilter(messages.FILTER_ATTACKERS);
                    break;
                case 'DEFENDERS':
                    messages.setFilter(messages.FILTER_DEFENDERS);
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
            document.getElementById('chat-count').textContent = '0';
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
                const count = document.getElementById('chat-count');
                count.textContent = Number(count.textContent) + 1;
            }

            messages.addMessage({
                isAllChat: serverChatEvent.isAllChat,
                role: serverChatEvent.role,
                senderId: serverChatEvent.senderId,
                senderName: serverChatEvent.senderName,
                message: serverChatEvent.message
            });

            console.log('received message', serverChatEvent);
        };

        const sendMessage = function (message, isAllChat) {
            if (message.startsWith(channel.COMMAND_ALL_PREFIX)) {
                isAllChat = true;
                message = message.substring(channel.COMMAND_ALL_PREFIX.length);
            } else if (message.startsWith(channel.COMMAND_TEAM_PREFIX)) {
                isAllChat = false;
                message = message.substring(channel.COMMAND_TEAM_PREFIX.length);
            }

            pushSocket.send('${clientChatEventName}', {
                gameId: ${gameId},
                allChat: isAllChat,
                message
            });

            console.log('sent message', message, isAllChat);
        }

        pushSocket.subscribe('${registrationEventName}', {
            gameId: ${gameId}
        });
        pushSocket.register('${serverChatEventName}', handleMessage);
    });

})();
</script>
