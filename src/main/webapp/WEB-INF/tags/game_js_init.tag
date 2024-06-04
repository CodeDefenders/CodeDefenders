<%@ tag pageEncoding="UTF-8" %>

<%@ tag import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>

<%--
    Provides common JS initialization for game pages.
--%>


<%-- Push notifications using WebSocket --%>
<jsp:include page="/jsp/push_socket.jsp"/>

<%-- Recieve events from the server --%>
<script type="module">
    import {objects, AchievementNotifications} from '${url.forPath("/js/codedefenders_main.mjs")}';

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

        socket.subscribe('registration.AchievementRegistrationEvent', {
            userId: ${login.userId}
        });

        const achievementNotifications = new AchievementNotifications(
            "${url.forPath("/images/achievements/")}",
            "${url.forPath(Paths.USER_PROFILE)}"
        );

        socket.register('achievement.AchievementUnlockedEvent', event => {
            console.log('Achievement unlocked.', event);
            achievementNotifications.showAchievementNotification(event.achievement);
            socket.send('achievement.ClientAchievementNotificationShownEvent', {
                achievementId: event.achievement.achievementId
            });
        });
    })();
</script>
