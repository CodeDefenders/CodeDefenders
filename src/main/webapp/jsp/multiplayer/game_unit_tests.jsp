<div class="col-md-6">
    <h2> JUnit tests </h2>
    <div class="slider single-item">
        <%
            isTests = false;
            for (Test t : mg.getExecutableTests()) {
                isTests = true;
                String tc = "";
                for (String l : t.getHTMLReadout()) { tc += l + "\n"; }
        %>
        <div><h4>Test <%= t.getId() %></h4><pre class="readonly-pre"><textarea class="utest" cols="20" rows="10"><%=tc%></textarea></pre></div>
        <%
            }
            if (!isTests) {%>
        <div><h2></h2><p> There are currently no tests </p></div>
        <%}
        %>
    </div> <!-- slider single-item -->
</div> <!-- col-md-6 left bottom -->