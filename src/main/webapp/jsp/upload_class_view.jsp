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

<jsp:include page="/jsp/header_main.jsp"/>

<div>
    <div class="container">
		<h2>Upload Class</h2>
		<div id="divUpload">
			<form id="formUpload" action="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>" class="form-upload" method="post" enctype="multipart/form-data">
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
					<span>The class used for games. Mutants are created from and tests are created for this class.</span>
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
                                If the class under test has dependencies, upload them as inside a <code>zip</code> file.
                            </span>
                        </div>
                        <div>
                            <h3>Upload Mutants (optional)</h3>
                            <span class="file-select">
                                <input id="fileUploadMutant" name="fileUploadMutant" type="file" class="file-loading" accept=".zip" />
                            </span>
                            <br>
                            <span>
                                Mutants uploaded with a class under test can be used to initialize games with existing mutants.
                                Note that all mutants must have the same class name as the class under test and must be uploaded inside a <code>zip</code> file.
                            </span>
                        </div>
                        <div>
                            <h3>Upload Tests (optional)</h3>
                            <span class="file-select">
                                <input id="fileUploadTest" name="fileUploadTest" type="file" class="file-loading" accept=".zip" />
                            </span>
                            <br>
                            <span>
                                Tests uploaded with a class under test can be used to initialize games with existing tests.
                                Note that all tests must be uploaded inside a <code>zip</code> file.
                            </span>
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

				<input type="hidden" value="<%=request.getParameter("fromAdmin")%>" name="fromAdmin">
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
				List<Double> avgMutationDifficulties = FeedbackDAO.getAverageMutationDifficulties();
				List<Double> avgTestDifficulties = FeedbackDAO.getAverageTestDifficulties();
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
					for (int i = 0; i < gameClasses.size(); i++) {
						GameClass c = gameClasses.get(i);
						double mutationDiff = avgMutationDifficulties.get(i);
						double testingDiff = avgTestDifficulties.get(i);
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
							<td><%=mutationDiff > 0 ? String.valueOf(mutationDiff) : ""%></td>
							<td><%=testingDiff > 0 ? String.valueOf(testingDiff) : ""%></td>
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
