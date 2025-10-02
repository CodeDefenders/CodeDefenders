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
                         body = '',
                         link = '',
                         icon = '',
                         extraElements = [],
                         timeout = true,
                         longTimeout = false
                     }) {

        console.log("Showing a toast");
        const toastElem = document.createElement('div');
        toastElem.classList.add('toast', 'bg-white');
        toastElem.role = 'alert';

        let linkElement;
        if (link !== '') {
            linkElement = document.createElement('a');
            linkElement.classList.add('text-decoration-none', 'text-reset', 'd-block');
            linkElement.setAttribute('href', link);
            linkElement.appendChild(toastElem)
        }


        if (!timeout) {
            toastElem.setAttribute('data-bs-autohide', 'false');
        } else if (longTimeout) {
            toastElem.setAttribute("data-bs-delay", "8000");
        }

        const toastBody = document.createElement('div');
        toastBody.classList.add('toast-body', 'me-auto', 'd-flex', 'flex-row', 'justify-content-between');

        if (icon !== '') {
            const iconElement = document.createElement("img");
            iconElement.classList.add("me-2", "w-25");
            iconElement.src = icon;
            toastBody.appendChild(iconElement);
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

        const toastColor = document.createElement('div');
        toastColor.classList.add('toast-color', 'p-2', 'ms-2', 'me-2', 'rounded-1', colorClass);

        if (title !== '' || secondary !== '') {
            const toastHeader = document.createElement('div');
            toastHeader.classList.add('toast-header');

            toastHeader.appendChild(toastColor);

            const toastTitle = document.createElement('strong');
            toastTitle.classList.add('toast-title', 'me-auto');
            toastTitle.innerText = title;
            toastHeader.appendChild(toastTitle);

            const toastSecondary = document.createElement('small');
            toastSecondary.classList.add('toast-secondary', 'text-body-secondary');
            toastSecondary.innerText = secondary;
            toastHeader.appendChild(toastSecondary);

            toastHeader.appendChild(closeButton);
            toastElem.appendChild(toastHeader);
            toastElem.appendChild(toastBody);

        } else {
            const intermediate = document.createElement('div');
            intermediate.classList.add("d-flex", "align-items-center", "justify-content-around");
            closeButton.classList.add('me-2');
            intermediate.appendChild(toastColor);
            intermediate.appendChild(toastBody);
            intermediate.appendChild(closeButton);
            toastElem.appendChild(intermediate);
        }

        const topLevelToastElem = link === '' ? toastElem : linkElement;
        document.getElementById('toasts').appendChild(topLevelToastElem);
        new Toast(toastElem).show();

        toastElem.addEventListener('hidden.bs.toast', () => {
            setTimeout(() => topLevelToastElem.remove(), 1000);
        });
    }
}

export default ShowToasts;
