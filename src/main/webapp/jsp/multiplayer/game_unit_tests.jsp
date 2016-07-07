<div class="col-md-6">
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
                <% if (p.equals(Participance.DEFENDER) || p.equals(Participance.CREATOR) || mg.getLevel().equals(Game.Level.EASY)){
                %><%=tc%><%
                } else { %>
//Only available to attackers in easy game mode.
                <%}%>
        </textarea></pre></div>
        <%
            }
            if (!isTests) {%>
        <div><h4>T0</h4><pre class="readonly-pre"><textarea class="utest" cols="20" rows="10">
//No Unit Tests Found
//
//&#9785;</textarea></pre></div>
        <%}
        %>
    </div> <!-- slider single-item -->
</div> <!-- col-md-6 left bottom -->