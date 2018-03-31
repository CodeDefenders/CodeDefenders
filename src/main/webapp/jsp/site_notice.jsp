<%@ page import="org.codedefenders.AdminSystemSettings" %>
<%@ page import="org.codedefenders.util.AdminDAO" %>
<% String pageTitle = "About CodeDefenders"; %>

<%@ include file="/jsp/header_base.jsp" %>

<div class="container" style=" max-width: 50%; min-width: 25%; ">
    <h2 style="text-align: center">Site Notice</h2>
    <div class="panel panel-default" style="padding:25px">
        <div class="panel-body">
            <%=AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.SITE_NOTICE).getStringValue()%>
        </div>
    </div>

    <h2 style="text-align: center">About CodeDefenders</h2>

    <div class="panel panel-default" style="padding:25px;">

        <div class="panel-body">

            CodeDefenders is developed at The University of Sheffield <br>Supported by the

            <a href="https://www.sheffield.ac.uk/sure">SURE (Sheffield Undergraduate Research Experience)</a>

            scheme <br>

        </div>

    </div>
</div>

<%@ include file="/jsp/footer.jsp" %>