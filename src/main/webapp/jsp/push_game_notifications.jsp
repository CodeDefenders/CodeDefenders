<link href="css/notification-game.css" rel="stylesheet" type="text/css" />

<%-- Assume this is already there or replace the one which is there !
<div class="notification-game"></div>
TODO after reloading the page we lose the notificaiton count so we might need to store this into the session or load it from the db
where we might have the list of "read" messages.  
 
--%>

<script language="javascript" type="text/javascript">

	// Send a registration messaget to the WebSocket
	var registration = {};
	// Check game_view.jsp@34
	registration['type'] = "org.codedefenders.notification.web.PushSocketRegistrationEvent";
	registration['gameID'] = <%=request.getAttribute("gameId")%>;
	registration['playerID'] = <%=request.getAttribute("playerId")%>;
	registration['target'] = "GAME_EVENT";
	
    // This ensures that connection is open. Will retry otherwise
	sendMessage(JSON.stringify(registration));
    console.log("Game notification registered " + JSON.stringify(registration) )
    
	// Replace this with something probably a bit more reliabel:
	// https://gist.github.com/ismasan/299789
	// notificationMessageHandlers.push(gameEventHandler);

	// This should be interpreted as a local var...
	shakeIt = function(message) {
		// Ideally here one would simply encode this behavior in the notificationMessageHandlers
		if( message['type'] != 'GAME')
			return;
		
		var el = document.querySelector('.notification-game');
		var count = Number(el.getAttribute('data-count')) || 0;
		el.setAttribute('data-count', count + 1);
		el.classList.remove('notify');
		el.offsetWidth = el.offsetWidth;
		el.classList.add('notify');
		if (count === 0) {
			el.classList.add('show-count');
		}
	}
	// This does not show the actual notifications, but only that there might some !
	notificationMessageHandlers.push(shakeIt);
</script>
