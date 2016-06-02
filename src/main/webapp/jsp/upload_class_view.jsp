<% String pageTitle="Upload Class"; %>
<%@ include file="/jsp/header.jsp" %>

<div id="divUpload" class="container">
	<form id="formUpload" action="upload" class="form-upload" method="post" enctype="multipart/form-data">
		<h2>Upload CUT</h2>
		<input id="fileUpload" name="fileUpload" type="file" class="file-loading" data-allowed-file-extensions='["java"]' data-show-preview="false" data-placeholder="No file" data-show-upload="true" data-show-remove="false" data-show-caption="true" data-buttonText="Find CUT">
	</form>
</div>
</body>
</html>
