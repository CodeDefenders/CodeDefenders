<%@ page import="org.codedefenders.util.Paths" %>
<script>
    class PuzzleAPI {
        static async fetchPuzzleData() {
            return await fetch(`<%=request.getContextPath() + Paths.API_ADMIN_PUZZLES_ALL%>`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            }).then(response => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw Error("Request failed");
                }
            });
        }

        static async updatePuzzle(puzzleId, puzzleData) {
            return await fetch(`<%=request.getContextPath() + Paths.API_ADMIN_PUZZLE%>` + `?id=` + puzzleId, {
                method: 'PUT',
                body: JSON.stringify(puzzleData),
                headers: {
                    'Content-Type': 'application/json'
                }
            }).then(response => {
                let json = response.json();
                if (response.ok) {
                    return json;
                } else {
                    throw Error("Request failed");
                }
            });
        }

        static async updatePuzzleChapter(chapterId, chapterData) {
            return await fetch(`<%=request.getContextPath() + Paths.API_ADMIN_PUZZLECHAPTER%>` + `?id=` + chapterId, {
                method: 'PUT',
                body: JSON.stringify(chapterData),
                headers: {
                    'Content-Type': 'application/json'
                }
            }).then(response => {
                let json = response.json();
                if (response.ok) {
                    return json;
                } else {
                    throw Error("Request failed");
                }
            });
        }

        static async deletePuzzle(puzzleId) {
            return await fetch(`<%=request.getContextPath() + Paths.API_ADMIN_PUZZLE%>` + `?id=` + puzzleId, {
                method: 'DELETE',
                headers: {
                    'Accept': 'application/json'
                }
            }).then(response => {
                let json = response.json();
                if (response.ok) {
                    return json;
                } else {
                    throw Error("Request failed");
                }
            });
        }

        static async deletePuzzleChapter(puzzleChapterId) {
            return await fetch(`<%=request.getContextPath() + Paths.API_ADMIN_PUZZLECHAPTER%>` + `?id=` + puzzleChapterId, {
                method: 'DELETE',
                headers: {
                    'Accept': 'application/json'
                }
            }).then(response => {
                let json = response.json();
                if (response.ok) {
                    return json;
                } else {
                    throw Error("Request failed");
                }
            });
        }
    }
</script>
