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

<%@ tag import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>

<%--
    Provides common JS initialization for game pages.
    Currently the WebSocket and achievement notifications.
--%>

<%-- Recieve events from the server --%>
<script type="module">
    import {objects, AchievementNotifications, ShowToasts} from '${url.forPath("/js/codedefenders_main.mjs")}';

    (async function () {
        /** @type {PushSocket} */
        const socket = await objects.await('pushSocket');

        // lifecycle events

        socket.subscribe('registration.GameLifecycleRegistrationEvent', {
            gameId: ${gameProducer.game.id},
            userId: ${login.userId}
        });

        socket.register('game.GameStoppedEvent', event => {
            console.log('Game with Id ' + event.gameId + ' was stopped.');
            window.location.reload();
        });

        socket.register('game.GameResolvedAllDuelsEvent', event => {
            console.log('Game with Id ' + event.gameId + ' has finished auto-resolving all ongoing equivalence duels.');
            window.location.reload();
        });

        socket.register('game.GameStartedEvent', event => {
            console.log('Game with Id ' + event.gameId + ' was started.');
            window.location.reload();
        });


        // equivalence duel events

        socket.subscribe('registration.EquivalenceDuelRegistrationEvent', {
            gameId: ${gameProducer.game.id},
            userId: ${login.userId}
        });

        socket.register('equivalence.EquivalenceDuelResultEvent', event => {
            if (event.defenderId === ${login.userId}) {
                if (!event.attackerWon) {
                    ShowToasts.showToast({
                        title: '${i18n.tr("Equivalence duel won!")}',
                        body: '${i18n.tr("Mutant {0} that you claimed was declared equivalent. The attacker lost all their points for the mutant and you gained an additional point.")}'.replaceAll('{0}', event.mutantId)
                    });
                } else {
                    ShowToasts.showToast({
                        title: '${i18n.tr("Equivalence duel lost!")}',
                        body: '${i18n.tr("Mutant {0} that you claimed was proven to be not equivalent. The attacker gained an additional point.")}'.replaceAll('{0}', event.mutantId)
                    });
                }
            }
        });


        // achievements

        socket.subscribe('registration.AchievementRegistrationEvent', {
            userId: ${login.userId}
        });

        socket.register('achievement.AchievementUnlockedEvent', event => {
            console.log('Achievement unlocked.', event);
            AchievementNotifications.showAchievementNotification(event.achievement,
                    "${url.forPath("/images/achievements/")}",
                    "${url.forPath(Paths.USER_PROFILE)}");
            socket.send('achievement.ClientAchievementNotificationShownEvent', {
                achievementId: event.achievement.achievementId
            });
        });
    })();
</script>
