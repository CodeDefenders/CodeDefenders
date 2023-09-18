const parseHTML = function (html) {
    const container = document.createElement('div');
    container.innerHTML = html;
    return container.firstElementChild;
}

export {
    parseHTML
};
