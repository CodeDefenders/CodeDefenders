class PuzzleAPI {
    /**
     * Fetches an object from a given JSON API.
     * @async
     * @param {string} url The URL to fetch from.
     * @param {string} method The method to use for the request.
     * @param {object=} body An optional body to be included in the request.
     * @returns {Promise<object>} A promise containing the response.
     */
    static async requestJSON (url, method, body = null) {
        const request = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };
        if (body !== null) {
            request.body = JSON.stringify(body);
        }
        const response = await fetch(url, request);
        if (!response.ok) {
            return Promise.reject();
        }
        return await response.json();
    }

    static async fetchPuzzleData() {
        return await PuzzleAPI.requestJSON('admin/api/puzzles', 'GET');
    }

    static async updatePuzzle(puzzleId, puzzleData) {
        return await PuzzleAPI.requestJSON(`admin/api/puzzles/puzzle?id=${puzzleId}`, 'PUT', puzzleData);
    }

    static async updatePuzzleChapter(chapterId, chapterData) {
        return await PuzzleAPI.requestJSON(`admin/api/puzzles/chapter?id=${chapterId}`, 'PUT', chapterData);
    }

    static async deletePuzzle(puzzleId) {
        return await PuzzleAPI.requestJSON(`admin/api/puzzles/puzzle?id=${puzzleId}`, 'DELETE');
    }

    static async deletePuzzleChapter(puzzleChapterId) {
        return await PuzzleAPI.requestJSON(`admin/api/puzzles/chapter?id=${puzzleChapterId}`, 'DELETE');
    }
}


export default PuzzleAPI;
