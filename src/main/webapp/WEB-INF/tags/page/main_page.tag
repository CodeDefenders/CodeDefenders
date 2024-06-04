<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>

<%@ attribute name="title" required="true" type="java.lang.String"
              description="Title of the page" %>
<%@ attribute name="additionalImports" required="false" fragment="true"
              description="Additional CSS or JavaScript tags to include in the head." %>

<%--
    Adds the header and footer along with other common elements to the base page.
--%>

<p:base_page title="${title}">
    <jsp:attribute name="additionalImports">
        <jsp:invoke fragment="additionalImports"/>
    </jsp:attribute>

    <jsp:body>
        <t:navbar/>
        <jsp:include page="/jsp/game_components/progress_bar_common.jsp"/>
        <jsp:include page="/jsp/messages.jsp"/>
        <div id="content">
            <jsp:doBody/>
        </div>
        <t:footer/>
    </jsp:body>
</p:base_page>
