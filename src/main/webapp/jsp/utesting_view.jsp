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
<!DOCTYPE html>
<html>

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- Title -->
    <title>Code Defenders - Defend</title>

    <!-- App context -->
    <base href="${pageContext.request.contextPath}/">

    <!-- jQuery -->
    <script src="js/jquery.min.js" type="text/javascript"></script>

    <!-- Slick -->
    <link href="css/slick_1.5.9.css" rel="stylesheet" type="text/css"/>
    <script src="js/slick_1.5.9.min.js" type="text/javascript"></script>

    <!-- Bootstrap -->
    <script src="js/bootstrap.min.js" type="text/javascript"></script>
    <link href="css/bootstrap.min.css" rel="stylesheet" type="text/css"/>

    <!-- Codemirror -->
    <script src="codemirror/lib/codemirror.js" type="text/javascript"></script>
    <script src="codemirror/mode/clike/clike.js" type="text/javascript"></script>
    <script src="codemirror/mode/diff/diff.js" type="text/javascript"></script>
    <link href="codemirror/lib/codemirror.css" rel="stylesheet" type="text/css"/>

    <!-- MultiplayerGame -->
    <link href="css/gamestyle.css" rel="stylesheet" type="text/css"/>

    <script>
        $(document).ready(function () {
            $('.single-item').slick({
                arrows: true,
                infinite: true,
                speed: 300,
                draggable: false
            });
            $('#messages-div').delay(10000).fadeOut();
        });
    </script>
</head>

<body>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.duel.DuelGame" %>
<%@ page import="static org.codedefenders.game.GameState.FINISHED" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.codedefenders.util.Paths" %>
<% DuelGame uTestingSession = (DuelGame) request.getAttribute("game"); %>

<nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse"
                    data-target="#navbar-collapse-1" aria-expanded="false">
            </button>
            <a class="navbar-brand" href="<%=request.getContextPath() %>/">
                <span><img class="logo" href="<%=request.getContextPath()%>/" src="images/logo.png"/></span>
                Code Defenders
            </a>
        </div>
        <div class="collapse navbar-collapse" id="navbar-collapse-1">
            <ul class="nav navbar-nav navbar-left">
                <% if (uTestingSession != null) { %>
                <li class="navbar-text">Unit Testing Session ID: <%= uTestingSession.getId() %>
                </li>
                <li class="navbar-text">Test <%= uTestingSession.getCurrentRound() %>
                    of <%= uTestingSession.getFinalRound() %>
                </li>
                <% } %>
            </ul>
            <ul class="nav navbar-nav navbar-right">
                <li>
                    <p class="navbar-text">
                        <span class="glyphicon glyphicon-user" aria-hidden="true"></span>
                        <%=request.getSession().getAttribute("username")%>
                    </p>
                </li>
                <li><input type="submit" form="logout" class="btn btn-inverse navbar-btn" value="Log Out"/></li>
            </ul>
        </div>
    </div>
</nav>

<form id="logout" action="<%=request.getContextPath()  + Paths.LOGIN%>" method="post">
    <input type="hidden" name="formType" value="logOut">
</form>

<%
    ArrayList<String> messages = (ArrayList<String>) request.getSession().getAttribute("messages");
    request.getSession().removeAttribute("messages");
    if (messages != null && !messages.isEmpty()) {
%>
<div class="alert alert-info" id="messages-div">
    <% for (String m : messages) { %>
    <pre><strong><%=m%></strong></pre>
    <% } %>
</div>
<% } %>

<%
    if (uTestingSession == null) {
%>
<div class="row-fluid">
    <h3> No unit testing session available at the moment. </h3>
</div>
<% } else {
    final GameClass cut = uTestingSession.getCUT();
%>

<div class="row-fluid">
    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <pre class="readonly-pre"><textarea
                class="readonly-textarea" id="sut" name="cut" cols="80"
                rows="30"><%=cut.getAsHTMLEscapedString()%></textarea></pre>
    </div> <!-- col-md6 left -->
    <div class="col-md-6" id="utest-div">
        <h3> Write a new JUnit test here
            <% if (uTestingSession.getState().equals(FINISHED)) {%>
            <button class="btn btn-primary btn-game btn-right disabled">Finished</button>
            <%} else { %>
            <button type="submit" class="btn btn-primary btn-game btn-right" form="def">Submit</button>
            <%} %>
        </h3>
        <form id="def" action="<%=request.getContextPath() + Paths.UTESTING_PATH%>" method="post">
            <input type="hidden" name="gameId" value=<%=uTestingSession.getId()%>>
            <%
                String testCode;
                String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
                request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
                if (previousTestCode != null) {
                    testCode = previousTestCode;
                } else
                    testCode = cut.getHTMLEscapedTestTemplate();
            %>
            <pre><textarea id="code" name="test" cols="80" rows="30"><%=testCode%></textarea></pre>
        </form>
    </div> <!-- col-md6 right top -->
</div> <!-- row-fluid 1 -->

<div class="row-fluid">
    <div class="col-md-6" id="submitted-div">
        <h3>JUnit tests </h3>
        <div class="slider single-item">
            <%
                boolean hasTests = false;
                for (Test t : uTestingSession.getTests()) {
                    hasTests = true;
            %>
            <div>
                <h4>Test <%= t.getId() %>
                </h4>
                <pre class="readonly-pre"><textarea class="utest" cols="20"rows="10"><%=t.getAsHTMLEscapedString()%></textarea></pre>
            </div>
            <%
                }
                if (!hasTests) {%>
            <div><h3></h3>
                <p> There are currently no tests </p></div>
            <%
                }
            %>
        </div> <!-- slider single-item -->
    </div> <!-- col-md-6 left bottom -->
</div>
<% } %>
<script>
    var editorTest = CodeMirror.fromTextArea(document.getElementById("code"), {
        lineNumbers: true,
        indentUnit: 4,
        indentWithTabs: true,
        matchBrackets: true,
        mode: "text/x-java",
    });
    editorTest.on('beforeChange', function (cm, change) {
        var text = cm.getValue();
        var lines = text.split(/\r|\r\n|\n/);
        var readOnlyLines = [0, 1, 2, 3, 4, 5, 6, 7];
        var readOnlyLinesEnd = [lines.length - 1, lines.length - 2];
        if (~readOnlyLines.indexOf(change.from.line) || ~readOnlyLinesEnd.indexOf(change.to.line)) {
            change.cancel();
        }
    });
    editorTest.setSize("100%", 500);
    var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
        lineNumbers: true,
        matchBrackets: true,
        mode: "text/x-java",
        readOnly: true
    });
    editorSUT.setSize("100%", 500);
    /* Submitted tests */
    var x = document.getElementsByClassName("utest");
    var i;
    for (i = 0; i < x.length; i++) {
        CodeMirror.fromTextArea(x[i], {
            lineNumbers: true,
            matchBrackets: true,
            mode: "text/x-java",
            readOnly: true
        });
    }
</script>
</body>
</html>
