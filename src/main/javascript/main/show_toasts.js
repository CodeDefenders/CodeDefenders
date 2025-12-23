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
import {Toast} from 'bootstrap';

class ShowToasts {
    static showToast({
                         colorClass = 'bg-primary',
                         title = '',
                         secondary = '',
                         bodyTitle = '',
                         body = '',
                         link = '',
                         icon = '',
                         extraElements = [],
                         timeout = true,
                         longTimeout = false
                     }) {

        if (extraElements.length > 0 && link !== '') {
            console.log("show_toast.js WARNING: Combining extraElements and links is not allowed.");
            return;
        }

        const toastElem = document.createElement('div');
        toastElem.classList.add('text-decoration-none', 'text-reset', 'toast', 'bg-white', 'd-flex');
        toastElem.role = 'alert';

        if (!timeout) {
            toastElem.setAttribute('data-bs-autohide', 'false');
        } else if (longTimeout) {
            toastElem.setAttribute("data-bs-delay", "8000");
        }

        if (icon !== '') {
            const iconAnchor = document.createElement('a');
            iconAnchor.classList.add('d-flex',
                    'align-items-center',
                    'align-self-stretch',
                    'justify-content-center');
            if (link !== '') {
                iconAnchor.setAttribute('href', link);
            }

            const iconElement = document.createElement("img");
            iconElement.classList.add("me-2", "w-75");
            iconElement.src = icon;

            iconAnchor.appendChild(iconElement);
            toastElem.appendChild(iconAnchor);
        }

        const toastBody = document.createElement('a');
        if (link !== '') {
            toastBody.setAttribute('href', link);
            toastBody.setAttribute('target', '_blank');
            toastBody.setAttribute('rel', 'noopener');
        }
        toastBody.classList.add('toast-body', 'me-auto', 'd-flex', 'flex-column', 'justify-content-between',
                'text-decoration-none', 'text-reset');


        if (bodyTitle !== '') {
            const bodyTitleHeading = document.createElement('h5');
            bodyTitleHeading.innerHTML = '<b>' + bodyTitle + '</b>';
            toastBody.appendChild(bodyTitleHeading);
        }


        const bodySpan = document.createElement("span");
        bodySpan.innerText = body;
        toastBody.appendChild(bodySpan);

        if (extraElements.length > 0) {
            const extraContainer = document.createElement('div');
            extraContainer.classList.add('d-flex', 'flex-row', 'justify-content-between');
            extraElements.forEach(element => {
                if (element instanceof HTMLElement) {
                    extraContainer.appendChild(element);
                } else {
                    console.warn('Extra element is not an HTMLElement:', element);
                }
            });
            toastBody.appendChild(extraContainer);
        }

        const closeButton = document.createElement('button');
        closeButton.type = 'button';
        closeButton.classList.add('btn-close');
        closeButton.setAttribute('data-bs-dismiss', 'toast');
        closeButton.ariaLabel = 'Close';

        let toastColor;
        if (colorClass !== '') {
            toastColor = document.createElement('div');
            toastColor.classList.add('toast-color', 'p-2', 'ms-2', 'me-2', 'rounded-1', colorClass);
        }

        const intermediate = document.createElement('div');
        intermediate.classList.add('flex-fill');
        if (title !== '' || secondary !== '') {
            const toastHeader = document.createElement('div');
            toastHeader.classList.add('toast-header');

            if (toastColor) {
                toastHeader.appendChild(toastColor);
            }

            const toastTitle = document.createElement('strong');
            toastTitle.classList.add('toast-title', 'me-auto');
            toastTitle.innerText = title;
            toastHeader.appendChild(toastTitle);

            const toastSecondary = document.createElement('small');
            toastSecondary.classList.add('toast-secondary', 'text-body-secondary');
            toastSecondary.innerText = secondary;
            toastHeader.appendChild(toastSecondary);

            toastHeader.appendChild(closeButton);
            intermediate.appendChild(toastHeader);
            intermediate.appendChild(toastBody);

        } else {
            intermediate.classList.add("d-flex", "align-items-center", "justify-content-around");
            closeButton.classList.add('me-2');
            if (toastColor) {
                intermediate.appendChild(toastColor);
            }
            intermediate.appendChild(toastBody);
            intermediate.appendChild(closeButton);
        }
        toastElem.appendChild(intermediate);

        document.getElementById('toasts').appendChild(toastElem);
        new Toast(toastElem).show();

        toastElem.addEventListener('hidden.bs.toast', () => {
            setTimeout(() => toastElem.remove(), 1000);
        });
    }
}

export default ShowToasts;
