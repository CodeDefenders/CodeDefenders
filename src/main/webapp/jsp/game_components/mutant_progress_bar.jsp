<%@ page import="org.codedefenders.model.NotificationType" %>

<%--
    Adds a JavaScript function progressBar() that inserts and updates a progressbar showing the status of the last
    submitted mutant. The progressbar is inserted after #logout. It reads the mutant status from /notifications.

    @param Integer gameId
        The id of the game.
--%>

<% { %>

<%
    Integer gameIdTODORENAME = (Integer) request.getAttribute("gameId");
%>

<script>
    var updateProgressBar = function (url) {
        var progressBarDiv = document.getElementById("progress-bar");
        $.get(url, function (r) {
                $(r).each(function (index) {
                    switch (r[index]) {
                        case 'COMPILE_MUTANT': // After test is compiled
                            progressBarDiv.innerHTML = '<div class="progress-bar bg-danger" role="progressbar" style="width: 66%; font-size: 15px; line-height: 40px;" aria-valuenow="66" aria-valuemin="0" aria-valuemax="100">Running first Test Against Mutant</div>';
                            break;
                        case "TEST_MUTANT": // After testing original
                            progressBarDiv.innerHTML = '<div class="progress-bar bg-danger" role="progressbar" style="width: 90%; font-size: 15px; line-height: 40px;" aria-valuenow="90" aria-valuemin="0" aria-valuemax="100">Running more Tests Against Mutant</div>';
                            break;
                    }
                });
            }
        );
    };

    function progressBar() {

        // Create the Div to host events if that's not there
        if (document.getElementById("progress-bar") == null) {
            // Load the progress bar
            var progressBar = document.createElement('div');
            progressBar.setAttribute('class', 'progress');
            progressBar.setAttribute('id', 'progress-bar');
            progressBar.setAttribute('style', 'height: 40px; font-size: 30px');
            //
            progressBar.innerHTML = '<div class="progress-bar bg-danger" role="progressbar" style="width: 33%; font-size: 15px; line-height: 40px;" aria-valuenow="33" aria-valuemin="0" aria-valuemax="100">Validating and Compiling Mutant</div>';
            var form = document.getElementById('logout');
            // Insert progress bar right under logout... this will conflicts with the other push-events
            form.parentNode.insertBefore(progressBar, form.nextSibling);
        }
        // Do a first request right away, such that compilation of this test is hopefully not yet started. This one will set the session...

        var updateURL = "<%= request.getContextPath()%>/notifications?type=<%=NotificationType.PROGRESSBAR%>&progressBar=1&gameId=" + <%= gameIdTODORENAME %>;
        updateProgressBar(updateURL);

        // Register the requests to start in 1 sec
        var interval = 1000;
        setInterval(function () {
            updateProgressBar(updateURL);
        }, interval)
    }
</script>

<% } %>
