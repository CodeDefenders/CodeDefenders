<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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
<%@ page import="org.codedefenders.execution.KillMap"%>
<%@ page import="org.codedefenders.execution.KillMap.KillMapJob"%>
<%@ page import="org.codedefenders.execution.KillMap.KillMapJob.Type"%>
<%@ page import="org.codedefenders.execution.KillMapProcessor"%>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings"%>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings.SettingsDTO"%>

<%
    String pageTitle = null;
%>
<%@ include file="/jsp/header_main.jsp"%>

<div class="full-width">
	<%
    request.setAttribute("adminActivePage", "adminAnalytics");
	   
    KillMapProcessor killMapProcessor = (KillMapProcessor) getServletContext().getAttribute(KillMapProcessor.NAME);
    int gamePendingJobs = 0;
    int classPendingJobs = 0;
    for( KillMap.KillMapJob pendingJob : killMapProcessor.getPendingJobs() ){
        switch (pendingJob.getType()) {
        case CLASS:
            classPendingJobs += 1;
            break;
        case GAME:
            gamePendingJobs += 1;
            break;
        }
    }
    
	%>
	<%@ include file="/jsp/admin_navigation.jsp"%>

	<h3>KillMaps</h3>
	<h4>Status</h4>
	<%-- Use this as starting point to provide more fine grained details and controls: --%>
	<p>
		Pending KillMap Jobs for Games:
		<%= gamePendingJobs %>
		</p>
	<p>
        Pending KillMap Jobs for Classes:
        <%= classPendingJobs %>
        </p>

	<h4>Submit KillMap Jobs</h4>
	<div class="full-width">
		<form id="killmapProcessor" name="killmapProcessor"
			action="admin/analytics/killmaps" method="post"></form>
	</div>

	<h4>Settings</h4>
	<div class="full-width">
		<form id="killmapProcessor" name="killmapProcessor"
			action="admin/analytics/killmaps" method="post">

			<%
			    for (AdminSystemSettings.SettingsDTO setting : AdminDAO.getSystemSettings()) {
			        if( ! AdminSystemSettings.SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION.equals( setting.getName())){
			            continue;
			        }
			        String readableName = setting.getName().name().toLowerCase().replace("_", " ");
			        String explanation = setting.getName().toString();
			        switch (setting.getType()) {
			        case BOOL_VALUE:
			%>
			<div class="input-group" id="<%="group_"+setting.getName().name()%>">
				<span class="input-group-addon"
					style="width: 250px; text-align: left;" title="<%=explanation%>"><%=readableName%>
				</span> <input type="checkbox" id="<%=setting.getName().name()%>"
					name="<%=setting.getName().name()%>" class="form-control"
					data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
					data-onstyle="primary" data-offstyle=""
					<%=setting.getBoolValue() ? "checked" : ""%>>
			</div>
            <%
                         break;
                    }
			   }
		    %>
			<br>
			<button type="submit" class="btn btn-primary" name="saveSettingsBtn"
				id="saveSettingsBtn">Save</button>
		</form>
	</div>
</div>
<%@ include file="/jsp/footer.jsp"%>