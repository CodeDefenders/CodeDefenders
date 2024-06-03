<%@ tag pageEncoding="UTF-8" %>

<%@ attribute name="title" required="true" %>
<%@ attribute name="statusCode" required="true" %>
<%@ attribute name="shortDescription" required="true" %>
<%@ attribute name="message" required="true" fragment="true" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<head>
    <meta content="width=device-width, initial-scale=1, maximum-scale=1" name="viewport">
    <title>${title}</title>
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
    <h2>${statusCode}</h2>
    <h3>${shortDescription}</h3>
    <hr/>
    <jsp:invoke fragment="message"/>
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
