<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ attribute name="id" required="false" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="content" fragment="true" required="true" %>
<%@ attribute name="footer" fragment="true" %>

<%@ attribute name="modalDialogClasses" required="false" description="Additional class(es) for the .modal-dialog div." %>
<%@ attribute name="modalHeaderClasses" required="false" description="Additional class(es) for the .modal-header div." %>
<%@ attribute name="modalBodyClasses" required="false" description="Additional class(es) for the .modal-body div." %>
<%@ attribute name="modalFooterClasses" required="false" description="Additional class(es) for the .modal-footer div." %>

<%@ attribute name="closeButtonText" required="false" %>
<c:set var="closeButtonText" value="${(empty closeButtonText) ? 'Close' : closeButtonText}" />

<div class="modal fade" tabindex="-1" aria-hidden="true" role="dialog" <c:if test="${not empty id}">id="${id}"</c:if>>
    <div class="modal-dialog ${modalDialogClasses}">
        <div class="modal-content">
            <div class="modal-header ${modalHeaderClasses}">
                <h5 class="modal-title">${title}</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body ${modalBodyClasses}">
                <jsp:invoke fragment="content"/>
            </div>
            <div class="modal-footer ${modalFooterClasses}">
                <jsp:invoke fragment="footer"/>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">${closeButtonText}</button>
            </div>
        </div>
    </div>
</div>
