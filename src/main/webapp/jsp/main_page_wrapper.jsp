<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--
This is intended as a workaround until we get scriptlets out of the last pages.

Takes the title (title) and JSP page (jspPage) from the request, and includes it in the main layout.
This allows us to include pages with scriptlets without having to create a wrapper for each one.
--%>

<p:main_page title="${requestScope.title}">
    <jsp:include page="${requestScope.jspPage}"/>
</p:main_page>
