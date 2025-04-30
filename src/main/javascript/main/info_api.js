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

    static async getInviteLinkDataWithoutGameId() {
        return await InfoApi.fetchJSON(`${contextPath}api/invite-link`);
    }
}


export default InfoApi;
