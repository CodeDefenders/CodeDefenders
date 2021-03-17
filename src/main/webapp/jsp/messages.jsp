<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<jsp:useBean id="messages" class="org.codedefenders.beans.message.MessagesBean" scope="request"/>

<c:if test="${messages.count > 0}">
    <div id="messages" style="margin: 0 2rem 0 2rem;">
        <c:forEach items="${messages.messages}" var="message">
            <div id="message-${message.id}" class="alert alert-info alert-dismissible fade in" role="alert">
                <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <%-- Don't escape text here; message.getText() already escapes the text. --%>
                <pre style="margin: 0;"><c:out value="${message.text}" escapeXml="false"/></pre>
            </div>
        </c:forEach>
    </div>

    <script>
        $(document).ready(() => {
            <c:forEach items="${messages.messages}" var="message">
                <c:if test="${message.fadeOut}">
                    setTimeout(() => $('#message-${message.id}').alert('close'), 10000);
                </c:if>
            </c:forEach>
        });
    </script>

    ${messages.clear()}
</c:if>
