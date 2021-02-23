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

<%-- The min-height and padding style attributes are here to make the footer a bit smaller, because it looks
     weird if it's the same size of as the header. --%>
<nav class="navbar navbar-cd" id="footer" style="min-height: 0;">
    <div> <%-- class="container-fluid" --%>
        <div id="navbar-controls-footer">
            <ul class="nav navbar-nav">
                <li>
                    <a id="footerAbout" class="text-white btn tab-link bg-minus-1"
                       href="<%=request.getContextPath() + Paths.ABOUT_PAGE%>"
                       style="padding-top: .5em; padding-bottom: .5em;">About CodeDefenders</a></li>
                <li><a id="footerContact" class="text-white btn tab-link bg-minus-1"
                       href="<%=request.getContextPath() + Paths.CONTACT_PAGE%>"
                       style="padding-top: .5em; padding-bottom: .5em;">Contact Us</a></li>
            </ul>
        </div>
    </div>
</nav>
