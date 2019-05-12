<link href="css/notification-icons.css" rel="stylesheet" type="text/css" />

<%-- Assume this is already there or replace the one which is there !
<div class="notification-chat"></div>
TODO after reloading the page we lose the notificaiton count so we might need to store this into the session or load it from the db
where we might have the list of "read" messages.  
--%>

<script>
	// Send a registration message to the WebSocket
	var registration = {
        type: 'org.codedefenders.notification.web.PushSocketRegistrationEvent',
        target: 'CHAT_EVENT'
    };

	sendMessage(registration);
    console.log("Chat notification registered:");
    console.log(registration);

	shakeIt = function(message) {
		console.log("Chat: Got message " + message);

		var el = document.querySelector('#notification-chat');
		var count = Number(el.getAttribute('data-count')) || 0;
		el.setAttribute('data-count', count + 1);
		el.classList.remove('notify');
		el.offsetWidth = el.offsetWidth;
		el.classList.add('notify');
		if (count === 0) {
			el.classList.add('show-count');
		}
	};

	pushSocket.register(PushSocket.EventType.CHAT, shakeIt);
</script>
