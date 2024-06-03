<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="messages" type="org.codedefenders.beans.message.MessagesBean"--%>

<c:if test="${messages.count > 0}">
    <div class="mx-3" id="messages">
        <c:forEach items="${messages.messages}" var="message">
            <div id="message-${message.id}" class="alert alert-primary alert-dismissible fade show" role="alert">
                <%-- Don't escape text here; message.getText() already escapes the text. --%>
                <pre class="m-0"><c:out value="${message.text}" escapeXml="false"/></pre>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:forEach>
    </div>

    <script type="module">
        import $, {Alert} from '';


        $(document).ready(() => {
            <c:forEach items="${messages.messages}" var="message">
                <c:if test="${message.fadeOut}">
                    setTimeout(function () {
                        const element = document.getElementById('message-${message.id}');
                        const alert = new Alert(element);
                        alert.close();
                    }, 10000);
                </c:if>
            </c:forEach>
        });
    </script>

    ${messages.clear()}
</c:if>
