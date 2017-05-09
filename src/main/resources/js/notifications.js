/**
 * Created by thomas on 09/05/2017.
 */
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
    }
);