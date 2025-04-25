<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

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
        import {Alert} from '${url.forPath("/js/bootstrap.mjs")}';
        import $ from '${url.forPath("/js/jquery.mjs")}';


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
