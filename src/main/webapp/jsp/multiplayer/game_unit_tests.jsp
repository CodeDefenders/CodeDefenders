<% if (role.equals(Role.DEFENDER) || role.equals(Role.CREATOR) || mg.getLevel().equals(Game.Level.EASY)){
%>
<div class="ws-6">
    <h2> JUnit tests </h2>
    <div class="slider single-item">
        <%
            isTests = false;
            for (Test t : tests) {
                for (Integer luc : t.getLineCoverage().getLinesUncovered()){
                    if (!linesUncovered.contains(luc) && !linesCovered.containsKey(luc)){
                        linesUncovered.add(luc);
                    }
                }


                for (Integer lc : t.getLineCoverage().getLinesCovered()){
                    if (!linesCovered.containsKey(lc)){
                        linesCovered.put(lc, new ArrayList<Test>());
                    }

                    if (linesUncovered.contains(lc)){
                        linesUncovered.remove(lc);
                    }

                    linesCovered.get(lc).add(t);
                }

                isTests = true;
                String tc = "";
                for (String l : t.getHTMLReadout()) { tc += l + "\n"; }
        %>
        <div><h4>Test <%= t.getId() %></h4><pre class="readonly-pre"><textarea class="utest" cols="20" rows="10">
                <%=tc%>
        </textarea></pre></div>
        <%
            }
            if (!isTests) {%>
        <div><h2></h2><p> There are currently no tests </p></div>
        <%}
        %>
    </div> <!-- slider single-item -->
</div> <!-- col-md-6 left bottom -->
<% } %>