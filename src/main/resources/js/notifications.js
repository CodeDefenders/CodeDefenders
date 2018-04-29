/**
 * Created by thomas on 09/05/2017.
 */

var sendMessage = function(elem){
    var ele = $(elem);
    if (ele.attr("target") && ele.attr("gameId")) {
        var inp = $(ele.parent().find("input"));
        // TODO: Not sure how we get the contextPath here so we use relative url
        var url = "messages/send?message=" + encodeURI(inp.val()) + "&target=" + ele.attr("target") + "&gameId=" + ele.attr("gameId");

        $('.send-message input').val('');

        $.get(url, function (r) {
            if (r.status != "Success"){
                window.alert("Could not send message: " + r.status);
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

                lastCount.html("0");
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
            sendMessage(e.target);
        });


        $(".send-message input").keyup(function(e){
            if(e.keyCode == 13){ // 13 is enter
                $(e.target).parent().find("button").click();
            }
        });

    }
);