<%@ page import="org.codedefenders.AdminSystemSettings" %>
<%@ page import="org.codedefenders.util.AdminDAO" %>
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
                <li>Sabina Galdobin (University of Passau)</li>
                <li><a href="http://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/">Alessio Gambi (University of Passau)</a></li>
                <li>Marvin Kreis (University of Passau)</li>
                <li>Rob Sharp (The University of Sheffield)</li>
                <li>Lorenz Wendlinger (University of Passau)</li>
                <li>Philemon Werli (University of Passau)</li>
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
    <h2 style="text-align: center">Site Notice</h2>
    <div class="panel panel-default" style="padding:25px">
        <div class="panel-body">
            <%=AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.SITE_NOTICE).getStringValue()%>
        </div>
    </div>


</div>

<%@ include file="/jsp/footer.jsp" %>