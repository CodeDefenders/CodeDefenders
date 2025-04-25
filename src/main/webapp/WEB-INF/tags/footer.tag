<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ tag pageEncoding="UTF-8" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ tag import="org.codedefenders.util.Paths" %>

<%--
    Provides the footer with links to static pages.
--%>

<nav class="navbar navbar-expand-md navbar-cd py-0" id="footer">
    <div class="container-fluid justify-content-center">
        <div id="footer-navbar-controls">
            <ul class="navbar-nav mx-auto">
                <li class="nav-item mx-2">
                    <a class="nav-link" id="footer-about" href="${url.forPath(Paths.ABOUT_PAGE)}">About CodeDefenders</a>
                </li>
                <li class="nav-item mx-2">
                    <a class="nav-link" id="footer-contact" href="${url.forPath(Paths.CONTACT_PAGE)}">Contact Us</a>
                </li>
                <li class="nav-item mx-2">
                    <a class="nav-link" id="footer-help" href="${url.forPath(Paths.HELP_PAGE)}">Help</a>
                </li>
                <li class="nav-item mx-2">
                    <a class="nav-link" id="footer-imprint" href="${url.forPath(Paths.IMPRINT_PAGE)}">Imprint and Privacy Policy</a>
                </li>
            </ul>
        </div>
    </div>
</nav>
