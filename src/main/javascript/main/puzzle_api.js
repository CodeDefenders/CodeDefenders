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
            throw await response.json();
        }
        return await response.json();
    }

    static async fetchPuzzleData() {
        return await PuzzleAPI.requestJSON(`${contextPath}admin/api/puzzles`, 'GET');
    }

    static async updatePuzzle(puzzleId, puzzleData) {
        return await PuzzleAPI.requestJSON(`${contextPath}admin/api/puzzles/puzzle?id=${puzzleId}`, 'PUT', puzzleData);
    }

    static async updatePuzzleChapter(chapterId, chapterData) {
        return await PuzzleAPI.requestJSON(`${contextPath}admin/api/puzzles/chapter?id=${chapterId}`, 'PUT', chapterData);
    }

    static async createPuzzleChapter(chapterData) {
        return await PuzzleAPI.requestJSON(`${contextPath}admin/api/puzzles/chapter?create`, 'PUT', chapterData);
    }

    static async deletePuzzle(puzzleId) {
        return await PuzzleAPI.requestJSON(`${contextPath}admin/api/puzzles/puzzle?id=${puzzleId}`, 'DELETE');
    }

    static async deletePuzzleChapter(puzzleChapterId) {
        return await PuzzleAPI.requestJSON(`${contextPath}admin/api/puzzles/chapter?id=${puzzleChapterId}`, 'DELETE');
    }

    static async batchUpdatePuzzlePositions(puzzlePositions) {
        return await PuzzleAPI.requestJSON(`${contextPath}admin/api/puzzles`, 'PUT', puzzlePositions);
        // current plan:
        // - send whole configuration on save
        // - create / edit and delete chapters eagerly
    }
}


export default PuzzleAPI;
