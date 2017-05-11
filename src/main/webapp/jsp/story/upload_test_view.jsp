<%@ page import="org.codedefenders.singleplayer.PrepareAI" %>
<% String pageTitle=null; %>
<%@ include file="/jsp/header.jsp" %>
<div>
    <div class="w-100 up">
        <h2>Story Mode Uploader - Test Class</h2>
        <p>Secondly, the test class</p>
        <div id="divUpload">
            <form id="formUpload" action="puzzle/uploader" class="form-upload" method="post" enctype="multipart/form-data">
                <span>
                    <li><b>Test files must start with 'Test' and no spaces e.g. TestLift</b></li>
                    <li><b>Test cases should not kill your mutant file</b></li>
                    <input id="testUpload" name="testUpload" type="file" class="file-loading" accept=".java" value="Upload Test" />
                </span>
                <span id="submit-button">
					<input type="submit" text="Upload" class="fileinput-upload-button" onClick="this.form.submit(); this.disabled=true; this.value='Uploading...';" />
				</span>
            </form>
        </div>
    </div>

</div>
<%@ include file="/jsp/footer.jsp" %>
