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
<%@ page import="org.codedefenders.notification.events.client.registration.MutantProgressBarRegistrationEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantSubmittedEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantValidatedEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantCompiledEvent" %>
<%@ page import="org.codedefenders.notification.events.server.mutant.MutantTestedEvent" %>

<%--
    Adds a JavaScript function mutantProgressBar() that inserts and updates a progressbar showing the status of the last
    submitted mutant. The progressbar is inserted after #logout. It gets progressbar updates from the WebSocket.

    @param Integer gameId
        The id of the game.
--%>

<%-- TODO: put the progressbar into the html and just reference it here? --%>

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {

        let progress;
        let progressBar;

        function updateMutantProgressBar (progress, text) {
            progressBar.setAttribute('aria-valuenow', progress);
            progressBar.style.width = progress + '%';
            progressBar.textContent = text;
        }

        const insertMutantProgressBar = function () {
            progress = document.createElement('div');
            progress.classList.add('progress');
            progress.id = 'progress-bar';
            progress.style['height'] = '40px';
            progress.style['font-size'] = '30px';
            progress.style['margin'] = '5px';
            progress.innerHTML = `<div class="progress-bar" role="progressbar"
                style="font-size: 15px; line-height: 40px;"
                aria-valuemin="0" aria-valuemax="100"></div>`;
            progressBar = progress.children[0];

            updateMutantProgressBar('16', 'Submitting Test');

            const form = document.getElementById('logout');
            form.parentNode.insertBefore(progress, form.nextSibling);
        };

        <%--
        function removeMutantProgressBar () {
            progress.parentNode.removeChild(progress);
        }
        --%>

        const onMutantSubmitted = function (data) {
            updateMutantProgressBar('40', 'Validating Mutant');
        };

        const onMutantValidated = function (data) {
            if (data.success) {
                updateMutantProgressBar('60', 'Compiling Mutant');
            } else {
                // TODO
            }
        };

        const onMutantCompiled = function (data) {
            if (data.success) {
                updateMutantProgressBar('80', 'Running Tests Against Mutant');
            } else {
                // TODO
            }
        };

        const onMutantTested = function (data) {
            if (data.survived) {
                updateMutantProgressBar('100', 'Mutant Survived!');
            } else {
                // TODO
                updateMutantProgressBar('100', 'Mutant Killed!');
            }
        };

        const registerMutantProgressBar = function () {
            pushSocket.subscribe('<%=EventNames.toClientEventName(MutantProgressBarRegistrationEvent.class)%>', {
                gameId: ${requestScope.gameId}
            });

            pushSocket.register('<%=EventNames.toServerEventName(MutantSubmittedEvent.class)%>', onMutantSubmitted);
            pushSocket.register('<%=EventNames.toServerEventName(MutantCompiledEvent.class)%>',  onMutantCompiled);
            pushSocket.register('<%=EventNames.toServerEventName(MutantValidatedEvent.class)%>', onMutantValidated);
            pushSocket.register('<%=EventNames.toServerEventName(MutantTestedEvent.class)%>',    onMutantTested);
        };

        <%--
        const unregisterMutantProgressBar = function () {
            pushSocket.unsubscribe('<%=EventNames.toClientEventName(MutantProgressBarRegistrationEvent.class)%>', {
                playerId: ${requestScope.playerId}
            });

            pushSocket.unregister('<%=EventNames.toServerEventName(MutantSubmittedEvent.class)%>', onMutantSubmitted);
            pushSocket.unregister('<%=EventNames.toServerEventName(MutantCompiledEvent.class)%>',  onMutantCompiled);
            pushSocket.unregister('<%=EventNames.toServerEventName(MutantValidatedEvent.class)%>', onMutantValidated);
            pushSocket.unregister('<%=EventNames.toServerEventName(MutantTestedEvent.class)%>',    onMutantTested);
        };
        --%>

        window.mutantProgressBar = function () {
            insertMutantProgressBar();
            registerMutantProgressBar();
        }
    })();
</script>

