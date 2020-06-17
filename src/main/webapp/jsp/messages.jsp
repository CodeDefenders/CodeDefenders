<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${messages.count > 0}">
    <div class="alert alert-info" id="messages-div" style="width: 98.9vw">
        <a href="" class="close" data-dismiss="alert" aria-label="close">&times;</a><br/>

        <c:forEach items="${messages.messages}" var="message">
            <%-- Don't escape text here; message.getText() already escapes the text. --%>
            <pre id="message-${message.id}"><strong><c:out value="${message.text}" escapeXml="false"/></strong></pre>
        </c:forEach>
    </div>

    <c:if test="${messages.fadeOut}">
        <script>
            $(document).ready(() => $('#messages-div').delay(10000).fadeOut());
        </script>
    </c:if>

    ${messages.clear()}
</c:if>
