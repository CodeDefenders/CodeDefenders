/* Wrap in a function to avoid polluting the global scope. */
(function () {

class GameChat {
    /**
     * @param gameId
     *      Given by [${gameChat.gameId}]
     * @param apiUrl
     *      Given by ['${gameChat.chatApiUrl}']
     * @param messageLimit
     *      Given by [${gameChat.messageLimit}]
     */
    constructor (gameId, apiUrl, messageLimit) {
        this.gameId = gameId;
        this.apiUrl = apiUrl;
        this.messageLimit = messageLimit;

        /* Used DOM elements. */
        this.chatElement = null;
        this.handleElement = null;
        this.closeButton = null;
        this.messagesContainerElement = null;
        this.messagesElement = null;
        this.channelElement = null;
        this.inputElement = null;
        this.countElement = null;
        this.indicatorElement = null;

        /* Instances of the inner classes. */
        this.messages = null;
        this.channel = null;
        this.messageCount = null;
        this.chatInput = null;

        this._init();
    }

    /**
     * Manages the stored messages and displays them.
     */
    static Messages = class Messages {
        /**
         * @param {GameChat} gameChat The instance of the outer class.
         */
        constructor (gameChat) {
            this.gameChat = gameChat;

            /**
             * Stored messages. Past messages are fetched on initialization; new messages are received via WebSocket.
             * @type {GameChatMessage[]}
             */
            this.messages = [];

            /**
             * Filter function that decides which messages are displayed.
             * @type {function(GameChatMessage): boolean}
             */
            this.filter = Messages.FILTER_ALL;
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
         * Replaces the messages with the given messages and displays the messages passing the currently set filter.
         * @param {GameChatMessage[]} messages The new messages.
         */
        setMessages (messages) {
            this.messages = [...messages];
            this.redraw();
        }

        /**
         * Adds new messages and displays the messages passing the currently set filter.
         * @param {GameChatMessage[]} messages The new messages.
         */
        addMessages (messages) {
            const wasScrolledToBottom = this.isScrolledToBottom();

            for (const message of messages) {
                this.messages.push(message);
                if (this.filter.call(this, message)) {
                    this.gameChat.messagesElement.appendChild(this.renderMessage(message));
                }
            }

            if (wasScrolledToBottom) {
                this.scrollToBottom();
            }
        }

        /**
         * Adds a new message and displays it if it passes the currently set filter.
         * @param {GameChatMessage} message The new messages.
         */
        addMessage (message) {
            this.messages.push(message);
            if (this.filter.call(this, message)) {
                const wasScrolledToBottom = this.isScrolledToBottom();
                this.gameChat.messagesElement.appendChild(this.renderMessage(message));
                if (wasScrolledToBottom) {
                    this.scrollToBottom();
                }
            }
        }

        /**
         * Clears the displayed messages and redraws them, filtering them according to the set filter.
         */
        redraw () {
            this.gameChat.messagesElement.innerHTML = '';

            const messages = this.messages.filter(this.filter);
            for (const message of messages) {
                this.gameChat.messagesElement.appendChild(this.renderMessage(message));
            }

            this.scrollToBottom();
        }

        /**
         * Checks whether the container element is scrolled all the way to the bottom.
         */
        isScrolledToBottom () {
            const container = this.gameChat.messagesContainerElement;
            return container.scrollTop === (container.scrollHeight - container.clientHeight);
        }

        /**
         * Scrolls the container element all the way to the bottom.
         */
        scrollToBottom () {
            const container = this.gameChat.messagesContainerElement;
            container.scrollTop = container.scrollHeight;
        }

        /**
         * Creates a DOM element for a message and caches it. Returns the cached element if present.
         * @param {GameChatMessage} message The message.
         * @return {HTMLDivElement} The rendered message.
         */
        renderMessage (message) {
            if (message._cache) {
                return message._cache;
            }

            const lowerCaseRole = message.system ? '' : message.role.toLowerCase();

            let messageRole;
            if (!message.system) {
                if (message.isAllChat) {
                    messageRole = 'All';
                } else {
                    // Capitalize role name.
                    messageRole = lowerCaseRole.charAt(0).toUpperCase() + lowerCaseRole.slice(1);
                }
            }

            const msgDiv = document.createElement('div');
            msgDiv.classList.add('chat-message');
            if (message.system) {
                msgDiv.classList.add('chat-message-system');
            } else {
                msgDiv.classList.add('chat-message-' + lowerCaseRole);
                msgDiv.classList.add(message.isAllChat ? 'chat-message-all' : 'chat-message-team');
            }

            if (!message.system) {
                const msgName = document.createElement('span');
                msgName.classList.add('chat-message-name');
                msgName.textContent = `[${messageRole}] ${message.senderName}: `;
                msgDiv.appendChild(msgName);
            }

            const msgText = document.createElement('span');
            msgText.classList.add('chat-message-text');
            msgText.textContent = message.message;
            msgDiv.appendChild(msgText);

            message._cache = msgDiv;
            return msgDiv;
        }

        /**
         * Fetches the messages for the game from the API and adds them.
         */
        async fetch () {
            const response = await fetch(this.gameChat.apiUrl, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            if (!response.ok) {
                this.addMessage(Messages.SYSTEM_MESSAGE_FAILED_LOAD);
            }
            const json = await response.json();
            this.addMessages(json);
        }

        /**
         * Filter to show all message.
         * @type {function(GameChatMessage): boolean}
         */
        static get FILTER_ALL () {
            return _ => true;
        }

        /**
         * Filter to show messages from the perspective of the attacker team.
         * @type {function(GameChatMessage): boolean}
         */
        static get FILTER_ATTACKERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== 'DEFENDER';
        }

        /**
         * Filter to show messages from the perspective of the defender team.
         * @type {function(GameChatMessage): boolean}
         */
        static get FILTER_DEFENDERS () {
            return message => message.system
                    || message.isAllChat
                    || message.role !== 'ATTACKER';
        }

        /**
         * Message to show on WebSocket connect.
         * @type {GameChatMessage}
         */
        static get SYSTEM_MESSAGE_CONNECT () {
            return {system: true, message: 'Connected to chat.'};
        }

        /**
         * Message to show on WebSocket disconnect.
         * @type {GameChatMessage}
         */
        static get SYSTEM_MESSAGE_DISCONNECT () {
            return {system: true, message: 'Disconnected from chat.'};
        }

        /**
         * Message to show on failing to fetch the existing messages from the API.
         * @type {GameChatMessage}
         */
        static get SYSTEM_MESSAGE_FAILED_LOAD () {
            return {system: true, message: 'Could not load chat messages.'};
        }
    }

    /**
     * Stores the currently active message channel (team / all) and displays it.
     * The active message channel determines if chat messages are sent to the team or all players.
     */
    static Channel = class Channel {
        /**
         * @param {GameChat} gameChat The instance of the outer class.
         */
        constructor (gameChat) {
            this.gameChat = gameChat;

            /**
             * Whether the channel is set to all-chat or team-chat.
             * @type {boolean}
             */
            this.allChat = false;

            /**
             * Whether the channel is currently overridden.
             * @type {boolean}
             */
            this.override = false;

            /**
             * Whether the channel is overridden with all-chat or team-chat.
             * @type {boolean}
             */
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
         * Sets the override by the command that is currently typed in the chat.
         * @param {?string} command The typed command.
         */
        setOverrideByCommand (command) {
            if (command === GameChat.ChatInput.COMMAND_ALL) {
                this.overrideAllChat(true);
                return
            }
            if (command === GameChat.ChatInput.COMMAND_TEAM) {
                this.overrideAllChat(false);
                return;
            }
            this.removeOverride();
        }

        /**
         * Updates the element displaying the message channel.
         */
        updateButton () {
            if (this.isAllChat()) {
                this.gameChat.channelElement.textContent = Channel.CHANNEL_ALL;
            } else {
                this.gameChat.channelElement.textContent = Channel.CHANNEL_TEAM;
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
    static MessageCount = class MessageCount {
        /**
         * @param {GameChat} gameChat The instance of the outer class.
         */
        constructor(gameChat) {
            this.gameChat = gameChat;

            /**
             * The count of unread messages.
             * @type {number}
             */
            this.count = 0;
        }

        /**
         * Sets the count to the given number and displays it.
         * @param {number} count The count to set.
         */
        setCount (count) {
            const countElement = this.gameChat.countElement;

            this.count = count;
            if (count > 0) {
                countElement.classList.remove('bg-secondary');
                countElement.classList.add('bg-warning');
            } else {
                countElement.classList.remove('bg-warning');
                countElement.classList.add('bg-secondary');
            }
            countElement.textContent = String(count);
        }

        getCount () {
            return this.count;
        }
    }

    /**
     * Manages the chat input textarea.
     */
    static ChatInput = class ChatInput  {
        /**
         * @param {GameChat} gameChat The instance of the outer class.
         * @param {HTMLTextAreaElement} inputElement The text area serving as chat input.
         */
        constructor(gameChat, inputElement) {
            this.gameChat = gameChat;
        }

        /**
         * Initializes the height and margin of the text area.
         * In order for this method to work, the text area has to be ready and visible.
         */
        init () {
            this.gameChat.inputElement.value = '';
            this.resize();
        }

        /**
         *  Resizes the text area to its text. (adopted from https://stackoverflow.com/a/36958094/9360382).
         */
        resize () {
            const textarea = this.gameChat.inputElement;

            /* Shrink the text area to one line. */
            textarea.style['height'] = '0px';

            /* Grow the text area. */
            const style = window.getComputedStyle(textarea);
            const newHeight = textarea.scrollHeight         /* text height incl. padding */
                    + parseFloat(style.borderTopWidth)
                    + parseFloat(style.borderBottomWidth);
            const newMargin = textarea.clientHeight         /* actual height (1 line) incl. padding */
                    - textarea.scrollHeight                 /* text height incl. padding */;
            textarea.style['height'] = newHeight + 'px';
            textarea.style['margin-top'] = newMargin + 'px';
        }

        /**
         * Returns the input's text.
         * @return {string}
         */
        getText () {
            return this.gameChat.inputElement.value;
        }

        /**
         * Sets the input's text.
         * @param {string} text
         */
        setText (text) {
            this.gameChat.inputElement.value = text;
            this.resize();
        }

        /**
         * If the text area's text starts with a command (a word prefixed with a '/'),
         * returns the word making up the command.
         * @return {?string}
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
        /**
         * @type {string}
         */
        static get COMMAND_ALL () {
            return 'all';
        }

        // TODO: Get the command string from the enum.
        /**
         * @type {string}
         */
        static get COMMAND_TEAM () {
            return 'team';
        }
    }

    /**
     * Sends a message to the server.
     * @param {string} message The message to be sent.
     * @param {boolean} isAllChat Whether the message should be sent to all players or the own team.
     */
    sendMessage (message, isAllChat) {
        CodeDefenders.objects.pushSocket.send('chat.ClientGameChatEvent', {
            gameId: this.gameId,
            allChat: isAllChat,
            message
        });
    }

    /**
     * Handles a received chat message.
     * @param {ServerGameChatEvent} serverChatEvent The received message.
     */
    _onChatMessage (serverChatEvent) {
        if (this.chatElement.hasAttribute('hidden')) {
            this.messageCount.setCount(this.messageCount.getCount() + 1);
        }
        this.messages.addMessage({
            system: false,
            isAllChat: serverChatEvent.isAllChat,
            role: serverChatEvent.role,
            senderId: serverChatEvent.senderId,
            senderName: serverChatEvent.senderName,
            message: serverChatEvent.message
        });
    }

    /**
     * Handles a received system message.
     * @param {ServerSystemChatEvent} serverChatEvent The received message.
     */
    _onSystemMessage (serverChatEvent) {
        this.messages.addMessage({
            system: true,
            message: serverChatEvent.message
        });
    }

    _init () {
        this._initElements();
        this._initComponents();
        this._initEvents();
        this._loadSettings();
        this._initSocket();
    }

    /**
     * Retrieve all the elements we need.
     * @private
     */
    _initElements () {
        this.chatElement = document.getElementById('chat');
        this.handleElement = document.getElementById('chat-handle');
        this.closeButton = document.getElementById('chat-close');
        this.messagesContainerElement = document.getElementById('chat-messages-container');
        this.messagesElement = document.getElementById('chat-messages');
        this.channelElement = document.getElementById('chat-channel');
        this.inputElement = document.getElementById('chat-input');
        this.countElement = document.getElementById('chat-count');
        this.indicatorElement = document.getElementById('chat-indicator');
    }

    /**
     * Set up subclasses.
     * @private
     */
    _initComponents () {
        this.input = new GameChat.ChatInput(this, this.inputElement);
        /* Initialize the textarea heights needed for the resizing once the textarea is shown. */
        new MutationObserver((mutations, observer) => {
            for (const mutation of mutations) {
                if (mutation.type === 'attributes' && mutation.attributeName === 'hidden') {
                    setTimeout(this.input.init.bind(this.input), 0);
                    observer.disconnect();
                }
            }
        }).observe(this.chatElement, {attributes: true});

        this.messages = new GameChat.Messages(this);
        this.messages.fetch();

        this.messageCount = new GameChat.MessageCount(this);
        this.messageCount.setCount(0);

        this.channel = new GameChat.Channel(this);
        this.channel.setAllChat(false);
    }

    /**
     * Set up event listeners for user interaction.
     * @private
     */
    _initEvents () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Resize after typing. Override message channel when "/all " or "/team " is entered. */
        this.inputElement.addEventListener('input', function (event) {
            self.input.resize();
            self.channel.setOverrideByCommand(self.input.getCommand());
        });
        this.inputElement.addEventListener('paste', function (event) {
            self.input.resize();
            self.channel.setOverrideByCommand(self.input.getCommand());
        });

        /* Submit message on enter. */
        this.inputElement.addEventListener('keypress', function (event) {
            if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey) {
                event.preventDefault();
                const message = self.input.getText().trim();
                if (message.length > 0) {
                    self.sendMessage(message, self.channel.isAllChat());
                    self.input.setText('');
                    self.channel.removeOverride();
                    self.messages.scrollToBottom();
                }
            }
        });

        /* Toggle message channel (all / team). */
        this.channelElement.addEventListener('click', function (event) {
            self.channel.setAllChat(!self.channel.isAllChat());
        });

        /* Filter messages based on the active tab. */
        this.chatElement.addEventListener('click', function (event) {
            const tabButton = event.target.closest('.chat-tab-button');
            if (tabButton === null) {
                return;
            }

            for (const otherTabButton of self.chatElement.querySelectorAll('.chat-tab-button')) {
                otherTabButton.classList.remove('active');
            }

            tabButton.classList.add('active');
            switch (tabButton.getAttribute('data-tab')) {
                case 'ALL':
                    self.messages.setFilter(GameChat.Messages.FILTER_ALL);
                    break;
                case 'ATTACKERS':
                    self.messages.setFilter(GameChat.Messages.FILTER_ATTACKERS);
                    break;
                case 'DEFENDERS':
                    self.messages.setFilter(GameChat.Messages.FILTER_DEFENDERS);
                    break;
            }
            self.messages.redraw();
        });

        /* Toggle the chat and reset it's position when the indicator is clicked. */
        this.indicatorElement.addEventListener('click', function (event) {
            if (!self.chatElement.hasAttribute('hidden')) {
                self.chatElement.setAttribute('hidden', '');
                localStorage.setItem('showChat', JSON.stringify(false))
            } else {
                self.chatElement.style.top = null;
                self.chatElement.style.right = null;
                self.chatElement.style.bottom = '0px';
                self.chatElement.style.left = '0px';
                self.messageCount.setCount(0);
                self.chatElement.removeAttribute('hidden');
                self.messages.scrollToBottom();
                localStorage.setItem('showChat', JSON.stringify(true))
            }
            localStorage.removeItem('chatPos');
        });

        /* Close chat when the X on the chat window is clicked. */
        this.closeButton.addEventListener('click', function (event) {
            self.chatElement.setAttribute('hidden', '');
            localStorage.setItem('showChat', JSON.stringify(false))
        });

        /* Make the chat window draggable. */
        $(this.chatElement).draggable({
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
            handle: this.handleElement
        });
    }

    /**
     * Restore the previous visibility and position of the chat.
     * @private
     */
    _loadSettings () {
        let showChat = JSON.parse(localStorage.getItem('showChat')) || false;
        let chatPos = JSON.parse(localStorage.getItem('chatPos'));

        if (showChat) {
            if (chatPos !== null) {
                this.chatElement.style.bottom = null;
                this.chatElement.style.right = null;
                this.chatElement.style.top = chatPos.top;
                this.chatElement.style.left = chatPos.left;
            }
            this.chatElement.removeAttribute('hidden');
        }
    }

    /**
     * Register for WebSocket events.
     * @private
     */
    _initSocket () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        const PushSocket = CodeDefenders.classes.PushSocket;
        const pushSocket = CodeDefenders.objects.pushSocket;
        pushSocket.subscribe('registration.GameChatRegistrationEvent', {gameId: this.gameId});
        pushSocket.register('chat.ServerGameChatEvent', this._onChatMessage.bind(this));
        pushSocket.register('chat.ServerSystemChatEvent', this._onSystemMessage.bind(this));
        pushSocket.register(PushSocket.WSEventType.CLOSE,
                () => self.messages.addMessage(GameChat.Messages.SYSTEM_MESSAGE_DISCONNECT));
        pushSocket.register(PushSocket.WSEventType.OPEN, () =>
                () => self.messages.addMessage(GameChat.Messages.SYSTEM_MESSAGE_CONNECT));
        if (pushSocket.readyState === 1) {
            self.messages.addMessage(GameChat.Messages.SYSTEM_MESSAGE_CONNECT);
        }
    }

}

CodeDefenders.classes.GameChat ??= GameChat;

})();
