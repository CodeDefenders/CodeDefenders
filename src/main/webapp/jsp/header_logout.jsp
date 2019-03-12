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
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ include file="/jsp/header_base.jsp" %>
<script type="text/javascript">
    $(document).ready(function() {
        $('#messages-div').delay(10000).fadeOut();
    });
</script>
<div class="menu-top bg-light-blue .minus-2 text-white" style="padding: 5px;">
    <div class="full-width">
        <div class="ws-12 container" style="text-align: right; clear:
        both; width: 100%; padding: 0">

            <!-- toggle menu button for small frames -->
            <button type="button"
                    class="navbar-toggle collapsed text-white button tab-link bg-minus-1"
                    data-toggle="collapse"
                    data-target="#bs-example-navbar-collapse-1"
                    style="margin-top: 3px">
                Menu <span class="glyphicon glyphicon-plus"></span>
            </button>

            <!-- logo and pagetitle -->
            <a id="site-logo" class="navbar-brand site-logo main-title text-white tab-link"
               href="${pageContext.request.contextPath}/" style="padding-left: 0">
                <img class="logo" href="${pageContext.request.contextPath}/"
                     src="images/logo.png" style="float:left; margin-left: 10px; margin-right: 10px"/>
                <div id="headerhome"
                     style="text-align: center; font-size: x-large; padding: 15px 20px 0 0; float: left">
                    Code Defenders
                </div>
            </a>

            <!-- navigation bar -->
            <div id="bs-example-navbar-collapse-1" class="navbar-collapse collapse">
                <ul class="crow no-gutter nav navbar-nav" style="display: flow-root; position: relative; z-index: 1000">
                    <%if (!pageTitle.equals("Login")) {%>
                    <li class="col-md-4"><a class="text-white button tab-link bg-minus-1"
                                               href="login" style="width:100%; margin-right: 80px">Login</a></li>
                    <%}%>

                    <li class="col-md-4"><a
                            class="text-white button tab-link bg-minus-1"
                            href="#research" style="width:100%; margin-right: 60px">Research</a></li>
                    <li class="col-md-4"><a class="text-white button tab-link bg-minus-1"
                                               href="help" style="width:100%; margin-right: 90px">Help</a></li>
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
