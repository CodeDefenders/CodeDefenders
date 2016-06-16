highlightCoverage = function(){
highlightLine([<% for (Integer i : linesCovered.keySet()){%>
<%=i%>,
<% } %>], COVERED_COLOR, "<%="#" + codeDivName%>");
};
highlightMutants = function(){
highlightLine([<% for (Integer i : linesUncovered){%>
<%=i%>,
<% } %>], UNCOVERED_COLOR, "<%="#" + codeDivName%>");
};
editorSUT.on("viewportChange", function(){
highlightCoverage();
highlightMutants();
});

highlightCoverage();
highlightMutants();