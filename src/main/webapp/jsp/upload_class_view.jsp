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
<%@ page import="org.codedefenders.database.FeedbackDAO" %>
<%@ page import="org.codedefenders.database.GameClassDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.*" %>
<%@ page import="java.util.Map" %>

<jsp:include page="/jsp/header_main.jsp"/>

<link href="${pageContext.request.contextPath}/css/uploadcut.css" rel="stylesheet">

<div>
    <div class="container">
		<h2>Upload Class</h2>
		<div id="divUpload">
			<form id="formUpload" action="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>" class="form-upload" method="post" enctype="multipart/form-data"
                  style="padding: 15px; margin: 0 auto; max-width: 60rem;">
				<span class="label label-danger" id="invalid_alias" style="color: white;visibility: hidden">Name with no whitespaces or special characters.</span>
				<input id="classAlias" onkeyup="validateAlias()" name="classAlias" type="text" class="form-control" placeholder="Optional class alias, otherwise class name is used" >
				<!--
				<input type="checkbox" name="prepareForSingle" value="prepare" style="margin-right:5px;">Generate mutants and tests for single-player mode? (It may take a while...)</input>
				-->
				<div>
                    <h3>Upload Class under Test</h3>
                    <span class="file-select">
                        <input id="fileUploadCUT" name="fileUploadCUT" type="file" class="file-loading" accept=".java" required />
                    </span>
					<br>
					<span>The class used for games. Mutants and tests are created for this class.</span>
				</div>
                <br>

				<div class="form-group">
					<label for="testingFramework" title="The testing framework used to write tests for this class.">
						Testing Framework
					</label>
					<select id="testingFramework" name="testingFramework" class="form-control" data-size="small" required>
						<%for (TestingFramework tf : TestingFramework.values()) {%>
						<option value="<%=tf.name()%>" <%=tf == TestingFramework.JUNIT4 ? "selected" : ""%>>
							<%=tf.getDescription()%>
						</option>
						<%}%>
					</select>
				</div>
				<div class="form-group">
					<label for="assertionLibrary" title="The assertion library used to write tests for this class.">
						Assertion Library
					</label>
					<select id="assertionLibrary" name="assertionLibrary" class="form-control" data-size="small" required>
						<%for (AssertionLibrary al : AssertionLibrary.values()) {%>
						<option value="<%=al.name()%>" <%=al == AssertionLibrary.JUNIT4_HAMCREST ? "selected" : ""%>>
							<%=al.getDescription()%>
						</option>
						<%}%>
					</select>
				</div>

				<button class="btn btn-light" type="button" data-toggle="collapse" data-target="#collapseAdvanced" aria-expanded="false" aria-controls="collapseExample">
					Advanced upload options...
				</button>


            <span class="border border-primary">
                    <div class="collapse" id="collapseAdvanced">
                        <div>
                            <h3>Upload Dependencies (optional)</h3>
                            <span class="file-select">
                                <input id="fileUploadDependency" name="fileUploadDependency" type="file" class="file-loading" accept=".zip" />
                            </span>
                            <br>
                            <span>
                                If the class under test has dependencies, you can upload them here.
                            </span>
                            <ul style="margin-top: .5em;">
                                <li>The dependencies are uploaded inside of a <code>zip</code> file, the folder structure of which is irrelevant.</li>
                                <li>Dependencies may contain packages.</li>
                            </ul>
                            <span>Example:</span>
<pre>deps
&#x251c;&#x2500; Event.java
&#x2514;&#x2500; events
    &#x251c;&#x2500; StartEvent.java
    &#x2514;&#x2500; StopEvent.java</pre>
                        </div>
                        <div>
                            <h3>Upload Mutants (optional)</h3>
                            <span class="file-select">
                                <input id="fileUploadMutant" name="fileUploadMutant" type="file" class="file-loading" accept=".zip" />
                            </span>
                            <br>
                            <span>
                                Mutants uploaded with a class under test can be used to initialize games with existing mutants.
                            </span>
                            <ul style="margin-top: .5em;">
                                <li>The dependencies are uploaded inside of a <code>zip</code> file, the folder structure of which is irrelevant.</li>
                                <li>All mutants must have the same class name as the class under test.</li>
                            </ul>
                            <span>Example:</span>
<pre>mutants
&#x251c;&#x2500; 01
&#x2502;   &#x2514;&#x2500; EventBus.java
&#x2514;&#x2500; 02
    &#x2514;&#x2500; EventBus.java</pre>
                        </div>
                        <div>
                            <h3>Upload Tests (optional)</h3>
                            <span class="file-select">
                                <input id="fileUploadTest" name="fileUploadTest" type="file" class="file-loading" accept=".zip" />
                            </span>
                            <br>
                            <span>
                                Tests uploaded with a class under test can be used to initialize games with existing tests.
                            </span>
                            <ul style="margin-top: .5em;">
                                <li>The dependencies are uploaded inside of a <code>zip</code> file, the folder structure of which is irrelevant.</li>
                                <li>Multiple tests can have the same name, but they don't have to.</li>
                            </ul>
                            <span>Example:</span>
