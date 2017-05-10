/**
 * Created by thomas on 09/05/2017.
 */

var sendMessage = function(elem){
    var ele = $(elem);
    if (ele.attr("target") && ele.attr("gameId")) {
        var url = "/messages/send?message=" + encodeURI($(ele.parent().find("input")).val()) + "&target=" + ele.attr("target") + "&gameId=" + ele.attr("gameId");
        $.get(url, function (r) {
            if (r.status != "Success"){
                window.alert("Could not send message: " + r.status);
            } else {
                //TODO: Put newly sent message here.
            }
        });
    }
};

$(document).ready(function() {
        $(".game-notifications a").click(function (e) {
            var parent = $(e.target).parent();

            var shown = parent.hasClass("expand");

            $(".game-notifications").removeClass("expand");
            $(".game-notifications").addClass("min");


            if (!shown) {
                parent.toggleClass("expand");
                parent.toggleClass("min");
                parent.removeClass("notif-alert");
                parent.focus();
                var lastCount = $(parent).find(".notif-count");
                lastCount.parent().hide();
            }
        });


        $("#notification-show-bar").click(function(e){
           var shown = $("#game-notification-bar").hasClass("expand");

           if (shown){
               $(".game-notifications").hide();
               $("#game-notification-bar").removeClass("expand");
               $("#game-notification-bar").addClass("min");
           } else {
               $(".game-notifications").show();
               $("#game-notification-bar").addClass("expand");
               $("#game-notification-bar").removeClass("min");

           }
        });

        $("#game-notification-bar").removeClass("expand");
        $("#game-notification-bar").addClass("min");
        $(".game-notifications").hide();
        $(".send-message button").click(function(e){
            alert("message")
            sendMessage(e.target);
        });

    }
);