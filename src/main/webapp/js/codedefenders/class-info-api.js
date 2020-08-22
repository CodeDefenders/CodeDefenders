class ClassAPI {

    /**
     * Requests information about a game class from the API and sets the value of a given
     * code mirror editor based on the response.
     *
     * @param textarea the textarea of the code mirror instance. Its DOM node name must end with
     * the class' identifier.
     * @param editor the code mirror editor which value is set.
     */
    static getAndSetEditorValue(textarea, editor) {
        if (!textarea || !editor) {
            console.error("Tried to update editor without textarea or editor DOM node elements.");
            return;
        }
        editor.setValue("Loading...");

        let classId = /\d*$/gm.exec(textarea.name);
        ClassAPI.getClassInfo(classId)
            .then(classInfo => {
                editor.setValue(classInfo.source);
            })
            .catch(() => {
                editor.setValue("Could not fetch class content.\nPlease reload the page at a later point.");
            });
    }

    /**
     * Calls the Code Defenders test API for a given mutant.
     *
     * @param classId the identifier of the class.
     * @returns {Promise<Object>} a promise either containing a test object or empty.
     */
    static async getClassInfo(classId) {
        let url = "api/class?classId=" + classId;
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

