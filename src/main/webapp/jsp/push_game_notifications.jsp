<link href="css/notification-icons.css" rel="stylesheet" type="text/css" />

<%-- Assume this is already there or replace the one which is there !
<div class="notification-game"></div>
TODO after reloading the page we lose the notificaiton count so we might need to store this into the session or load it from the db
where we might have the list of "read" messages.  
--%>

<script>
	// Send a registration message to the WebSocket
	var registration = {
	    type: "org.codedefenders.notification.web.PushSocketRegistrationEvent",
        gameID: <%=request.getAttribute("gameId")%>, // Check game_view.jsp@34
        playerID: <%=request.getAttribute("playerId")%>,
        target: 'GAME_EVENT'
    };

    // This ensures that connection is open. Will retry otherwise
	pushSocket.send(registration);
    console.log("Game notification registered:");
    console.log(registration);

	// This should be interpreted as a local var...
	shakeIt = function (message) {
		// Ideally here one would simply encode this behavior in the notificationMessageHandlers
		var el = document.querySelector('#notification-game');
		var count = Number(el.getAttribute('data-count')) || 0;
		el.setAttribute('data-count', count + 1);
		el.classList.remove('notify');
		el.offsetWidth = el.offsetWidth;
		el.classList.add('notify');
		if (count === 0) {
			el.classList.add('show-count');
		}
	};

	// This does not show the actual notifications, but only that there might some !
    pushSocket.register(PushSocket.EventType.GAME, shakeIt);
</script>
