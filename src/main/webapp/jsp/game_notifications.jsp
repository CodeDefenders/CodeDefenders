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
<%@page import="org.codedefenders.servlets.util.ServletUtils"%>
<%@page import="org.codedefenders.game.multiplayer.MultiplayerGame"%>
<%@page import="org.codedefenders.util.Paths"%>
<%@ page import="org.codedefenders.game.Role"%>
<%@ page import="org.codedefenders.model.NotificationType"%>

<%
    {
				int gameId = (Integer) request.getAttribute("gameId");
				MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
				int userId = ServletUtils.userId(request); // required for playerFeedback, too
				Role role = game.getRole(userId); // required for header_game, too
%>
<script>
    var receivedMessage = []
    
    // Scroll down "a lot"
    var scrollToBottom = function(view){
    	view.scrollTop(1E10)
    }
    
    var refreshTheChatWindows = function(sortedMessagesToDisplay){
        
        var total = sortedMessagesToDisplay.length

        var gameView = $("#game-notifications-game").children("div.events")
        gameView.empty()
        
        var attackView = $("#game-notifications-attackers").children("div.events")
        attackView.empty()
        
        var defendView = $("#game-notifications-defenders").children("div.events")
        defendView.empty()
        
        // Messages are sorted so we can render them on the fly
        for (var index = 0; index < total; index++) {
        	
            if (sortedMessagesToDisplay[index].eventType == "DEFENDER_MESSAGE"){
            	defendView.append("<p><span class=\"event\">" + sortedMessagesToDisplay[index].parsedMessage + "</span></p>")
            	scrollToBottom(defendView);
                                 
            } else if (sortedMessagesToDisplay[index].eventType == "ATTACKER_MESSAGE"){
            	attackView.append("<p><span class=\"event\">" + sortedMessagesToDisplay[index].parsedMessage + "</span></p>")
            	scrollToBottom(attackView);
                
            } else {
            	gameView.append("<p><span class=\"event\">" + sortedMessagesToDisplay[index].parsedMessage + "</span></p>")
            	scrollToBottom(gameView);
            }
        }
    } 
    
    //If the user is logged in, start receiving notifications
    var updateGameNotifications = function(url) {
    	
        $.getJSON(url, 
        		function (r) {
            for (var i = 0; i < r.length; i++){
                r[i].time = Date.parse(r[i].time);
                receivedMessage.push( r[i]  );
            }

            receivedMessage.sort(function (a, b) {
                return a.time - b.time;
            });
            
            refreshTheChatWindows( receivedMessage );
            
        });
    };

    var toggleNotificationTimer = function(show){
        //TODO: Show/Hide loading animation
    };

    var updateGameMutants = function(url) {
        $.getJSON(url, function (r) {
            var mutLines = [];
            $(r).each(function (index) {
                var mut = r[index];

                for (line in mut.lines){
                    if (!mutLines[line]){
                        mutLines[line] = [];
                    }

                    mutLines.push(mut);
                }
            });

            mutantLine(mutLines, "cut-div");

        });
    };

    // TODO Make this a on-demand function call when one clicks on the notification icons...
    $(document).ready(function() {
        var interval = 5000;
        var lastTime = 0;
        setInterval(function () {
            var url = "<%=request.getContextPath() + Paths.API_NOTIFICATION%>?type=<%=NotificationType.GAMEEVENT%>&gameId=<%=gameId%>&timestamp=" + lastTime;
            lastTime = Math.round(new Date().getTime()/1000);
            updateGameNotifications(url);
        }, interval)
    });
</script>


<script type="text/javascript">

	var updateMessages = function(url) {
        $.getJSON(url, function (r) {
			$(r).each(function (index) {

				// Skip messages that belong to the current user
				if( r[index].userId == <%=request.getSession().getAttribute("uid")%> ){
					return;
				}
				// Create the Div to host events if that's not there
				if( document.getElementById("push-events-div") == null ){
					var div = document.createElement('div');
					div.setAttribute('class','alert alert-info');
					div.setAttribute('id','push-events-div');
					// This is fine, but then it closes it and no messaged can be shown anymore !
					div.innerHTML='<a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><br />';
					var form = document.getElementById('logout');
					form.parentNode.insertBefore(div, form.nextSibling);
				}

				var msgId='_' + Math.random().toString(36).substr(2, 9);
				var msg = document.createElement('pre');
				msg.setAttribute('id', msgId);
				msg.innerHTML="<strong>"+r[index].message+"</strong>"
				document.getElementById("push-events-div").appendChild( msg );
				// Fade out and remove the message
				$('#'+msgId).delay(10000).fadeOut("normal", function() {
					$(this).remove();
					// Check how many elements are left, in case no more messages are there, remove the bar as well...
					var div = document.getElementById("push-events-div");
					if( div != null ){
						if( div.getElementsByTagName('*').length <= 2){ // There's <a> and <br>
							document.getElementById("push-events-div").remove();
						}
					}
				});
			});
		});
	};

	$(document).ready(function() {
            //notifications written here:
            // refreshed every 5 seconds
            var interval = 5000;
            setInterval(function () {
                var url = "<%=request.getContextPath() + Paths.API_NOTIFICATION%>?type=<%=NotificationType.PUSHEVENT%>&gameId=" + <%=gameId%> +"&timestamp=" + (new Date().getTime() - interval);
                updateMessages(url);
            }, interval)
    });
</script>


<%
    if (game.isChatEnabled()) {
%>
<div id="game-notification-bar"
	class="min<%if (role.equals(Role.OBSERVER)) {%> creator<%}%>">
	<a id="notification-show-bar"><span>(<span
			id="notif-game-total-count">0</span>)
	</span> </a>

	<%
	    int rightPosition = 20;
	            if (!role.equals(Role.DEFENDER)) {
	%>
	<div class="game-notifications min" id="game-notifications-attackers"
		style="right: <%=rightPosition%>px;">
		<a>Attackers<span class="hidden">(<span class="notif-count"></span>)
		</span></a>
		<div class="events">&nbsp;</div>
		<div class="send-message">
			<input type="text" placeholder="send message" />
			<button gameId="<%=gameId%>" target="ATTACKER_MESSAGE">&gt;
			</button>
		</div>
	</div>

	<%
	    rightPosition += 200;

	            }
	            if (!role.equals(Role.ATTACKER)) {
	%>
	<div class="game-notifications min" id="game-notifications-defenders"
		style="right: <%=rightPosition%>px">
		<a>Defenders<span class="hidden">(<span class="notif-count"></span>)
		</span></a>
		<div class="events">&nbsp;</div>
		<div class="send-message">
			<input type="text" placeholder="send message" />
			<button gameId="<%=gameId%>" target="DEFENDER_MESSAGE">&gt;
			</button>
		</div>
	</div>

	<%
	    rightPosition += 200;
	            }
	%>

	<div class="game-notifications min" id="game-notifications-game"
		style="right: <%=rightPosition%>px;">
		<a>Game<span class="hidden">(<span class="notif-count"></span>)
		</span></a>
		<div class="events">&nbsp;</div>
		<div class="send-message">
			<input type="text" placeholder="send message" />
			<button gameId="<%=gameId%>" target="GAME_MESSAGE">&gt;</button>
		</div>
	</div>
	<!-- col-md-6 left bottom -->
</div>
<%
    }
    }
%>
