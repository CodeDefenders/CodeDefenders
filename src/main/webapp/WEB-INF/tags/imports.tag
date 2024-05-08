<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ attribute name="additionalImports" required="false" fragment="true" %>


<!-- Variables -->
<link href="${url.forPath("/css/global/variables.css")}" rel="stylesheet">

<!-- Favicon.ico -->
<link rel="icon" href="${url.forPath("/favicon.ico")}" type="image/x-icon">

<!-- Bootstrap -->
<link href="${url.forPath("/webjars/bootstrap/5.0.1/css/bootstrap.min.css")}" rel="stylesheet">
<link href="${url.forPath("/css/global/bootstrap_customize.css")}" rel="stylesheet">

<!-- Codemirror -->
<link href="${url.forPath("/webjars/codemirror/5.65.2/lib/codemirror.css")}" rel="stylesheet">
<link href="${url.forPath("/webjars/codemirror/5.65.2/addon/dialog/dialog.css")}" rel="stylesheet">
<link href="${url.forPath("/webjars/codemirror/5.65.2/addon/search/matchesonscrollbar.css")}" rel="stylesheet">
<link href="${url.forPath("/webjars/codemirror/5.65.2/addon/hint/show-hint.css")}" rel="stylesheet">
<link href="${url.forPath("/css/global/codemirror_customize.css")}" rel="stylesheet">

<!-- FontAwesome -->
<link href="${url.forPath("/webjars/font-awesome/4.7.0/css/font-awesome.min.css")}" rel="stylesheet">

<!-- DataTables -->
<link href="${url.forPath("/webjars/datatables/1.11.4/css/dataTables.bootstrap5.min.css")}" rel="stylesheet">
<%--
    <link href="${url.forPath("/webjars/datatables-select/1.3.4/css/select.bootstrap5.min.css")}" rel="stylesheet">
    We use custom CSS instead.
--%>
<link href="${url.forPath("/css/global/datatables_customize.css")}" rel="stylesheet">

<!-- Code Defenders CSS -->
<link href="${url.forPath("/css/global/page.css")}" rel="stylesheet">
<link href="${url.forPath("/css/global/common.css")}" rel="stylesheet">
<link href="${url.forPath("/css/global/loading_animation.css")}" rel="stylesheet">
<link href="${url.forPath("/css/global/achievement_notifications.css")}" rel="stylesheet">

<jsp:invoke fragment="additionalImports"/>
