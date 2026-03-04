<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
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
                            title="${i18n.tr('Show all messages.')}">${i18n.tr('All')}</button>
                    <button type="button" data-tab="ATTACKERS" class="chat-tab-button btn btn-xs btn-outline-danger"
                            title="${i18n.tr('Show messages from the perspective of the attacker team.')}">${i18n.tr('Attackers')}</button>
                    <button type="button" data-tab="DEFENDERS" class="chat-tab-button btn btn-xs btn-outline-primary"
                            title="${i18n.tr('Show messages from the perspective of the defender team.')}">${i18n.tr('Defenders')}</button>
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
                             title="${i18n.tr('Switch between sending messages to your own team or all players.')}">
                            ${i18n.tr('Team')}
                        </div>
                        <label class="visually-hidden" for="chat-input">${i18n.tr('Message')}</label>
                        <textarea type="text" id="chat-input" class="form-control"
                                  maxlength="${gameChat.maxMessageLength}"
                                  placeholder="${i18n.tr('Message')}"
                                  style="resize: none;"></textarea>
                    </div>
                </c:when>
                <c:otherwise>
                    <div>
                        <div id="chat-channel" hidden>
                            ${i18n.tr('Team')}
                        </div>
                        <label class="visually-hidden" for="chat-input">${i18n.tr('Message')}</label>
                        <textarea type="text" id="chat-input" class="form-control"
                                  maxlength="${gameChat.maxMessageLength}"
                                  placeholder="${i18n.tr('Message')}"
                                  style="resize: none;"></textarea>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<button type="button" id="chat-indicator" class="btn btn-sm btn-outline-secondary" aria-label="${i18n.tr('Chat')}">
    <i class="fa fa-comments"></i>
    ${i18n.tr('Chat')}
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
