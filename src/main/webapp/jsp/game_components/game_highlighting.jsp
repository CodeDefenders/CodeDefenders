<%@ page import="com.google.gson.Gson" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.Mutant" %>

<%--
    Adds highlighting of coverage (green lines) and mutants (gutter icons) to a CodeMirror editor.

    @param Role role
    @param String codeDivName
    @param List<Test> tests
    @param List<Mutant> mutants
--%>

<% { %>

<%
    String codeDivSelectorTODORENAME = (String) request.getAttribute("codeDivSelector");
    Boolean showEquivalenceButton = (Boolean) request.getAttribute("showEquivalenceButton");
    List<Test> testsTODORENAME = (List<Test>) request.getAttribute("tests");
    List<Mutant> mutantsTODORENAME = (List<Mutant>) request.getAttribute("mutants") ;

    Gson gsonTODORENAME = new Gson();
%>

<%
    HashMap<Integer, ArrayList<Test>> linesCoveredTODORENAME = new HashMap<>();

    for (Test t : testsTODORENAME) {
        for (Integer lc : t.getLineCoverage().getLinesCovered()) {
            if (!linesCoveredTODORENAME.containsKey(lc)) {
                linesCoveredTODORENAME.put(lc, new ArrayList<>());
            }

            linesCoveredTODORENAME.get(lc).add(t);
        }
    }
%>

<script type="text/javascript" src="js/game_highlighting.js"></script>

<script>
    testMap = {
        <% for (Integer i : linesCoveredTODORENAME.keySet()){%>
            <%= i %>: [<%= linesCoveredTODORENAME.get(i).stream().map(t -> Integer.toString(t.getId())).distinct().collect(Collectors.joining(","))%>],
        <% } %>
    };

    highlightCoverage = function(){
    highlightLine([
        <% for (Integer i : linesCoveredTODORENAME.keySet()){%>
            [<%=i%>, <%=((float)linesCoveredTODORENAME.get(i).size() / (float) testsTODORENAME.size())%>],
        <% } %>], COVERED_COLOR, "<%= codeDivSelectorTODORENAME %>");
    };

    getMutants = function(){
        return JSON.parse("<%= gsonTODORENAME.toJson(mutantsTODORENAME).replace("\"", "\\\"") %>");
    };

    showMutants = function(){
        mutantLine("<%= codeDivSelectorTODORENAME %>", <%= showEquivalenceButton ? "true" : "false" %>);
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

    $(window).resize(function (e) {
        setTimeout(updateCUT, 500);
    });
</script>

<% } %>
