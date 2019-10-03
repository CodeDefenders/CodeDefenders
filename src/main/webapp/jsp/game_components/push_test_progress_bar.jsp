<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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

    @param Integer gameId
        The id of the game.
--%>

<%-- TODO: put the progressbar into the html and just reference it here? --%>

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {

        let progress;
        let progressBar;

        function updateTestProgressBar (progress, text) {
            progressBar.setAttribute('aria-valuenow', progress);
            progressBar.style.width = progress + '%';
            progressBar.textContent = text;
        }

        const insertTestProgressBar = function () {
            progress = document.createElement('div');
            progress.classList.add('progress');
            progress.id = 'progress-bar';
            progress.style['height'] = '40px';
            progress.style['font-size'] = '30px';
            progress.innerHTML = `<div class="progress-bar" role="progressbar"
                style="font-size: 15px; line-height: 40px;"
                aria-valuemin="0" aria-valuemax="100"></div>`;
            progressBar = progress.children[0];

            updateTestProgressBar('16', 'Submitting Test');

            const form = document.getElementById('logout');
            form.parentNode.insertBefore(progressBar, form.nextSibling);
        };

        <%--
        function removeTestProgressBar () {
            progress.parentNode.removeChild(progress);
        }
        --%>

        const onTestSubmitted = function (data) {
            updateTestProgressBar('33', 'Compiling Test');
        };

        const onTestCompiled = function (data) {
            if (data.success) {
                updateTestProgressBar('50', 'Validating Test');
            } else {
                // TODO
            }
        };

        const onTestValidated = function (data) {
            if (data.success) {
                updateTestProgressBar('66', 'Running Test Against Mutants');
            } else {
                // TODO
            }
        };

        const onTestTestedOriginal = function (data) {
            if (data.survived) {
                updateTestProgressBar('83', 'Test Survived!');
            } else {
                // TODO
                updateTestProgressBar('83', 'Test was Killed!');
            }
        };

        const onTestTestedMutants = function (data) {
            if (data.numKilled > 0) {
                updateTestProgressBar('100', 'Test Killed ' + data.numKilled + ' Mutants!');
            } else {
                // TODO
                updateTestProgressBar('100', 'Test Did Not Kill Any Mutants Yet!');
            }
        };

        const registerTestProgressBar = function () {
            pushSocket.subscribe('<%=EventNames.toClientEventName(TestProgressBarRegistrationEvent.class)%>', {
                gameId: ${requestScope.gameId}
            });

            pushSocket.register('<%=EventNames.toServerEventName(TestSubmittedEvent.class)%>',      onTestSubmitted);
            pushSocket.register('<%=EventNames.toServerEventName(TestCompiledEvent.class)%>',       onTestCompiled);
            pushSocket.register('<%=EventNames.toServerEventName(TestValidatedEvent.class)%>',      onTestValidated);
            pushSocket.register('<%=EventNames.toServerEventName(TestTestedOriginalEvent.class)%>', onTestTestedOriginal);
            pushSocket.register('<%=EventNames.toServerEventName(TestTestedMutantsEvent.class)%>',  onTestTestedMutants);
        };

        <%--
        const unregisterTestProgressBar = function () {
            pushSocket.unsubscribe('<%=EventNames.toClientEventName(TestProgressBarRegistrationEvent.class)%>', {
                playerId: ${requestScope.playerId}
            });

            pushSocket.unregister('<%=EventNames.toServerEventName(TestSubmittedEvent.class)%>',      onTestSubmitted);
            pushSocket.unregister('<%=EventNames.toServerEventName(TestCompiledEvent.class)%>',       onTestCompiled);
            pushSocket.unregister('<%=EventNames.toServerEventName(TestValidatedEvent.class)%>',      onTestValidated);
            pushSocket.unregister('<%=EventNames.toServerEventName(TestTestedOriginalEvent.class)%>', onTestTestedOriginal);
            pushSocket.unregister('<%=EventNames.toServerEventName(TestTestedMutantsEvent.class)%>',  onTestTestedMutants);
        };
        --%>

        window.testProgressBar = function () {
            insertTestProgressBar();
            registerTestProgressBar();
        };
    })();
</script>


