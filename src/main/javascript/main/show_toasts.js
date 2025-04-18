import {Toast} from 'bootstrap';
class ShowToasts {
    static showToast({colorClass = 'bg-primary', title = '', secondary = '', body = ''}) {
        const toastElem = document.createElement('div');
        toastElem.classList.add('toast', 'bg-white');
        toastElem.role = 'alert';
        const toastBody = document.createElement('div');
        toastBody.classList.add('toast-body');

        const closeButton = document.createElement('button');
        closeButton.type = 'button';
        closeButton.classList.add('btn-close');
        closeButton.setAttribute('data-bs-dismiss', 'toast');
        closeButton.ariaLabel = 'Close';

        if (title !== '' || secondary !== '') {
            const toastHeader = document.createElement('div');
            toastHeader.classList.add('toast-header');

            const toastColor = document.createElement('div');
            toastColor.classList.add('toast-color', 'p-2', 'me-2', 'rounded-1', colorClass);
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

        } else {
            toastBody.appendChild(closeButton);
        }
        toastBody.innerText = body;
        toastElem.appendChild(toastBody);

        document.getElementById('toasts').appendChild(toastElem);
        new Toast(toastElem).show();

        toastElem.addEventListener('hidden.bs.toast', () => {
            setTimeout(() => toastElem.remove(), 1000);
        });
    }
}

export default ShowToasts;
