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
<div class="menu-top bg-light-blue .minus-2 text-white" style="padding: 5px;">
    <div class="full-width" style="padding-top: 3px;">
        <div class="ws-12 container" style="text-align: right; clear:
        both; margin: 0px; padding: 0px; width: 100%;">
            <button type="button"
                    class="navbar-toggle tex-white buton tab-link bg-minus-1" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                Menu <span class="glyphicon glyphicon-plus"></span>
            </button>
            <ul class="navbar navbar-nav collapse navbar-collapse"
                id="bs-example-navbar-collapse-1"
                style="z-index: 1000; text-align: center; list-style:none;
                 width: 100%;">
                <li><a class="text-white button tab-link bg-minus-1"
                       href="#research" style="width:100%;">Research</a></li>
                <li><a class="text-white button tab-link bg-minus-1"
                       href="#contact" style="width:100%;">Contact</a></li>
                <li><a class="text-white button tab-link bg-minus-1"
                       href="help" style="width:100%;">Help</a></li>
            </ul>
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
    <script> $('#messages-div').delay(10000).fadeOut(); </script>
</div>
<%	} %>