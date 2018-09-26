<%@ page import="com.google.gson.Gson" %>
<%@ page import="java.util.stream.Collectors" %>

<% { %>

<%Gson gson = new Gson();%>

<script type="text/javascript" src="js/game_highlighting.js"></script>

<script>
    testMap = {
        <% for (Integer i : linesCovered.keySet()){%>
            <%= i %>: [<%= linesCovered.get(i).stream().map(t -> Integer.toString(t.getId())).distinct().collect(Collectors.joining(","))%>],
        <% } %>
    };

    highlightCoverage = function(){
    highlightLine([
        <% for (Integer i : linesCovered.keySet()){%>
            [<%=i%>, <%=((float)linesCovered.get(i).size() / (float) tests.size())%>],
        <% } %>], COVERED_COLOR, "<%="#" + codeDivName%>");
    };

    getMutants = function(){
        return JSON.parse("<%= gson.toJson(mutants).replace("\"", "\\\"") %>");
    };

    showMutants = function(){
        mutantLine("<%="#" + codeDivName%>", <%= role.equals(Role.DEFENDER)? "true" : "false" %>);
    };

    var updateCUT = function(){
        showMutants();
        highlightCoverage();
    };

    editorSUT.on("viewportChange", function(){
        updateCUT();
    });
    $(document).ready(function(){
        updateCUT();
    });

    //inline due to bug in Chrome?
    $(window).resize(function (e){setTimeout(updateCUT, 500);});
</script>

<% } %>