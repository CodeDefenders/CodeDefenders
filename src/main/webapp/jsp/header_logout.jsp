<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ include file="/jsp/header_base.jsp" %>
<script type="text/javascript">
    $(document).ready(function() {
        $('#messages-div').delay(10000).fadeOut();
    });
</script>
<div class="menu-top bg-light-blue .minus-2 text-white" style="padding: 5px;">
    <div class="full-width" style="padding-top: 3px;">
        <div class="ws-12 container" style="text-align: right; clear:
        both; width: 100%;">
            <div>
                <a id="site-logo" class="main-title text-white tab-link bg-minus-1"
                   href="${pageContext.request.contextPath}/">
                    <img class="logo" href="${pageContext.request.contextPath}/"
                         src="images/logo.png" style="float:left; margin-left: 10px; margin-right: 10px"/>
                    <div id="home"
                         style="text-align: center; font-size: x-large; padding: 15px 20px 0 0;float: left">
                        Code Defenders
                    </div>
                </a>
            </div>
            <button type="button"
                    class="navbar-toggle tex-white buton tab-link bg-minus-1" data-toggle="collapse"
                    data-target="#bs-example-navbar-collapse-1">
                Menu <span class="glyphicon glyphicon-plus"></span>
            </button>
            <div class="col-md-9">
                <ul class="crow fly no-gutter navbar navbar-nav collapse navbar-collapse"
                    id="bs-example-navbar-collapse-1"
                    style="z-index: 1000; text-align: center; list-style:none;
                width: 100%; float: right">

                    <%if (!pageTitle.equals("Login")) {%>
                    <li style="float: none"><a class="text-white button tab-link bg-minus-1"
                                               href="login" style="width:100%;">Login</a></li>
                    <%}%>

                    <li style="float: none"><a
                            class="text-white button tab-link bg-minus-1"
                            href="#research" style="width:100%;">Research</a></li>
                    <li style="float: none"><a class="text-white button tab-link bg-minus-1"
                                               href="help" style="width:100%;">Help</a></li>
                </ul>
            </div>
        </div>
    </div>
</div>
<%
    ArrayList<String> messages = (ArrayList<String>) request.getSession().getAttribute("messages");
    request.getSession().removeAttribute("messages");
    if (messages != null && !messages.isEmpty()) {
%>
<div class="alert alert-info" id="messages-div">
    <% for (String m : messages) { %>
    <pre><strong><%=m%></strong></pre>
    <% } %>
    <script> $('#messages-div').delay(10000).fadeOut(); </script>
</div>
<% } %>
