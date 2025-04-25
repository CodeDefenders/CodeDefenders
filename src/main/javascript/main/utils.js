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
