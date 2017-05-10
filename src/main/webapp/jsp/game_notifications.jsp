<script>
    //If the user is logged in, start receiving notifications
    var updateGameNotifications = function(url) {
        $.get(url, function (r) {

            var notificationCount = 0;

            $(r).each(function (index) {

                var eventClass = "#game-notifications-game"

                 if (r[index].eventType.includes("DEFENDER")){
                    eventClass = "#game-notifications-defenders"
                 } else if (r[index].eventType.includes("ATTACKER")){
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

                var oldNotifications = $(eventClass).html();

                oldNotifications =
                    "<p><span class=\"event\">" +
                    r[index].message +
                    "</span></p>" + oldNotifications;

                $(eventClass).html(oldNotifications);

                var lastTotalCount = $("#notif-game-total-count");

                var totalCount = parseInt(lastTotalCount.html()) + 1;

                if (isNaN(total)){
                    total = 1;
                }

                lastTotalCount.html(totalCount);
            });
        });
    };

    var toggleNotificationTimer = function(show){
        //TODO: Show/Hide loading animation
    };

    var updateGameMutants = function(url) {
        $.get(url, function (r) {
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
                var url = "/game_notifications?gameId=" + <%=gameId%> +"&timestamp=" + lastTime;
                lastTime = Math.round(new Date().getTime()/1000);
                updateGameNotifications(url);
            }, interval)
        }
    );
</script>
<div id="game-notification-bar" class="min">
<a id="notification-show-bar"><span>(<span
id="notif-game-total-count">0</span>)</span>
</a>
    <div class="game-notifications min" id="game-notifications-attackers"
    style="right: 20px;">
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

     <div class="game-notifications min" id="game-notifications-defenders"
     style="right: 220px;">
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

     <div class="game-notifications min" id="game-notifications-game"
     style="right: 420px;">
        <a>Game<span class="hidden">(<span class="notif-count"></span>)</span></a>
            <div class="events">
    &nbsp;
        </div>
        <div class="send-message">
            <input disabled="true" type="text"
            placeholder="Cannot send game messages" />
            <button>&gt;</button>
        </div>
     </div><!-- col-md-6 left bottom -->
</div>