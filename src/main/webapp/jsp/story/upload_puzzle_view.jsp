<%@ page import="org.codedefenders.singleplayer.PrepareAI" %>
<% String pageTitle=null; %>
<%@ include file="/jsp/header.jsp" %>
<div>
    <div class="w-100 up">
        <h2>Story Mode Uploader - Puzzle Class</h2>
        <p>To add a puzzle, you must upload:
        <li>A puzzle class</li>
        <li>A test class</li>
        <li>a mutant class.</li>
        </p>
        <p>Firstly, we are going to upload the puzzle class</p>
        <div id="divUpload">
            <form id="formUpload" action="puzzle/uploader" class="form-upload" method="post" enctype="multipart/form-data">
                <span>
                    Upload your class here:
					<input id="fileUpload" name="fileUpload" type="file" class="file-loading" accept=".java" value="Upload Class"/>
				</span>
                <span id="submit-button">
					<input type="submit" text="Upload" class="fileinput-upload-button" onClick="this.form.submit(); this.disabled=true; this.value='Uploading...';" />
				</span>
            </form>
        </div>
    </div>

</div>
<%@ include file="/jsp/footer.jsp" %>
