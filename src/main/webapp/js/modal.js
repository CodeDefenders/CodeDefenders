/* Wrap in a function to avoid polluting the global scope. */
(function () {

class Modal {

    /**
     * Creates a new modal element and exposes its internal elements.
     * @param {object} options Options to be passed to the Bootstrap Modal constructor.
     */
    constructor (options) {
        const modal = document.createElement('div');
        modal.classList.add('modal', 'fade');
        modal.setAttribute('tabindex', '-1');
        modal.setAttribute('aria-hidden', 'true');

        const modalDialog = document.createElement('div');
        modalDialog.classList.add('modal-dialog');
        modal.appendChild(modalDialog);

        const modalContent = document.createElement('div');
        modalContent.classList.add('modal-content');
        modalDialog.appendChild(modalContent);

        const modalHeader = document.createElement('div');
        modalHeader.classList.add('modal-header');
        modalContent.appendChild(modalHeader);

        const modalTitle = document.createElement('h5');
        modalTitle.classList.add('modal-title');
        modalHeader.appendChild(modalTitle);

        const headerCloseButton = document.createElement('button');
        headerCloseButton.classList.add('btn-close');
        headerCloseButton.setAttribute('type', 'button');
        headerCloseButton.setAttribute('aria-label', 'Close');
        headerCloseButton.dataset.bsDismiss = 'modal';
        modalHeader.appendChild(headerCloseButton);

        const modalBody = document.createElement('div');
        modalBody.classList.add('modal-body');
        modalContent.appendChild(modalBody);

        const modalFooter = document.createElement('div');
        modalFooter.classList.add('modal-footer');
        modalContent.appendChild(modalFooter);

        const footerCloseButton = document.createElement('button');
        footerCloseButton.classList.add('btn', 'btn-secondary');
        headerCloseButton.setAttribute('type', 'button');
        footerCloseButton.dataset.bsDismiss = 'modal';
        footerCloseButton.innerText = 'Close';
        modalFooter.appendChild(footerCloseButton);

        /** @type HTMLDivElement */
        this.modal = modal;
        /** @type HTMLDivElement */
        this.dialog = modalDialog;
        /** @type HTMLDivElement */
        this.content = modalContent;
        /** @type HTMLDivElement */
        this.header = modalHeader;
        /** @type HTMLHeadingElement */
        this.title = modalTitle;
        /** @type HTMLButtonElement */
        this.headerCloseButton = headerCloseButton;
        /** @type HTMLDivElement */
        this.body = modalBody;
        /** @type HTMLDivElement */
        this.footer = modalFooter;
        /** @type HTMLButtonElement */
        this.footerCloseButton = footerCloseButton;

        /** @type bootstrap.Modal */
        this.controls = new bootstrap.Modal(modal, options);

        document.body.appendChild(modal);
    }
}

CodeDefenders.classes.Modal ??= Modal;

})();
