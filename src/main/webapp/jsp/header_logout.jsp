<%@ include file="/jsp/header_base.jsp" %>
<script type="text/javascript">
    $(document).ready(function() {
        $('#messages-div').delay(10000).fadeOut();
    });
    var $j = jQuery.noConflict();

    $j(document).ready(function() {
        // Toggle Single Bibtex entry
        $j('a.papercite_toggle').click(function() {
            $j( "#" + $j(this).attr("id") + "_block" ).toggle();
            return false;
        });
    });
</script>
<div class="menu-top bg-light-blue .minus-2 text-white" style="height: 50px; padding: 10px;">
    <div class="full-width" style="padding-top: 3px;">
        <div class="nest">
            <div class="crow">
                <div class="ws-12" style="text-align: left">
                    <div class="inline crow fly">
                        <a class="text-white button tab-link bg-minus-1" href="#research">Research</a>
                        <a class="text-white button tab-link bg-minus-1" href="#contact">Contact</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%
    ArrayList<String> messages = (ArrayList<String>) request.getSession().getAttribute("messages");
    request.getSession().removeAttribute("messages");
    if (messages != null && ! messages.isEmpty()) {
%>
<div class="alert alert-info" id="messages-div">
    <% for (String m : messages) { %>
    <pre><strong><%=m%></strong></pre>
    <% } %>
</div>
<%	} %>