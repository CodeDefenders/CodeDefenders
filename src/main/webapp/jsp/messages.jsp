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

<%@ page import="org.codedefenders.util.Paths" %>

<div id="toasts" class="position-fixed top-0 end-0 p-3 d-flex flex-column gap-1" style="z-index: 11"></div>
<c:if test="${messages.count > 0}">
    <div class="mx-3" id="messages">
        <c:forEach items="${messages.getAlertMessages(true)}" var="message">
            <div id="message-${message.id}" class="alert alert-primary alert-dismissible fade show" role="alert">
                    <%-- Don't escape text here; message.getText() already escapes the text. --%>
                <c:if test="${message.title != '' && message.secondary != ''}">
                    <div class="d-flex justify-content-between align-content-center">
                        <strong><c:out value="${message.title}"/></strong>
                        <i><c:out value="${message.secondary}"/></i>
                    </div>
                </c:if>
                <pre class="m-0"><c:out value="${message.text}" escapeXml="false"/></pre>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:forEach>
    </div>

    <script type="module">
        import $ from '${url.forPath("/js/jquery.mjs")}';
        import {ShowToasts} from '${url.forPath("/js/codedefenders_main.mjs")}';

        const API_URL = '${url.forPath(Paths.API_MESSAGES)}';


        $(document).ready(async () => {
            const response = await fetch(`\${API_URL}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            if (!response.ok) {
                console.log("Error fetching messages: " + response.status);
                return;
            }
            const list = await response.json();

            list.forEach((message) => {
                const title = message.title;
                const secondary = message.secondary;
                const body = message.text;
                ShowToasts.showToast({title: title, secondary: secondary, body: body});
            });
        });
    </script>


</c:if>
