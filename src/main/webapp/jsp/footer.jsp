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
<%@ page import="org.codedefenders.util.Paths" %>

</div> <%-- closes #content --%>

<nav class="navbar navbar-expand-md navbar-cd py-0" id="footer">
    <div class="container-fluid justify-content-center">
        <div id="footer-navbar-controls">
            <ul class="navbar-nav mx-auto">
                <li class="nav-item mx-2">
                    <a class="nav-link" id="footer-about" href="<%=request.getContextPath() + Paths.ABOUT_PAGE%>">About CodeDefenders</a>
                </li>
                <li class="nav-item mx-2">
                    <a class="nav-link" id="footer-contact" href="<%=request.getContextPath() + Paths.CONTACT_PAGE%>">Contact Us</a>
                </li>
                <li class="nav-item mx-2">
                    <a class="nav-link" id="footer-help" href="<%=request.getContextPath() + Paths.HELP_PAGE%>">Help</a>
                </li>
                <li class="nav-item mx-2">
                    <a class="nav-link" id="footer-imprint" href="<%=request.getContextPath() + Paths.IMPRINT_PAGE%>">Imprint and Privacy Policy</a>
                </li>
            </ul>
        </div>
</nav>
