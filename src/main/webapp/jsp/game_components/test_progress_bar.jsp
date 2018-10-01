<%--
    Adds a JavaScript function progressBar() that inserts and updates a progressbar showing the status of the last
    submitted test. The progressbar is inserted after #logout. It reads the test status from /notifications.

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
                    switch( r[index] ){
                        case 'COMPILE_TEST': // After test is compiled
                            progressBarDiv.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 50%; font-size: 15px; line-height: 40px;" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100">Running Test Against Original</div>';
                            break;
                        case "TEST_ORIGINAL": // After testing original
                            progressBarDiv.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 75%; font-size: 15px; line-height: 40px;" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100">Running Test Against first Mutant</div>';
                            break;
                        // Not sure will ever get this one... since the test_mutant target execution might be written after testing mutants.
                        case "TEST_MUTANT":
                            progressBarDiv.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 90%; font-size: 15px; line-height: 40px;" aria-valuenow="90" aria-valuemin="0" aria-valuemax="100">Running Test Against more Mutants</div>';
                            break;
                    }
                });
            }
        );
    };

    var progressBar = function () {
        // Create the Div to host events if that's not there
        if (document.getElementById("progress-bar") == null) {
            // Load the progress bar
            var progressBar = document.createElement('div');
            progressBar.setAttribute('class', 'progress');
            progressBar.setAttribute('id', 'progress-bar');
            progressBar.setAttribute('style', 'height: 40px; font-size: 30px');
            //
            progressBar.innerHTML = '<div class="progress-bar bg-danger" role="progressbar" style="width: 25%; font-size: 15px; line-height: 40px;" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100">Validating and Compiling the Test</div>';
            var form = document.getElementById('logout');
            // Insert progress bar right under logout... this will conflicts with the other push-events
            form.parentNode.insertBefore(progressBar, form.nextSibling);
        }
        // Do a first request right away, such that compilation of this test is hopefully not yet started. This one will set the session...
        var updateURL = "<%= request.getContextPath()%>/notifications?type=<%=NotificationType.PROGRESSBAR%>&progressBar=1&gameId=" + <%= gameIdTODORENAME %> +"&isDefender=1";
        updateProgressBar(updateURL);

        // Register the requests to start in 1 sec
        var interval = 1000;
        setInterval(function () {
            updateProgressBar(updateURL);
        }, interval);
    }
</script>

<% } %>
