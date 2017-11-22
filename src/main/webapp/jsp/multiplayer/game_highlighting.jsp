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
<% for(Mutant mm : mutantLines.get(line)){%>
<%= mm.getId() %>,
<%}%>
]],
<%
    } %>
],"<%="#" + codeDivName%>", <%= role.equals(Role.DEFENDER)? "true" : "false" %>);
};


showMutantsEquiv = function(){
mutantEquivLine([
<% for (Integer line : mutantEquivPending.keySet()) {
%>
[<%= line %>,
<%= mutantEquivPending.get(line).size() %>, [
<% for(Mutant mm : mutantEquivPending.get(line)){%>
<%= mm.getId() %>,
<%}%>
]],
<%
    } %>
],"<%="#" + codeDivName%>", <%= role.equals(Role.DEFENDER)? "true" : "false" %>);
};

showKilledMutants = function(){
mutantKilledLine([
<% for (Integer line : mutantKilledLines.keySet()) {
%>
[<%= line %>,
<%= mutantKilledLines.get(line).size() %>, [
<% for(Mutant mm : mutantKilledLines.get(line)){%>
<%= mm.getId() %>,
<%}%>
]],
<%
    } %>
],"<%="#" + codeDivName%>");
};
editorSUT.on("viewportChange", function(){
    showMutants();
    showMutantsEquiv();
    showKilledMutants();
    highlightCoverage();
});
$(document).ready(function(){
    showMutants();
    showMutantsEquiv();
    showKilledMutants();
    highlightCoverage();
});
