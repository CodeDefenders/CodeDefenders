<script>
    //If the user is logged in, start receiving notifications
    var updateGameNotifications = function(url) {
        $.get(url, function (r) {

            var notificationCount = 0;

            $(r).each(function (index) {
                var oldNotifications = $("#game-notifications").html();

                var eventClass = "gameEvent"

                 if (r[index].eventType.includes("DEFENDER")){
                    eventClass = "defenderEvent"
                 } else if (r[index].eventType.includes("ATTACKER")){
                    eventClass = "attackerEvent"
                }

                oldNotifications =
                    "<p><span class='" + eventClass + "'>" +
                    r[index].message +
                    "</span></p>" + oldNotifications;

                $("#game-notifications").html(oldNotifications);
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
<h1 style="text-align: center;">Game Log</h1>
<div class="ws-12" id="game-notifications">

</div> <!-- col-md-6 left bottom -->