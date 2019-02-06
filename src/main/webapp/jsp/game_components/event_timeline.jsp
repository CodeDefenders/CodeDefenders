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
<%--
    Displays a list of events on a timeline based on https://codepen.io/jasondavis/pen/fDGdK
--%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.codedefenders.model.Event"%>
<%@page import="java.util.List"%>
<%@page import="org.codedefenders.game.Role"%>
<script type="text/javascript">
	/* var file = location.pathname.split( "/" ).pop(); */
	var file = "css/timeline.css"

	var link = document.createElement("link");
	link.href = file.substr(0, file.lastIndexOf(".")) + ".css";
	link.type = "text/css";
	link.rel = "stylesheet";
	link.media = "screen,print";

	document.getElementsByTagName("head")[0].appendChild(link);
</script>

<div class="container">
	<div class="page-header">
    <h1 id="timeline">Timeline</h1>
  </div>
	<ul class="timeline">
		<%
		    List<Event> events = new ArrayList<Event>();
		    if (request.getAttribute("events") != null) {
		        events.addAll((List<Event>) request.getAttribute("events"));
		    }

		    for (Event e : events) {
		        String position = "";
		        String icon = "";
		        String title = e.getEventType() + " - " + e.getEventStatus();
		        String timestamp = "11 hours ago via Twitter";
		        // This is mostly a message templage
		        String body = e.getMessage();
		        switch (e.getRole()) {
		        case ATTACKER:
		              icon = "glyphicon glyphicon-check";
		        %>
		        <li>
	                <div class="timeline-badge"><i class="<%=icon%>"></i></div>
	                <div class="timeline-panel">
	                    <div class="timeline-body"><p><%=body%></p></div>
	                </div>
                </li>
                <%	                    
		            break;
		        case DEFENDER:
		            icon = "glyphicon glyphicon-check";
		        %>
                    <li class="timeline-inverted">
	                    <div class="timeline-badge"><i class="<%=icon%>"></i></div>
	                    <div class="timeline-panel">
	                        <div class="timeline-body"><p><%=body%></p></div>
	                    </div>
	                </li>
                <%
		            break;
		        case CREATOR:
		            icon = "glyphicon glyphicon-check";
		            %>
		            <li>
                    <div class="timeline-badge"><i class="<%=icon%>"></i></div>
                    <div class="timeline-panel">
                        <div class="timeline-body"><p><%=body%></p></div>
                    </div>
                    <div class="timeline-panel-right">
                        <div class="timeline-body"><p><%=body%></p></div>
                    </div>
                </li>
                <%
		            continue;
		        default:
		            continue;
		        }
		    }
		%>
		 <%-- Event visualization template --%>
		<%--		<li>
      <div class="timeline-badge"><i class="glyphicon glyphicon-check">
				<i class="<%=icon%>"></i>
			</div>
			<div class="timeline-panel">
				<div class="timeline-heading">
					Timing Info
					<h4 class="timeline-title"><%=title%></h4>
					Timing Info
					<p>
						<small class="text-muted"><i
							class="glyphicon glyphicon-time"></i> <%=timestamp%></small>
					</p>
				</div>
				<div class="timeline-body">
					<p><%=body%></p>
					<!-- <div class="btn-group">
                        <button type="button"
                            class="btn btn-primary btn-sm dropdown-toggle"
                            data-toggle="dropdown">
                            <i class="glyphicon glyphicon-cog"></i> <span class="caret"></span>
                        </button>
                        <ul class="dropdown-menu" role="menu">
                            <li><a href="#">Action</a></li>
                            <li><a href="#">Another action</a></li>
                            <li><a href="#">Something else here</a></li>
                            <li class="divider"></li>
                            <li><a href="#">Separated link</a></li>
                        </ul>
                    </div> -->
				</div>
			</div>
		</li> --%>
	</ul>
</div>
