class MutantAPI {

    /**
     * Requests information about a mutant from the API and sets the value of a given
     * code mirror editor based on the response.
     *
     * @param textarea the textarea of the code mirror instance. Its DOM node name must end with
     * the mutant's identifier.
     * @param editor the code mirror editor which value is set.
     */
    static getAndSetEditorValueWithDiff(textarea, editor) {
        if (!textarea || !editor) {
            console.error("Tried to update editor without textarea or editor DOM node elements.");
            return;
        }
        editor.setValue("Loading...");

        let mutantId = /\d*$/gm.exec(textarea.name);
        MutantAPI.getMutantInfo(mutantId)
            .then(mutant => {
                editor.setValue(mutant.diff);
            })
            .catch(() => {
                editor.setValue("Could not fetch mutant content.\nPlease reload the page at a later point.");
            });
    }

    /**
     * Calls the Code Defenders test API for a given mutant.
     *
     * @param mutantId the identifier of the mutant.
     * @returns {Promise<Object>} a promise either containing a test object or empty.
     */
    static async getMutantInfo(mutantId) {
        let url = "api/mutant?mutantId=" + mutantId;
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

