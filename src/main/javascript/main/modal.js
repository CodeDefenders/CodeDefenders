/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
import {Modal as BootstrapModal} from '../thirdparty/bootstrap';


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

        /** @type BootstrapModal */
        this.controls = new BootstrapModal(modal, options);

        document.body.appendChild(modal);
    }
}


export default Modal;