<pre>tests
&#x251c;&#x2500; 01
&#x2502;   &#x2514;&#x2500; TestEventBus.java
&#x2514;&#x2500; 02
    &#x2514;&#x2500; TestEventBus.java</pre>
                        </div>
                        <br>
                        <input id="mockingEnabled" type="checkbox" name="enableMocking" value="isMocking" style="margin-right:5px;">Enable Mocking for this class</input>
                        <br>
                    </div>
                </span>
				<span class="submit-button">
					<button id="upload" type="submit" class="fileinput-upload-button btn btn-light">
						Upload
					</button>
				</span>

                <input type="hidden" name="origin" value="<%=request.getParameter("origin")%>" />
				<input type="checkbox" name="disableAutomaticRedirect" value="disabled" style="margin-right:5px;">I want to upload another class</input>
				
				<script>
                    function validateAlias() {
                        let classAlias = document.forms["formUpload"]["classAlias"].value;

                        let regExp = new RegExp('^[a-zA-Z0-9]*$');
                        if (regExp.test(classAlias)) {
                            document.getElementById('invalid_alias').style.visibility = "hidden";
                            document.getElementById('upload').disabled = false;
                        } else {
                            document.getElementById('invalid_alias').style.visibility = "visible";
                            document.getElementById('upload').disabled = true;
                        }
                    }
				</script>
			</form>
		</div>
	</div>
	<div class="container">
		<h2>Uploaded Classes</h2>
		<!-- Deactivated because single player mode is not activated currently
		<span>Preparing classes for the single player mode (action 'Prepare AI') may take a long time.</span>
		-->
		<div id="classList" >
			<%
				List<GameClass> gameClasses = GameClassDAO.getAllPlayableClasses();
				Map<Integer, Double> avgMutationDifficulties = FeedbackDAO.getAverageMutationDifficulties();
				Map<Integer, Double> avgTestDifficulties = FeedbackDAO.getAverageTestDifficulties();
			%>
			<table class="table table-striped table-hover table-responsive table-center" id = "tableUploadedClasses">
				<thead>
				<tr>
					<th class="col-sm-1 col-sm-offset-2">ID</th>
					<th>Class name (alias)</th>
					<th>Available dependencies/tests/mutants</th>
					<th>Mutation Difficulty</th>
					<th>Testing Difficulty</th>
					<th>Testing Framework</th>
					<th>Assertion Library</th>
				</tr>
				</thead>
				<tbody>
				<% if (gameClasses.isEmpty()) { %>
					<tr><td colspan="100%">No classes uploaded.</td></tr>
				<% } else {
					for (GameClass c : gameClasses) {
						Double mutationDiff = avgMutationDifficulties.get(c.getId());
						Double testingDiff = avgTestDifficulties.get(c.getId());
					%>
						<tr>
							<td><%= c.getId() %></td>
							<td>
								<a href="#" data-toggle="modal" data-target="#modalCUTFor<%=c.getId()%>">
									<%=c.getBaseName()%> <%=c.getBaseName().equals(c.getAlias()) ? "" : "(alias "+c.getAlias()+")"%>
								</a>
								<div id="modalCUTFor<%=c.getId()%>" class="modal fade" role="dialog" style="text-align: left;" tabindex="-1">
									<div class="modal-dialog">
										<!-- Modal content-->
										<div class="modal-content">
											<div class="modal-header">
												<button type="button" class="close" data-dismiss="modal">&times;</button>
												<h4 class="modal-title"><%=c.getBaseName()%></h4>
											</div>
											<div class="modal-body">
												<pre class="readonly-pre"><textarea
														class="readonly-textarea classPreview" id="sut<%=c.getId()%>"
														name="cut<%=c.getId()%>" cols="80"
														rows="30"></textarea></pre>
											</div>
											<div class="modal-footer">
												<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
											</div>
										</div>
									</div>
								</div>
							</td>
							<td><%=GameClassDAO.getMappedDependencyIdsForClassId(c.getId()).size()%>/<%=GameClassDAO.getMappedTestIdsForClassId(c.getId()).size()%>/<%=GameClassDAO.getMappedMutantIdsForClassId(c.getId()).size()%></td>
							<td><%=mutationDiff != null ? String.valueOf(mutationDiff) : ""%></td>
							<td><%=testingDiff != null ? String.valueOf(testingDiff) : ""%></td>
							<%--
							<td>
								<form id="aiPrepButton<%= c.getId() %>" action="<%=request.getContextPath() + Paths.AI_PREPARER%>" method="post" >
									<button type="submit" class="btn btn-primary btn-game pull-right" form="aiPrepButton<%= c.getId() %>" onClick="this.form.submit(); this.disabled=true; this.value='Preparing...';"
											<% //if (PrepareAI.isPrepared(c)) { %> disabled <% //} %>>
										Prepare AI
									</button>
									<input type="hidden" name="formType" value="runPrepareAi" />
									<input type="hidden" name="cutID" value="<%= c.getId() %>" />
								</form>
							</td>
							--%>
							<td><%=c.getTestingFramework().getDescription()%></td>
							<td><%=c.getAssertionLibrary().getDescription()%></td>
						</tr>
					<% } %>
				</tbody>
				<% } %>
			</table>
		</div>
	</div>
	<script>
        $('.modal').on('shown.bs.modal', function() {
			let codeMirrorContainer = $(this).find(".CodeMirror")[0];
			if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
				codeMirrorContainer.CodeMirror.refresh();
			} else {
                let textarea = $(this).find('textarea')[0];
                let editor = CodeMirror.fromTextArea(textarea, {
					lineNumbers: false,
					readOnly: true,
					mode: "text/x-java"
				});
                editor.setSize("100%", 500);
                ClassAPI.getAndSetEditorValue(textarea, editor);
            }
		});

        $(document).ready(function () {
            $('#tableUploadedClasses').DataTable({
                'paging': false,
                lengthChange: false,
                searching: false,
                order: [[0, "desc"]]
            });
        });
	</script>
</div>
<%@ include file="/jsp/footer.jsp" %>
