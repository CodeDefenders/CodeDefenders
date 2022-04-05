/* Wrap in a function to avoid polluting the global scope. */
(function () {

class InfoApi {

    /**
     * Sets the given editor to the given value.
     * If the value is a failing promise, a placeholder text is displayed instead.
     * @param {CodeMirror} editor The CodeMirror editor which value is set.
     * @param {string|Promise<string>} value The value to set, can be a promise.
     */
    static setEditorValue(editor, value) {
        editor.setValue("Loading...");

        Promise.resolve(value)
                .then(response => {
                    editor.setValue(response);
                })
                .catch(() => {
                    editor.setValue("Could not fetch content.\nPlease reload the page at a later point.");
                });
    }

    /**
     * Fetches an object from a given JSON API.
     * @async
     * @param {string} url The URL to fetch from.
     * @returns {Promise<object>} A promise containing the response.
     */
    static async fetchJSON (url) {
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) {
            return Promise.reject();
        }
        return await response.json();
    }

    static async getClassInfo (classId) {
        return await InfoApi.fetchJSON(`api/class?classId=${classId}`);
    }

    static async getMutantInfo (mutantId) {
        return await InfoApi.fetchJSON(`api/mutant?mutantId=${mutantId}`);
    }

    static async getTestInfo (testId) {
        return await InfoApi.fetchJSON(`api/test?testId=${testId}`);
    }

    static setClassEditorValue (editor, classId) {
        InfoApi.setEditorValue(editor,
                InfoApi.getClassInfo(classId).then(classInfo => classInfo.source));
    }

    static setMutantEditorValue (editor, mutantId) {
        InfoApi.setEditorValue(editor,
                InfoApi.getMutantInfo(mutantId).then(mutantInfo => mutantInfo.diff));
    }

    static setTestEditorValue (editor, testId) {
        InfoApi.setEditorValue(editor,
                InfoApi.getTestInfo(testId).then(testInfo => testInfo.source));
    }
}

CodeDefenders.classes.InfoApi ??= InfoApi;

})();
