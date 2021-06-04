<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="title" required="true" %>
<%@ attribute name="content" fragment="true" required="true" %>
<%@ attribute name="footer" fragment="true" %>
<%@ attribute name="id" required="false" %>

<%@ attribute name="closeButtonText" required="false" %>
<c:set var="closeButtonText" value="${(empty closeButtonText) ? 'Close' : closeButtonText}" />

<div class="modal fade" tabindex="-1" aria-hidden="true" <c:if test="${not empty id}">id="${id}"></c:if>>
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">${title}</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <jsp:invoke fragment="content"/>
            </div>
            <div class="modal-footer">
                <jsp:invoke fragment="footer"/>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">${closeButtonText}</button>
            </div>
        </div>
    </div>
</div>
