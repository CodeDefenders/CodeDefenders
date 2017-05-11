<%@ page import="org.codedefenders.singleplayer.PrepareAI" %>
<% String pageTitle=null; %>
<%@ include file="/jsp/header.jsp" %>
<div>
    <div class="w-100 up">
        <h2>Story Mode Uploader - Mutant Class</h2>
        <p>Finally, the mutant class</p>
        <div id="divUpload">
            <form id="formUpload" action="puzzle/uploader" class="form-upload" method="post" enctype="multipart/form-data">
                <span>
                    <li><b>Make sure the Mutant file is the same name as the Puzzle file!</b></li>
                    <input id="mutantUpload" name="mutantUpload" type="file" class="file-loading" accept=".java" value="Upload Mutant"/>
                </span>
                <span id="submit-button">
					<input type="submit" text="Upload" class="fileinput-upload-button" onClick="this.form.submit(); this.disabled=true; this.value='Uploading...';" />
				</span>
            </form>
        </div>
    </div>
</div>
<%@ include file="/jsp/footer.jsp" %>
