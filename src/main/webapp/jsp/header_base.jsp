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

<%--
    @param String pageTitle
        The title of the page.
        TODO: change this to a bean?
--%>

<%
    String pageTitle = (String) request.getAttribute("pageTitle");
%>

<!DOCTYPE html>
<html>

<head>
    <title>Code Defenders<%= pageTitle != null ? " - " + pageTitle : "" %></title>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- App context -->
    <base href="${pageContext.request.contextPath}/">

    <!-- jQuery -->
    <script src="js/jquery.js" type="text/javascript" ></script>

    <!-- Slick -->
    <link href="css/slick_1.5.9.css" rel="stylesheet" type="text/css" />
    <script src="js/slick_1.5.9.min.js" type="text/javascript" ></script>

	<!-- Favicon.ico -->
	<link rel="icon" href="favicon.ico" type="image/x-icon">

    <!-- File Input -->
    <!--
    <script src="js/fileinput.min.js" type="text/javascript"></script>
    -->
    <!--
    <link href="css/fileinput.min.css" rel="stylesheet" type="text/css" />
    -->

    <!-- Bootstrap -->
    <script src="js/bootstrap.min.js" type="text/javascript" ></script>
    <link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />

    <!-- JQuery UI -->
    <script src="js/jquery-ui.min.js" type="text/javascript" ></script>
    <link href="css/jquery-ui.min.css" rel="stylesheet" type="text/css" />

    <link href="css/bootstrap-toggle_2.2.0.min.css" rel="stylesheet" type="text/css" />
    <script src="js/bootstrap-toggle_2.2.0.min.js" type="text/javascript" ></script>
    <!-- select -->
    <link href="css/bootstrap-select_1.9.3.min.css" rel="stylesheet" type="text/css" />
    <script src="js/bootstrap-select_1.9.3.min.js" type="text/javascript" ></script>


    <!-- Leaf -->
    <link href="css/base.css" rel="stylesheet">
    <script type="text/javascript" src="js/script.js"></script>

    <!-- Codemirror -->
    <script src="codemirror/lib/codemirror.js" type="text/javascript" ></script>
    <script src="codemirror/mode/clike/clike.js" type="text/javascript" ></script>
    <script src="codemirror/mode/diff/diff.js" type="text/javascript" ></script>
    <script src="codemirror/addon/dialog/dialog.js" type="text/javascript" ></script>
    <script src="codemirror/addon/search/searchcursor.js" type="text/javascript" ></script>
    <script src="codemirror/addon/search/search.js" type="text/javascript" ></script>
    <script src="codemirror/addon/scroll/annotatescrollbar.js" type="text/javascript" ></script>
    <script src="codemirror/addon/search/matchesonscrollbar.js" type="text/javascript" ></script>
    <script src="codemirror/addon/search/jump-to-line.js" type="text/javascript" ></script>
    <script src="codemirror/addon/selection/active-line.js" type="text/javascript" ></script>
    <script src="codemirror/addon/edit/matchbrackets.js" type="text/javascript" ></script>
    <script src="codemirror/addon/edit/closebrackets.js" type="text/javascript" ></script>
    <script src="codemirror/addon/hint/show-hint.js" type="text/javascript" ></script>
    <script src="codemirror/addon/hint/anyword-hint.js" type="text/javascript" ></script>

    <script src="codemirror/keymap/emacs.js" type="text/javascript" ></script>
    <script src="codemirror/keymap/sublime.js" type="text/javascript" ></script>
    <script src="codemirror/keymap/vim.js" type="text/javascript" ></script>

    <link href="codemirror/lib/codemirror.css" rel="stylesheet" type="text/css" />
    <!-- <link href="codemirror/lib/codemirror.css" rel="stylesheet" type="text/css" > -->
    <link href="codemirror/addon/dialog/dialog.css" rel="stylesheet" type="text/css" >
    <link href="codemirror/addon/search/matchesonscrollbar.css" rel="stylesheet" type="text/css" >
    <link href="codemirror/addon/hint/show-hint.css" rel="stylesheet" type="text/css" >

    <%-- This function shall be loaded on any page in which we compile the mutant --%>
    <%-- Sources:
        https://stackoverflow.com/questions/688196/how-to-use-a-link-to-call-javascript/688228#688228
        https://stackoverflow.com/questions/11715646/scroll-automatically-to-the-bottom-of-the-page
        https://stackoverflow.com/questions/10575343/codemirror-is-it-possible-to-scroll-to-a-line-so-that-it-is-in-the-middle-of-w/10725768
    --%>
    <!-- Function to link compiler error messages to code mirror -->
    <script type="text/javascript">
    function jumpToLine(i) {
        var editor = document.querySelector('#code').nextSibling.CodeMirror;
            // This is to scrool down the view to the form containing the code.
            var form = document.getElementById("atk")
            if (form == null ){
                form = document.getElementById("def")
            }
            if (form == null ){
                // We are not in a game page since there's no attacker or defender form
                return;
            }
            form.scrollIntoView()

            // This highlights the selected line
            editor.setCursor(i-1);
            // This simulate scrolling down inside code mirror view
            window.setTimeout(function() {
                editor.addLineClass(i-1, null, "center-me");
                var line = $('.CodeMirror-lines .center-me');
                var h = line.parent();
                $('.CodeMirror-scroll').scrollTop(0).scrollTop(line.offset().top - $('.CodeMirror-scroll').offset().top - Math.round($('.CodeMirror-scroll').height()/2));
           }, 200);
        }
    </script>


    <!-- Table sorter -->
    <script type="text/javascript" src="js/jquery.dataTables.min.js"></script>
    <script type="text/javascript" src="js/moment.min.js"></script> <!-- must come before datetime-moment -->
    <script type="text/javascript" src="js/datetime-moment.js"></script> <!-- must come after moment -->
    <link href="css/jquery.dataTables.min.css" rel="stylesheet" type="text/css" />
    <link href="css/datatables-override.css" rel="stylesheet" type="text/css" />


    <!-- MultiplayerGame -->
    <link href="css/gamestyle.css" rel="stylesheet" type="text/css" />
    <link href="css/notification-style.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="js/codedefenders/messaging.js"></script>

    <script type="text/javascript" src="js/codedefenders/class-info-api.js"></script>
    <script type="text/javascript" src="js/codedefenders/test-info-api.js"></script>
    <script type="text/javascript" src="js/codedefenders/mutant-info-api.js"></script>

    <!-- Upload page -->
    <link href="css/uploadcut.css" rel="stylesheet" type="text/css" />

    <link href="css/game_highlighting.css" rel="stylesheet" type="text/css" />
    <link href="css/error_highlighting.css" rel="stylesheet" type="text/css" />

    <link href="css/modal_dialogs.css" rel="stylesheet" type="text/css" />

    <script>
        $(document).ready(function() {
            $('.single-item').slick({
                arrows: true,
                infinite: true,
                speed: 300,
                draggable: false
            });
        });
    </script>

    <script>
        $(document).ready(function() {
            try {
                $('table.mutant-table').DataTable( {
                    "pagingType": "full",
                    "searching": true,
                    "lengthChange": false,
                    "ordering": false,
                    "pageLength": 4,
                    language: {
                        search: "_INPUT_",
                        searchPlaceholder: "Search...",
                        info: "",
                        sInfoEmpty: "",
                        sInfoFiltered: ""
                    }
                } );
            } catch (e) {
                // statements to handle TypeError exceptions
            }
        } );
    </script>

</head>

<body class="page-grid">
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="static org.codedefenders.game.GameState.ACTIVE" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.codedefenders.util.Paths" %>
