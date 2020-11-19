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
<%@ page import="org.codedefenders.notification.events.client.registration.TestProgressBarRegistrationEvent" %>
<%@ page import="org.codedefenders.notification.events.server.test.TestSubmittedEvent" %>
<%@ page import="org.codedefenders.notification.events.server.test.TestCompiledEvent" %>
<%@ page import="org.codedefenders.notification.events.server.test.TestValidatedEvent" %>
<%@ page import="org.codedefenders.notification.events.server.test.TestTestedOriginalEvent" %>
<%@ page import="org.codedefenders.notification.events.server.test.TestTestedMutantsEvent" %>

<%--
    Adds a JavaScript function testProgressBar() that inserts and updates a progressbar showing the status of the last
    submitted test. The progressbar is inserted after #logout. It gets progressbar updates from the WebSocket.
--%>

<jsp:useBean id="testProgressBar" class="org.codedefenders.beans.game.TestProgressBarBean" scope="request"/>

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {

        const onTestSubmitted = function (e) {
            setProgress(33, 'Validating Test');
        };

        const onTestValidated = function (e) {
            if (e.success) {
                setProgress(50, 'Compiling Test');
            } else {
                setProgress(100, 'Test Is Not Valid');
            }
        };

        const onTestCompiled = function (e) {
            if (e.success) {
                setProgress(66, 'Running Test Against Original');
            } else {
                setProgress(100, 'Test Did Not Compile');
            }
        };

        const onTestTestedOriginal = function (e) {
            if (e.success) {
                setProgress(83, 'Running Test Against Mutants');
            } else {
                setProgress(100, 'Test Failed Against Original');
            }
        };

        const onTestTestedMutants = function (e) {
            setProgress(100, 'Done');
        };

        const registerTestProgressBar = function () {
            pushSocket.subscribe('<%=EventNames.toClientEventName(TestProgressBarRegistrationEvent.class)%>', {
                gameId: ${testProgressBar.gameId}
            });

            pushSocket.register('<%=EventNames.toServerEventName(TestSubmittedEvent.class)%>', onTestSubmitted);
            pushSocket.register('<%=EventNames.toServerEventName(TestCompiledEvent.class)%>', onTestCompiled);
            pushSocket.register('<%=EventNames.toServerEventName(TestValidatedEvent.class)%>', onTestValidated);
            pushSocket.register('<%=EventNames.toServerEventName(TestTestedOriginalEvent.class)%>', onTestTestedOriginal);
            pushSocket.register('<%=EventNames.toServerEventName(TestTestedMutantsEvent.class)%>', onTestTestedMutants);
        };

        <%--
        const unregisterTestProgressBar = function () {
            pushSocket.unsubscribe('<%=EventNames.toClientEventName(TestProgressBarRegistrationEvent.class)%>', {
                gameId: ${requestScope.gameId}
            });

            pushSocket.unregister('<%=EventNames.toServerEventName(TestSubmittedEvent.class)%>', onTestSubmitted);
            pushSocket.unregister('<%=EventNames.toServerEventName(TestCompiledEvent.class)%>', onTestCompiled);
            pushSocket.unregister('<%=EventNames.toServerEventName(TestValidatedEvent.class)%>', onTestValidated);
            pushSocket.unregister('<%=EventNames.toServerEventName(TestTestedOriginalEvent.class)%>', onTestTestedOriginal);
            pushSocket.unregister('<%=EventNames.toServerEventName(TestTestedMutantsEvent.class)%>', onTestTestedMutants);
        };
        --%>

        window.testProgressBar = function () {
            setProgress(16, 'Submitting Test');
            registerTestProgressBar();

            /* Reconnect on close, because on Firefox the WebSocket connection gets closed on POST. */
            const reconnect = () => {
                pushSocket.unregister(PushSocket.WSEventType.CLOSE, reconnect);
                pushSocket.reconnect();
                registerTestProgressBar();
            };
            // pushSocket.register(PushSocket.WSEventType.CLOSE, reconnect);
        };

    })();
</script>


