<% String pageTitle="Upload Class"; %>
<%@ include file="/jsp/header.jsp" %>

<div id="divUpload" class="container">
	<form id="formUpload" action="upload" class="form-upload" method="post" enctype="multipart/form-data">
		<input id="fileUpload" name="fileUpload" type="file" class="file-loading" data-allowed-file-extensions='["java"]' data-show-preview="false" data-placeholder="No file" data-show-upload="true" data-show-remove="false" data-show-caption="true" data-buttonText="Find CUT">
		<input type="submit" text="Upoad" />
	</form>
	</div>
<%@ include file="/jsp/footer.jsp" %>
