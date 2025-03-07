class InfoApi {

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

    static async getClassInfo (classId, withDependencies) {
        return await InfoApi.fetchJSON(`${contextPath}api/class?classId=${classId}&withDependencies=${withDependencies ? 1 : 0}`);
    }

    static async getMutantInfo (mutantId) {
        return await InfoApi.fetchJSON(`${contextPath}api/mutant?mutantId=${mutantId}`);
    }

    static async getTestInfo (testId) {
        return await InfoApi.fetchJSON(`${contextPath}api/test?testId=${testId}`);
    }

    static async setClassEditorValue (editor, classId) {
        try {
            const classInfo = await InfoApi.getClassInfo(classId, false);
            editor.setValue(classInfo.source)
        } catch (e) {
            editor.setValue("Could not fetch class.\nPlease try again later.");
        }
    }

    static async setDependencyEditorValue (editor, classId, dependencyIndex) {
        try {
            const classInfo = await InfoApi.getClassInfo(classId, true);
            editor.setValue(classInfo.dependency_code[dependencyIndex])
        } catch (e) {
            editor.setValue("Could not fetch dependency \n Please try again later.");
        }
    }

    static async setMutantEditorValue (editor, mutantId) {
        try {
            const mutantInfo = await InfoApi.getMutantInfo(mutantId);
            editor.setValue(mutantInfo.diff)
        } catch (e) {
            editor.setValue("Could not fetch mutant.\nPlease try again later.");
        }
    }

    static async setTestEditorValue (editor, testId) {
        try {
            const testInfo = await InfoApi.getTestInfo(testId);
            editor.setValue(testInfo.source)
        } catch (e) {
            editor.setValue("Could not fetch test.\nPlease try again later.");
        }
    }
}


export default InfoApi;
