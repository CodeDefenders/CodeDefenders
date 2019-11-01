<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="java.io.IOException" %>
<% String pageTitle = "About CodeDefenders"; %>

<%
    Object uid = request.getSession().getAttribute("uid");
    Object username = request.getSession().getAttribute("username");
    if (uid != null && username != null){
%>
<%@ include file="/jsp/header.jsp" %>
<%} else {%>
<%@ include file="/jsp/header_logout.jsp" %>
<%}%>

<div class="container" style=" max-width: 50%; min-width: 25%; ">
    <h2 style="text-align: center">About CodeDefenders</h2>

    <div class="panel panel-default" style="padding:25px;">

        <div class="panel-body">

             <%
                String version  = GameClass.class.getPackage().getImplementationVersion();

                // version may now be null: https://stackoverflow.com/questions/21907528/war-manifest-mf-and-version?rq=1
                if (version==null) {
                    Properties prop = new Properties();
                    try {
                        prop.load(request.getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
                        version = prop.getProperty("Implementation-Version");
                    } catch (IOException e) {
                        // Ignore -- if we have no version, then we show no version section
                    }
                }
                if(version != null) {
                    %>
            <h3>Version</h3>
            <p>
                This is Code Defenders version <%= version%>.
            </p>
                <%
                }
                %>

            <h3>Source Code</h3>
            <p>
            CodeDefenders is developed and maintained at the <a href="http://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">Chair of Software Engineering II</a> at the University of Passau and the <a href="https://www2.le.ac.uk/departments/informatics/people/jrojas">University of Leicester</a>.
            </p>
            <p>
            Code Defenders is an open source project. See the <a href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a> project page.
            <p/>
            <p>
            <h3>Contributors</h3>
            <ul>
                <li><a href="http://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">Gordon Fraser (University of Passau)</a></li>
                <li><a href="http://jmrojas.github.io/">Jose Miguel Rojas (University of Leicester)</a></li>
            </ul>
            <ul>
                <li>Ben Clegg (The University of Sheffield)</li>
                <li>Alexander Degenhart (University of Passau)</li>
                <li>Sabina Galdobin (University of Passau)</li>
                <li><a href="http://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">Alessio Gambi (University of Passau)</a></li>
                <li>Marvin Kreis (University of Passau)</li>
                <li>Rob Sharp (The University of Sheffield)</li>
                <li>Lorenz Wendlinger (University of Passau)</li>
                <li><a href="https://github.com/werli">Phil Werli</a> (University of Passau)</li>
                <li>Thomas White (The University of Sheffield)</li>
            </ul>
            </p>

            <h3>Supporters</h3>
            <p>
            <ul>
                <li><a href="https://impress-project.eu/">IMPRESS Project</a> (Improving Engagement of Students in Software Engineering Courses through Gamification)</li>
                <li><a href="https://www.sheffield.ac.uk/sure">SURE (Sheffield Undergraduate Research Experience)</a></li>
                <li><a href="http://royalsociety.org/">Royal Society (Grant RG160969)</a></li>
            </ul>
            </p>
        </div>

    </div>

    <%
        String siteNotice = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.SITE_NOTICE).getStringValue();
        if(!siteNotice.isEmpty()) {
%>
    <h2 style="text-align: center">Site Notice</h2>
    <div class="panel panel-default" style="padding:25px">
        <div class="panel-body">
            <%=siteNotice%>
        </div>
    </div>
<%
    }
%>

</div>

<%@ include file="/jsp/footer.jsp" %>
