<%@ tag pageEncoding="UTF-8" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ tag import="org.codedefenders.util.Paths" %>

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
