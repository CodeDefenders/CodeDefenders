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

var updateCUTTimeout = 60;

var updateTimeout = null;

var scheduleUpdate = function(){
    if (updateTimeout != null){
        clearTimeout(updateTimeout);
        updateTimeout = null;
    }
    updateTimeout = setTimeout(updateCUT, updateCUTTimeout);
}

editorSUT.on("viewportChange", function(){
    scheduleUpdate();
});

editorSUT.on("cursorActivity", function(){
    scheduleUpdate();
});

editorSUT.on("gutterContextMenu", function(line, gutter){
    scheduleUpdate();
});

$(document).ready(function(){
    scheduleUpdate();
});

//inline due to bug in Chrome?
$(window).resize(function (e){setTimeout(updateCUT, 500);});