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
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.model.NotificationType" %>

<%
    {
    int gameId = (Integer) request.getAttribute("gameId");
%>
<script>
    //If the user is logged in, start receiving notifications
    var updateGameNotifications = function(url) {
        $.getJSON(url, function (r) {
            for (var i = 0; i < r.length; i++){
                r[i].time = Date.parse(r[i].time);
            }
            r.sort(function (a, b) {
                return a.time - b.time;
            });
            $(r).each(function (index) {

                var eventClass = "#game-notifications-game"

                 if (r[index].eventType == "DEFENDER_MESSAGE"){
                    eventClass = "#game-notifications-defenders"
                 } else if (r[index].eventType == "ATTACKER_MESSAGE"){
                    eventClass = "#game-notifications-attackers"
                }

                var lastCount = $(eventClass).find(".notif-count");

                var total = parseInt(lastCount.html()) + 1;

                if (isNaN(total)){
                    total = 1;
                }

                if (total > 0){
                    lastCount.parent().show();
                    lastCount.parent().removeClass("hidden");
                } else {
                    lastCount.parent().hide();
                }

                lastCount.html(total);

                $(eventClass).addClass("notif-alert");

                eventClass += " .events";

                $(eventClass).scrollTop(0)

                var oldNotifications = $(eventClass).html();

                oldNotifications =
                    "<p><span class=\"event\">" +
                    r[index].parsedMessage +
                    "</span></p>" + oldNotifications;

                $(eventClass).html(oldNotifications);

                var lastTotalCount = $("#notif-game-total-count");

                var totalCount = parseInt(lastTotalCount.html()) + 1;

                if (isNaN(totalCount)){
                    totalCount = 1;
                }

                lastTotalCount.html(totalCount);
            });
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

    $(document).ready(function() {
        var interval = 5000;
        var lastTime = 0;
        setInterval(function () {
            var url = "<%= request.getContextPath() + Paths.API_NOTIFICATION%>?type=<%=NotificationType.GAMEEVENT%>&gameId=<%=gameId%>&timestamp=" + lastTime;
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


    <%if(game.isChatEnabled()) {%>
<div id="game-notification-bar" class="min<%
        if (role.equals(Role.CREATOR)) { %> creator<% } %>">
<a id="notification-show-bar"><span>(<span
id="notif-game-total-count">0</span>)</span>
</a>

     <%
     int rightPosition = 20;
     if (!role.equals(Role.DEFENDER)){
    %>
    <div class="game-notifications min" id="game-notifications-attackers"
    style="right: <%= rightPosition %>px;">
        <a>Attackers<span class="hidden">(<span class="notif-count"></span>)</span></a>
            <div class="events">

    &nbsp;
            </div>
            <div class="send-message">
                <input type="text" placeholder="send message" />
                <button gameId="<%=gameId%>" target="ATTACKER_MESSAGE">&gt;
                </button>
            </div>
    </div>

    <%
    rightPosition += 200;

    } if (!role.equals(Role.ATTACKER)){ %>
     <div class="game-notifications min" id="game-notifications-defenders"
     style="right: <%= rightPosition %>px">
        <a>Defenders<span class="hidden">(<span class="notif-count"></span>)</span></a>
            <div class="events">

            &nbsp;
            </div>
            <div class="send-message">
                <input type="text" placeholder="send message" />
                <button gameId="<%=gameId%>" target="DEFENDER_MESSAGE">&gt;
                </button>
            </div>
     </div>

     <%
      rightPosition += 200;
      } %>

     <div class="game-notifications min" id="game-notifications-game"
     style="right: <%= rightPosition %>px;">
        <a>Game<span class="hidden">(<span class="notif-count"></span>)</span></a>
            <div class="events">
    &nbsp;
        </div>
            <div class="send-message">
                <input type="text" placeholder="send message" />
                <button gameId="<%=gameId%>" target="GAME_MESSAGE">&gt;
                </button>
            </div>
     </div><!-- col-md-6 left bottom -->
</div>
<%
    }
}
%>
