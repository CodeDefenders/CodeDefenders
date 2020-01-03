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
<%@ page import="org.codedefenders.notification.events.EventNames" %>
<%@ page import="org.codedefenders.notification.events.client.registration.MutantProgressBarRegistrationEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantSubmittedEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantValidatedEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantCompiledEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantTestedEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantDuplicateCheckedEvent" %>

<%--
    Adds a JavaScript function mutantProgressBar() that inserts and updates a progressbar showing the status of the last
    submitted mutant. The progressbar is inserted after #logout. It gets progressbar updates from the WebSocket.

    @param Integer gameId
        The id of the game.
--%>

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {

        const onMutantSubmitted = function (data) {
            setProgress(33, 'Validating Mutant');
        };

        const onMutantValidated = function (data) {
            if (data.success) {
                setProgress(50, 'Checking For Duplicate Mutants');
            } else {
                setProgress(100, 'Mutant Is Not Valid');
            }
        };

        const onDuplicateChecked = function (data) {
            if (data.success) {
                setProgress(66, 'Compiling Mutant');
            } else {
                setProgress(100, 'Found Duplicate Mutant');
            }
        };

        const onMutantCompiled = function (data) {
            if (data.success) {
                setProgress(83, 'Running Tests Against Mutant');
            } else {
                setProgress(100, 'Mutant Did Not Compile');
            }
        };

        const onMutantTested = function (data) {
            setProgress(100, 'Done');
            <%--
            if (data.survived) {
                setProgress(100, 'Mutant Survived');
            } else {
                setProgress(100, 'Mutant Killed');
            }
            --%>
        };

        const registerMutantProgressBar = function () {
            pushSocket.subscribe('<%=EventNames.toClientEventName(MutantProgressBarRegistrationEvent.class)%>', {
                gameId: ${requestScope.gameId}
            });

            pushSocket.register('<%=EventNames.toServerEventName(MutantSubmittedEvent.class)%>', onMutantSubmitted);
            pushSocket.register('<%=EventNames.toServerEventName(MutantValidatedEvent.class)%>', onMutantValidated);
            pushSocket.register('<%=EventNames.toServerEventName(MutantDuplicateCheckedEvent.class)%>', onDuplicateChecked);
            pushSocket.register('<%=EventNames.toServerEventName(MutantCompiledEvent.class)%>', onMutantCompiled);
            pushSocket.register('<%=EventNames.toServerEventName(MutantTestedEvent.class)%>', onMutantTested);
        };

        <%--
        const unregisterMutantProgressBar = function () {
            pushSocket.unsubscribe('<%=EventNames.toClientEventName(MutantProgressBarRegistrationEvent.class)%>', {
                playerId: ${requestScope.playerId}
            });

            pushSocket.unregister('<%=EventNames.toServerEventName(MutantSubmittedEvent.class)%>', onMutantSubmitted);
            pushSocket.unregister('<%=EventNames.toServerEventName(MutantCompiledEvent.class)%>', onMutantCompiled);
            pushSocket.register('<%=EventNames.toServerEventName(MutantDuplicateCheckedEvent.class)%>', onDuplicateChecked);
            pushSocket.unregister('<%=EventNames.toServerEventName(MutantValidatedEvent.class)%>', onMutantValidated);
            pushSocket.unregister('<%=EventNames.toServerEventName(MutantTestedEvent.class)%>', onMutantTested);
        };
        --%>

        window.mutantProgressBar = function () {
            setProgress(16, 'Submitting Mutant');
            registerMutantProgressBar();

            /* Reconnect on close, because on Firefox the WebSocket connection gets closed on POST. */
            const reconnect = () => {
                pushSocket.reconnect();
                registerMutantProgressBar();
                pushSocket.unregister(PushSocket.WSEventType.CLOSE, reconnect);
            };
            pushSocket.register(PushSocket.WSEventType.CLOSE, reconnect);
        }

    })();
</script>
