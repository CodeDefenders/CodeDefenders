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
            console.log("Got event for progress bar" + message)
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
            case 'NEW_TEST':
            	progressBar.innerHTML = '<div class="progress-bar progress-bar-info" role="progressbar" style="width: 25%; font-size: 15px; line-height: 40px;" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100">Validating and Compiling the Test</div>';
                break;
            case 'COMPILE_TEST': // After test is compiled
            	progressBar.innerHTML='<div class="progress-bar progress-bar-info" role="progressbar" style="width: 50%; font-size: 15px; line-height: 40px;" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100">Running Test Against Original</div>';
                break;
            case "TEST_ORIGINAL": // After testing original
            	progressBar.innerHTML='<div class="progress-bar progress-bar-success" role="progressbar" style="width: 75%; font-size: 15px; line-height: 40px;" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100">Running Test Against first Mutant</div>';
                break;
            case "TEST_MUTANT":
            	progressBar.innerHTML='<div class="progress-bar progress-bar-success" role="progressbar" style="width: 90%; font-size: 15px; line-height: 40px;" aria-valuenow="90" aria-valuemin="0" aria-valuemax="100">Running Test Against more Mutants</div>';
                break;
            case 'TEST_DONE':
            	progressBar.innerHTML='<div class="progress-bar progress-bar-success" role="progressbar" style="width: 100%; font-size: 15px; line-height: 40px;" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100">Running Test Against more Mutants</div>';
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
                console.log("Test progress bar unregistered " + JSON.stringify(registration) )
                
                break;
            }

        }
    }

    // Send a registration messaget to the WebSocket
    // Registration is done while submitting the mutant
    function registerTestProgressBar(){
        var registration = {};
        registration['type'] = "org.codedefenders.notification.web.PushSocketRegistrationEvent";
        registration['gameID'] = <%=request.getAttribute("gameId")%>;
        registration['playerID'] = <%=request.getAttribute("playerId")%>;
        registration['target'] = "PROGRESSBAR_EVENT";
        
        // This ensures that connection is open. Will retry otherwise
        sendMessage(JSON.stringify(registration));
        console.log("Test progress bar registered " + JSON.stringify(registration) )
    
        // This will be automatically unregisterd @OnClose or via unregistration message
        notificationMessageHandlers.push(progressBarUpdateHandler);
        console.log("Progress bar handler registered")
    }
    
</script>

