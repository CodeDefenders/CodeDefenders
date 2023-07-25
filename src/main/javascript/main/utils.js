/**
 * Parses an HTML string into an HTML element.
 * @param {string} html The string to be parsed.
 * @returns {Element} The parsed HTML element.
 */
const parseHTML = function (html) {
    const container = document.createElement('div');
    container.innerHTML = html;
    return container.firstElementChild;
};

/**
 * Creates and posts a form with the given data.
 * @param {Object.<string, string>} formData Object with [name: value] pairs of form data.
 * @param {string?} action The URL to post to.
 */
const postForm = function (action, formData) {
    const form = document.createElement('form');
    form.method = 'POST';
    if (action !== null) {
        form.action = action;
    }

    for (let [name, value] of Object.entries(formData)) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
    }

    document.body.appendChild(form);
    form.submit();
};

class DeferredPromise {
    constructor() {
        this.promise = new Promise((resolve, reject) => {
            this.reject = reject;
            this.resolve = resolve;
        });
    }
}


export {
    DeferredPromise,
    parseHTML,
    postForm
};
