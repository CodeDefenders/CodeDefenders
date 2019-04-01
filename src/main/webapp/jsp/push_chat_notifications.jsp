<link href="css/notification-chat.css" rel="stylesheet" type="text/css" />

<%-- Assume this is already there or replace the one which is there !
<div class="notification-chat"></div>
TODO after reloading the page we lose the notificaiton count so we might need to store this into the session or load it from the db
where we might have the list of "read" messages.  
--%>

<script language="javascript" type="text/javascript">
	// Send a registration messaget to the WebSocket
	var registration = {};
	
	registration['type'] = "org.codedefenders.notification.web.PushSocketRegistrationEvent";
	registration['target'] = "CHAT_EVENT";
	
	// Will this work ?
	sendMessage(JSON.stringify(registration));

	shakeIt = function(message) {
		// Ideally here one would simply encode this behavior in the notificationMessageHandlers
        if( message['target'] != 'CHAT_EVENT')
            return;
		
		console.log("Chat: Got message " + message)

		var el = document.querySelector('.notification-chat');
		var count = Number(el.getAttribute('data-count')) || 0;
		el.setAttribute('data-count', count + 1);
		el.classList.remove('notify');
		el.offsetWidth = el.offsetWidth;
		el.classList.add('notify');
		if (count === 0) {
			el.classList.add('show-count');
		}
	}
	// This does not show the actual notifications content... just the count
	// Replace this with something probably a bit more reliabel:
    // https://gist.github.com/ismasan/299789
    // notificationMessageHandlers.push(gameEventHandler);
	notificationMessageHandlers.push(shakeIt);
</script>
