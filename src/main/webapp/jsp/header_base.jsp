<!DOCTYPE html>
<html>

<head>
    <title>Code Defenders - <% if (pageTitle != null) { %><%= pageTitle %>
        <% } %></title>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- App context -->
    <base href="${pageContext.request.contextPath}/">

    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- jQuery -->
    <script src="js/jquery.min.js" type="text/javascript" ></script>

    <!-- Slick -->
    <link href="css/slick_1.5.9.css" rel="stylesheet" type="text/css" />
    <script src="js/slick_1.5.9.min.js" type="text/javascript" ></script>

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
    <script src="js/jquery-ui.js" type="text/javascript" ></script>
    <link href="css/jquery-ui.css" rel="stylesheet" type="text/css" />

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

    <link href="codemirror/lib/codemirror.css" rel="stylesheet" type="text/css" />
    <!-- <link href="codemirror/lib/codemirror.css" rel="stylesheet" type="text/css" > -->
    <link href="codemirror/addon/dialog/dialog.css" rel="stylesheet" type="text/css" >
    <link href="codemirror/addon/search/matchesonscrollbar.css" rel="stylesheet" type="text/css" >

    <!-- Table sorter -->
    <script type="text/javascript" src="js/jquery.dataTables.min.js"></script>
    <script type="text/javascript" src="js/moment.min.js"></script> <!-- must come before datetime-moment -->
    <script type="text/javascript" src="js/datetime-moment.js"></script> <!-- must come after moment -->
    <link href="css/jquery.dataTables.min.css" rel="stylesheet" type="text/css" />
    <link href="css/datatables-override.css" rel="stylesheet" type="text/css" />



    <!-- MultiplayerGame -->
    <link href="css/gamestyle.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="js/game_highlighting.js"></script>

    <!-- Upload page -->
    <link href="css/uploadcut.css" rel="stylesheet" type="text/css" />

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
            $('table.mutant-table').DataTable( {
                "pagingType": "full_numbers",
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
        } );
    </script>

</head>

<body class="page-grid">
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.Test" %>
<%@ page import="org.codedefenders.User" %>
<%@ page import="org.codedefenders.Mutant" %>
<%@ page import="org.codedefenders.duel.DuelGame" %>
<%@ page import="org.codedefenders.story.StoryGame" %>
<%@ page import="org.codedefenders.Constants" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<%@ page import="org.codedefenders.Role" %>
<%@ page import="static org.codedefenders.GameState.ACTIVE" %>
<%@ page import="org.codedefenders.GameClass" %>
<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%@ page import="java.util.ArrayList" %>
    <% DuelGame game = (DuelGame) session.getAttribute("game"); %>
    <% StoryGame puzzle = (StoryGame) session.getAttribute("puzzle"); %>
<div class="menu-top bg-grey bg-plus-4 text-white" style="padding-bottom:0px;">
    <div class="full-width">
        <div class="nest">
            <div class="crow fly nogutter">
                <div>
                    <div>
                        <div class="tabs-blue-grey">
                            <a href="/" class="main-title" id="site-logo">
                                <div class="crow">
                                    <div class="ws-12" style="font-size: 36px; text-align: center;">
                                        <span><img class="logo" href="/" src="images/logo.png"/></span>
                                        Code Defenders
                                    </div>
                                </div>
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="bg-plus-3" style="padding:2px 0; margin-bottom: 0px; margin-top: 5px;"></div>
    </div>
</div>
