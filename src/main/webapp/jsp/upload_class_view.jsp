<%@ page import="org.codedefenders.singleplayer.PrepareAI" %>
<% String pageTitle=null; %>
<%@ include file="/jsp/header.jsp" %>
<div>
	<div class="w-100 up">
		<h2>Upload Class</h2>
		<div id="divUpload" >
			<form id="formUpload" action="upload" class="form-upload" method="post" enctype="multipart/form-data">
				<input id="classAlias" name="classAlias" type="text" class="form-control" placeholder="Optional class alias" >
				<input type="checkbox" name="prepareForSingle" value="prepare" style="margin-right:5px;">Generate mutants and tests for single-player mode? (It may take a while...)</input>
				<span id="file-select">
					<input id="fileUpload" name="fileUpload" type="file" class="file-loading" accept=".java" />
				</span>
				<span id="submit-button">
					<input type="submit" text="Upload" class="fileinput-upload-button" value="Upload" />
				</span>
			</form>
		</div>
	</div>
	<div class="w-100">
		<h2>Uploaded Classes</h2>
		<div id="classList" >
			<%
				ArrayList<GameClass> gameClasses = DatabaseAccess.getAllClasses();
			%>
			<table class="table table-hover table-responsive table-paragraphs games-table" style="max-width: 100%">
				<tr>
					<th>ID</th>
					<th>Alias</th>
					<th>Name</th>
					<th>Action</th>
				</tr>
				<% if (gameClasses.isEmpty()) { %>
					<tr><td colspan="100%">No classes uploaded.</td></tr>
				<% } else {
					for (GameClass c : gameClasses) {
					%>
						<tr>
							<td><%= c.getId() %></td>
							<td><%= c.getAlias() %></td>
							<td><%= c.getBaseName() %></td>
							<td>
								<form id="aiPrepButton<%= c.getId() %>" action="ai_preparer" method="post" >
									<button type="submit" class="btn btn-primary btn-game btn-right" form="aiPrepButton<%= c.getId() %>"
											<% if (PrepareAI.isPrepared(c)) { %> disabled <% } %>>
										Prepare AI
									</button>
									<input type="hidden" name="formType" value="runPrepareAi" />
									<input type="hidden" name="cutID" value="<%= c.getId() %>" />
								</form>
							</td>
						</tr>
					<% } %>
				<% } %>
			</table>
		</div>
	</div>
</div>
<%@ include file="/jsp/footer.jsp" %>
