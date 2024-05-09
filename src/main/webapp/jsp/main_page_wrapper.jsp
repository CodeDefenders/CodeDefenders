<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<p:main_page title="${requestScope.title}">
    <jsp:include page="${requestScope.jspPage}"/>
</p:main_page>
