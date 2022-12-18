<%--

    Copyright (C) 2016-2019 Code Defenders contributors

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

<%--@elvariable id="login" type="org.codedefenders.beans.user.LoginBean"--%>
<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>

</div> <%-- closes #game-container --%>

<script type="module">
    import {objects} from './js/codedefenders_main.mjs';

    (async function () {
        /** @type {PushSocket} */
        const socket = await objects.await('pushSocket');

        socket.subscribe('registration.GameLifecycleRegistrationEvent', {
            gameId: ${gameProducer.game.id},
            userId: ${login.userId}
        });

        socket.register('game.GameStoppedEvent', event => {
            console.log('Game with Id ' + event.gameId + ' was stopped.');
            window.location.reload();
        });

        socket.register('game.GameStartedEvent', event => {
            console.log('Game with Id ' + event.gameId + ' was started.');
            window.location.reload();
        });
    })();
</script>

<%@ include file="/jsp/footer.jsp" %>
