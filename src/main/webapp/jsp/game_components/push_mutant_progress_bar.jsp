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
<%@ page import="org.codedefenders.notification.events.client.registration.RegistrationEvent" %>
<%@ page import="org.codedefenders.notification.events.server.KillEvent" %>

<script>
    // TODO: put the progressbar into the html and just reference it here

    const insertProgressBar = function () {
        let progressBar = document.getElementById("progress-bar");

        // Create the Div to host the progress bar if that's not there
        if (progressBar == null) {
            // Load the progress bar
            progressBar = document.createElement('div');
            progressBar.setAttribute('class', 'progress');
            progressBar.setAttribute('id', 'progress-bar');
            progressBar.setAttribute('style', 'height: 40px; font-size: 30px');
            const form = document.getElementById('logout');
            // Insert progress bar right under logout... this will conflicts with the other push-events
            form.parentNode.insertBefore(progressBar, form.nextSibling);
        }

        return progressBar;
    };

	const pbOnMutantSubmitted = function (data) {
        const progressBar = insertProgressBar();
        progressBar.innerHTML = '<div class="progress-bar progress-bar-info" role="progressbar" style="width: 33%; font-size: 15px; line-height: 40px;" aria-valuenow="33" aria-valuemin="0" aria-valuemax="100">Compiling And Validating Mutant</div>';
    };

    const pbOnMutantCompiled = function (data) {
        const progressBar = insertProgressBar();
        progressBar.innerHTML = '<div class="progress-bar progress-bar-info" role="progressbar" style="width: 66%; font-size: 15px; line-height: 40px;" aria-valuenow="66" aria-valuemin="0" aria-valuemax="100">Running Tests Against Mutant</div>';
    };

    const pbOnMutantInvalid = function (data) {
        const progressBar = insertProgressBar();
        progressBar.innerHTML = '<div class="progress-bar progress-bar-danger" role="progressbar" style="width: 100%; font-size: 15px; line-height: 40px;" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100">Mutant Is Not Valid</div>';
    };

    const pbOnMutantKilled = function (data) {
        const progressBar = insertProgressBar();
        progressBar.innerHTML = '<div class="progress-bar progress-bar-danger" role="progressbar" style="width: 100%; font-size: 15px; line-height: 40px;" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100">Mutant Was Killed</div>';
        progressBar.parentNode.removeChild(progressBar);
        unregisterMutantProgressBar();
    };

    const pbOnMutantSurvived = function (data) {
        const progressBar = insertProgressBar();
        progressBar.innerHTML = '<div class="progress-bar progress-bar-success" role="progressbar" style="width: 100%; font-size: 15px; line-height: 40px;" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100">Mutant Survived</div>';
        progressBar.parentNode.removeChild(progressBar);
        unregisterMutantProgressBar();
    };

    // Send a registration message to the WebSocket
	// Registration is done while submitting the mutant
	const registerMutantProgressBar = function () {
	    pushSocket.subscribe('<%=RegistrationEvent.EventType.PROGRESSBAR%>', {
            gameId: <%=request.getAttribute("gameId")%>,
            playerId: <%=request.getAttribute("playerId")%>
        });

	    pushSocket.register('<%=System.out.println(KillEvent.class)%>',
            progressBarUpdateHandler
        );
	};

    const unregisterMutantProgressBar = function () {
        pushSocket.unsubscribe('<%=RegistrationEvent.EventType.PROGRESSBAR%>');
    };

    /* MUTANTS PROGRESSBAR */
    // mutant submitted
    // mutant compiled and validated -> outcome
    // running tests against mutant
    // done running tests against mutant -> outcome

    /* TESTS PROGRESSBAR */
    // test submitted
    // test compiled and validated -> outcome
    // running test against mutants
    // done running test against mutants -> outcome

</script>

