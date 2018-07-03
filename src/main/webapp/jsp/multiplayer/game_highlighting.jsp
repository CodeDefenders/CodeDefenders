<%@ page import="com.google.gson.Gson" %>
<%@ page import="java.util.stream.Collectors" %>
<%Gson gson = new Gson();%>

testMap = {<% for (Integer i : linesCovered.keySet()){%>
<%= i%>: [<%= linesCovered.get(i).stream().map(t -> Integer.toString(t.getId())).distinct().collect(Collectors.joining(","))%>],
<% } %>
};


highlightCoverage = function(){
highlightLine([<% for (Integer i : linesCovered.keySet()){%>
[<%=i%>, <%=((float)linesCovered.get(i).size() / (float) tests.size())%>],
<% } %>], COVERED_COLOR, "<%="#" + codeDivName%>");
};

getMutants = function(){
    return JSON.parse("<%= gson.toJson(mutants).replace("\"", "\\\"") %>");
}

showMutants = function(){
mutantLine("<%="#" + codeDivName%>", <%= role.equals(Role.DEFENDER)? "true" : "false" %>);
};

var updateCUT = function(){
    showMutants();
    highlightCoverage();
};

var updateCUTTimeout = 15;

editorSUT.on("viewportChange", function(){
    setTimeout(updateCUT, updateCUTTimeout);
});

editorSUT.on("cursorActivity", function(){
    setTimeout(updateCUT, updateCUTTimeout);
});

editorSUT.on("gutterContextMenu", function(line, gutter){
    setTimeout(updateCUT, updateCUTTimeout);
});

$(document).ready(function(){
    setTimeout(updateCUT, updateCUTTimeout);
});

//inline due to bug in Chrome?
$(window).resize(function (e){setTimeout(updateCUT, 500);});