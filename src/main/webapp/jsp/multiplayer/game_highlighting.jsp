highlightCoverage = function(){
highlightLine([<% for (Integer i : linesCovered.keySet()){%>
[<%=i%>, <%=((float)linesCovered.get(i).size() / (float) tests.size())%>],
<% } %>], COVERED_COLOR, "<%="#" + codeDivName%>");
};



showMutants = function(){
    mutantLine([
        <% for (Integer line : mutantLines.keySet()) {
        %>
        [<%= line %>,
            <%= mutantLines.get(line).size() %>, [
            <% for(MultiplayerMutant mm : mutantLines.get(line)){%>
                <%= mm.getId() %>,
            <%}%>
        ]],
        <%
            } %>
    ],"<%="#" + codeDivName%>", <%= role.equals(Role.DEFENDER)? "true" : "false" %>);
};
editorSUT.on("viewportChange", function(){
    showMutants();
    highlightCoverage();
});
$(document).ready(function(){
    showMutants();
    highlightCoverage();
});
