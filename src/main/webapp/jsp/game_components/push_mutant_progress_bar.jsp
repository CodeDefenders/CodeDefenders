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
<%@ page import="org.codedefenders.model.NotificationType"%>

<script>
	// Define the handler for the progress bar message and register that to websocket
	var progressBarUpdateHandler = function(message) {
		
		// Do we need to remove the bar or it will go away by itself ?
		if (message['type'] == 'PROGRESSBAR') {
			/* console.log("Got event for progress bar" + message['type']) */
			var progressBar = document.getElementById("progress-bar");
			// Create the Div to host the progress bar if that's not there
			if (progressBar == null) {
				// Load the progress bar
				progressBar = document.createElement('div');
				progressBar.setAttribute('class', 'progress');
				progressBar.setAttribute('id', 'progress-bar');
				progressBar.setAttribute('style',
						'height: 40px; font-size: 30px');
				var form = document.getElementById('logout');
				// Insert progress bar right under logout... this will conflicts with the other push-events
				form.parentNode.insertBefore(progressBar, form.nextSibling);
			}
		    // Update the message in the progress bar
			switch (message['message']) {
			case 'NEW_MUTANT':
				// After test is submitted
				progressBar.innerHTML = '<div class="progress-bar progress-bar-info" role="progressbar" style="width: 33%; font-size: 15px; line-height: 40px;" aria-valuenow="33" aria-valuemin="0" aria-valuemax="100">Validating and Compiling Mutant</div>';
				break;
			case 'COMPILE_MUTANT': // After test is compiled
				progressBar.innerHTML = '<div class="progress-bar progress-bar-info" role="progressbar" style="width: 66%; font-size: 15px; line-height: 40px;" aria-valuenow="66" aria-valuemin="0" aria-valuemax="100">Running first Test Against Mutant</div>';
				break;
			case 'TEST_MUTANT': // After testing original
				progressBar.innerHTML = '<div class="progress-bar progress-bar-warning" role="progressbar" style="width: 90%; font-size: 15px; line-height: 40px;" aria-valuenow="90" aria-valuemin="0" aria-valuemax="100">Running more Tests Against Mutant</div>';
				break;
			case 'MUTANT_KILLED': // If the mutant dies
				progressBar.innerHTML = '<div class="progress-bar progress-bar-danger" role="progressbar" style="width: 90%; font-size: 15px; line-height: 40px;" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100">Mutant was Killed</div>';
				
				// Remove the bar from the DOM
                progressBar.parentNode.removeChild(progressBar);
                // Deregister progress bar
                var registration = {};
                registration['type'] = "org.codedefenders.notification.web.PushSocketRegistrationEvent";
                registration['action'] = "UNREGISTER"
                registration['gameID'] = <%=request.getAttribute("gameId")%>;
                registration['playerID'] = <%=request.getAttribute("playerId")%>;
                registration['target'] = "PROGRESSBAR_EVENT";
        
                // This ensures that connection is open. Will retry otherwise
                sendMessage(JSON.stringify(registration));
                console.log("Mutant progress bar unregistered " + JSON.stringify(registration) )
        
                break;
			case 'MUTANT_DONE':
				progressBar.innerHTML = '<div class="progress-bar progress-bar-success" role="progressbar" style="width: 90%; font-size: 15px; line-height: 40px;" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100">Mutant survived</div>';

				// Remove the bar from the DOM
				progressBar.parentNode.removeChild(progressBar);
				// Deregister progress bar
				var registration = {};
			    registration['type'] = "org.codedefenders.notification.web.PushSocketRegistrationEvent";
			    registration['action'] = "UNREGISTER"
			    registration['gameID'] = <%=request.getAttribute("gameId")%>;
			    registration['playerID'] = <%=request.getAttribute("playerId")%>;
			    registration['target'] = "PROGRESSBAR_EVENT";
        
			    // This ensures that connection is open. Will retry otherwise
                sendMessage(JSON.stringify(registration));
                console.log("Mutant progress bar unregistered " + JSON.stringify(registration) )
				
                break;
			}

		}
	}

	// Send a registration messaget to the WebSocket
	// Registration is done while submitting the mutant
	function registerMutantProgressBar(){
	    var registration = {};
	    registration['type'] = "org.codedefenders.notification.web.PushSocketRegistrationEvent";
	    registration['gameID'] = <%=request.getAttribute("gameId")%>;
	    registration['playerID'] = <%=request.getAttribute("playerId")%>;
	    registration['target'] = "PROGRESSBAR_EVENT";
	    
	    // This ensures that connection is open. Will retry otherwise
	    sendMessage(JSON.stringify(registration));
	    console.log("Mutant progress bar registered " + JSON.stringify(registration) )
	
	    // This will be automatically unregisterd @OnClose or via unregistration message
	    notificationMessageHandlers.push(progressBarUpdateHandler);
	    console.log("Progress bar handler registered")
	}
	
</script>

