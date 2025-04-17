import {Toast} from 'bootstrap';
class ShowToasts {
    static showToast({colorClass = 'bg-primary', title = '', secondary = '', body = ''}) {
        const toastElem = document.createElement('div');
        toastElem.classList.add('toast', 'bg-white');
        toastElem.role = 'alert';
        toastElem.innerHTML = `
                    <div class="toast-header">
                        <div class="toast-color p-2 me-2 rounded-1"></div>
                        <strong class="toast-title me-auto"></strong>
                        <small class="toast-secondary text-body-secondary"></small>
                        <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                    </div>
                    <div class="toast-body"></div>`;

        toastElem.querySelector('.toast-color').classList.add(colorClass);
        toastElem.querySelector('.toast-title').innerText = title;
        toastElem.querySelector('.toast-secondary').innerText = secondary;
        toastElem.querySelector('.toast-body').innerText = body;

        document.getElementById('toasts').appendChild(toastElem);
        new Toast(toastElem).show();

        toastElem.addEventListener('hidden.bs.toast', () => {
            setTimeout(() => toastElem.remove(), 1000);
        });
    }
}

export default ShowToasts;
