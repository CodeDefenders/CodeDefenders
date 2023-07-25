<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="gameChat" type="org.codedefenders.beans.game.GameChatBean"--%>

<c:if test="${gameChat.chatEnabled}">

<link href="${url.forPath("/css/specific/game_chat.css")}" rel="stylesheet">

<div id="chat" class="rounded-3 shadow-sm" style="position: fixed; left: 0; bottom: 0; z-index: 11;" hidden>
    <div class="card m-0">
        <c:choose>
            <c:when test="${gameChat.showTabs}">
                <div id="chat-handle" class="card-header p-1 ps-2 d-flex align-items-center gap-2">
                    <button type="button" data-tab="ALL" class="chat-tab-button btn btn-xs btn-outline-success active"
                            title="Show all messages.">All</button>
                    <button type="button" data-tab="ATTACKERS" class="chat-tab-button btn btn-xs btn-outline-danger"
                            title="Show messages from the perspective of the attacker team.">Attackers</button>
                    <button type="button" data-tab="DEFENDERS" class="chat-tab-button btn btn-xs btn-outline-primary"
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
        <div class="card-footer p-2">
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

<script type="module">
    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
    import {GameChat} from '${url.forPath("/js/codedefenders_game.mjs")}';


    const gameId = ${gameChat.gameId};
    const messageLimit = ${gameChat.messageLimit};

    const gameChat = await new GameChat(gameId, messageLimit).initAsync();


    objects.register('gameChat', gameChat);
</script>

</c:if>
