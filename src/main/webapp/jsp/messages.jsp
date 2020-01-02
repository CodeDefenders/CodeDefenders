<%@ page import="org.codedefenders.beans.Message" %>

<jsp:useBean id="messages" class="org.codedefenders.beans.MessageBean" scope="request" />

<% if (messages.getCount() > 0) { %>
    <div class="alert alert-info" id="messages-div" style="width: 98.9vw">
        <a href="" class="close" data-dismiss="alert" aria-label="close">&times;</a><br/>
        <% for (Message message : messages) { %>
            <pre><strong><%=message.getText()%></strong></pre>
            <% if (message.isFadeOut()) { %>
                <script> $('#messages-div').delay(10000).fadeOut(); </script>
            <% } %>
        <% } %>
    </div>
    <% messages.clear(); %>
<% } %>
