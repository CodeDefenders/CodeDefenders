class TestAPI {

    /**
     * Requests information about a mutant from the API and sets the value of a given
     * code mirror editor based on the response.
     *
     * @param textarea the textarea of the code mirror instance. Its DOM node name must end with
     * the test's identifier.
     * @param editor the code mirror editor which value is set.
     */
    static getAndSetEditorValue(textarea, editor) {
        if (!textarea || !editor) {
            console.error("Tried to update editor without textarea or editor DOM node elements.");
            return;
        }
        editor.setValue("Loading...");

        let testId = /\d*$/gm.exec(textarea.name);
        TestAPI.getTestInfo(testId)
            .then(test => {
                editor.setValue(test.source);
            })
            .catch(() => {
                editor.setValue("Could not fetch test content.\nPlease reload the page at a later point.");
            });
    }

    /**
     * Calls the Code Defenders test API for a given test.
     *
     * @param testId the identifier of the test.
     * @returns {Promise<Object>} a promise either containing a test object or empty.
     */
    static async getTestInfo(testId) {
        let url = "api/test?testId=" + testId;
        let response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) {
            return Promise.reject();
        }
        let json = await response.json();
        return Promise.resolve(json);
    }
}

