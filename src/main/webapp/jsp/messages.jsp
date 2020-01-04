<%@ page import="org.codedefenders.beans.Message" %>

<jsp:useBean id="messages" class="org.codedefenders.beans.MessagesBean" scope="request" />

<% if (messages.getCount() > 0) {
    boolean allFadeOut = true; %>

    <div class="alert alert-info" id="messages-div" style="width: 98.9vw">
        <a href="" class="close" data-dismiss="alert" aria-label="close">&times;</a><br/>

        <% for (Message message : messages.getMessages()) { %>
            <pre id="message-<%=message.getId()%>"><strong><%=message.getText()%></strong></pre>
            <% allFadeOut &= message.isFadeOut(); %>
            <%-- <% if (message.isFadeOut()) { %>
                <script> $('#message-<%=message.getId()%>').delay(10000).fadeOut(); </script>
            <% } else allFadeOut = false; %> --%>
        <% } %>

        <% if (allFadeOut) { %>
            <script> $('#messages-div').delay(10000).fadeOut(); </script>
        <% } %>
    </div>

    <% messages.clear(); %>
<% } %>
