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
<%@ page pageEncoding="UTF-8" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<!DOCTYPE html>
<html>

<head>
    <meta content="width=device-width, initial-scale=1, maximum-scale=1" name="viewport">
    <title>You don't have permission to perform this request (403)</title>
    <link rel="icon" href="${url.forPath("/favicon.ico")}" type="image/x-icon">
    <link href="${url.forPath("/css/specific/error_page.css")}" rel="stylesheet">
</head>

<body>
    <div class="content">
        <a href="${url.forPath("/")}" class="branding">
            <img src="${url.forPath("/images/logo.png")}"
                 alt="Code Defenders Logo"
                 width="58">
            <h1>Code Defenders</h1>
        </a>
        <h2>403</h2>
        <h3>You don't have permission to perform this request.</h3>
        <hr/>
        <p>Your accounts permissions do not include the accessed page or performed action.</p>
        <%-- TODO: Info about appyling for teacher roles here? --%>
        <p>Please contact your administrator if you think this is a mistake.</p>
        <div class="go-back" hidden>
            <a href="javascript:history.back()">Go back</a>
        </div>
        <script>
            if (history.length > 1) {
                document.querySelector('.go-back').removeAttribute('hidden');
            }
        </script>
    </div>
</body>

</html>
