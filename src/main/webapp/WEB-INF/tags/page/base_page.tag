<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>

<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="additionalImports" required="false" fragment="true" %>

<!DOCTYPE html>
<html>
    <head>
        <title>${title}</title>

        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

        <t:imports>
            <jsp:attribute name="additionalImports">
                <jsp:invoke fragment="additionalImports"/>
            </jsp:attribute>
        </t:imports>

        <t:js_init/>
    </head>
    <body>
        <jsp:doBody/>
    </body>
</html>
