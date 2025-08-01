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

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>

<%--
    Provides common JS initialization for all pages.
--%>

<%-- Push notifications using WebSocket --%>
<jsp:include page="/jsp/push_socket.jsp"/>

<!-- Context path of server, so plain JS code (without JSP templating) can construct a correct url. -->
<script>
    const contextPath = "${url.forPath("/")}";
    const applicationURL = "${url.getAbsoluteURLForPath("/")}";
</script>

<!-- JS Init -->
<script type="module">
    import '${url.forPath("/js/codedefenders_init.mjs")}';
    import {objects, ShowToasts} from '${url.forPath("/js/codedefenders_main.mjs")}';

    /** @type {PushSocket} */
    const socket = await objects.await('pushSocket');

    (async function () {

        const userId = ${login.loggedIn ? login.userId : -1};
        if (userId >= 0) {
            socket.subscribe('registration.InviteRegistrationEvent', {
                userId: userId
            });
        }

        socket.register('invite.InviteEvent', event => {
            let extraElements;
            if (event.mayChooseRole) {
                const defenderButton = document.createElement('a');
                defenderButton.classList.add('btn', 'btn-defender');
                defenderButton.textContent = 'Defender';
                defenderButton.href = event.inviteLink + '&role=defender';

                const flexButton = document.createElement('a');
                flexButton.classList.add('btn', 'btn-secondary');
                flexButton.textContent = 'Any role';
                flexButton.href = event.inviteLink + '&role=flex';

                const attackerButton = document.createElement('a');
                attackerButton.classList.add('btn', 'btn-attacker');
                attackerButton.textContent = 'Attacker';
                attackerButton.href = event.inviteLink + '&role=attacker';

                extraElements = [defenderButton, flexButton, attackerButton];
            } else {
                const joinButton = document.createElement('a');
                joinButton.classList.add('btn');
                switch (event.role) {
                    case "DEFENDER":
                        joinButton.textContent = "Defender";
                        joinButton.classList.add("btn-defender");
                        break;
                    case "ATTACKER":
                        joinButton.textContent = "Attacker";
                        joinButton.classList.add("btn-attacker");
                        break;
                    case "FLEX":
                        joinButton.textContent = "Any role";
                        joinButton.classList.add("btn-secondary");
                    case null:
                        joinButton.textContent = "Player";
                        joinButton.classList.add("btn-player");
                }
                joinButton.href = event.inviteLink;
                extraElements = [joinButton];
            }
            ShowToasts.showToast({
                title: 'Invite received',
                body: `You have been invited to join a game. \n Join as: `,
                extraElements: extraElements,
                timeout: false
            });
        });
    })();
</script>
