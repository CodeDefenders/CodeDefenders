<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<p:main_page title="Puzzle Management">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/puzzle_management.css")}" rel="stylesheet">
    </jsp:attribute>
    <jsp:body>
        <div class="container">
            <t:admin_navigation activePage="adminPuzzles"/>
            <div id="puzzle-management" class="d-flex flex-column gap-5">
                <div class="chapters d-flex flex-column gap-4"></div>
            </div>
        </div>

        <script type="module">
            import {Sortable} from '${url.forPath('/js/sortablejs.mjs')}'
            import {PuzzleAPI, DeferredPromise} from '${url.forPath("/js/codedefenders_main.mjs")}';


            const watermarkUrl = '${url.forPath("/images/achievements/")}';

            const puzzleData = await PuzzleAPI.fetchPuzzleData();
            const puzzles = puzzleData.puzzles;
            const chapters = puzzleData.puzzleChapters;

            const puzzlesPerChapter = new Map();
            puzzlesPerChapter.set('unassigned', []);
            puzzlesPerChapter.set('archived', []);
            for (const puzzle of puzzles) {
                if (!puzzle.active) {
                    puzzlesPerChapter.get('archived').push(puzzle);
                } else if (puzzle.chapterId === null) {
                    puzzlesPerChapter.get('unassigned').push(puzzle);
                } else {
                    if (!puzzlesPerChapter.has(puzzle.chapterId)) {
                        puzzlesPerChapter.set(puzzle.chapterId, [puzzle])
                    } else {
                        puzzlesPerChapter.get(puzzle.chapterId).push(puzzle);
                    }
                }
            }

            chapters.sort((a, b) => a.position - b.position);
            for (const puzzles of puzzlesPerChapter.values()) {
                puzzles.sort((a, b) => a.position - b.position);
            }

            function createChapterElement(chapter, type = 'regular') {
                let chapterControls =
                           `<div class="chapter__controls d-flex gap-1 align-items-center">
                                <div class="chapter__handle me-3"></div>
                                <button class="btn btn-xs btn-primary"
                                        data-bs-toggle="tooltip"
                                        title="Edit">
                                    <i class="fa fa-edit"></i>
                                </button>
                                <button class="btn btn-xs btn-danger"
                                        data-bs-toggle="tooltip"
                                        title="Delete">
                                    <i class="fa fa-trash"></i>
                                </button>
                            </div>`;

                const chapterDiv = document.createElement('div');
                chapterDiv.classList.add('chapter', `chapter-\${type}`);
                chapterDiv.innerHTML =
                        `<div class="chapter__header d-flex align-items-center justify-content-between mb-3">
                            <div class="chapter__info d-flex gap-3 flex-shrink-1 align-items-baseline">
                                <span class="chapter__title badge fs-6 bg-secondary"></span>
                                <div class="chapter__description"></div>
                            </div>
                            \${type === 'regular' ? chapterControls : ''}
                        </div>
                        <div class="puzzles"></div>`;

                switch (type) {
                    case 'regular':
                        chapterDiv.querySelector('.chapter__title').innerText = chapter.title;
                        chapterDiv.querySelector('.chapter__description').innerText = chapter.description;
                        break;
                    case 'unassigned':
                        chapterDiv.querySelector('.chapter__title').innerText = 'Unassigned Puzzles';
                        break;
                    case 'archived':
                        chapterDiv.querySelector('.chapter__title').innerText = 'Archived Puzzles';
                        break;

                }

                if (chapter !== null) {
                    chapterDiv.dataset.id = chapter.id;
                }
                return chapterDiv;
            }

            function createUnassignedChapter() {
                return createChapterElement(null, 'unassigned');
            }

            function createArchivedChapter() {
                return createChapterElement(null, 'archived');
            }

            function createPuzzleElement(puzzle) {
                const watermark = document.createElement('img');
                watermark.classList.add('puzzle__watermark');
                watermark.src = `\${watermarkUrl}codedefenders_achievements_\${puzzle.activeRole == 'ATTACKER' ? 1 : 2}_lvl_0.png`;

                const puzzleDiv = document.createElement('div');
                puzzleDiv.classList.add('puzzle', `puzzle-\${puzzle.activeRole.toLowerCase()}`)
                puzzleDiv.innerHTML =
                        `<div class="puzzle__container">
                            <div class="puzzle__info">
                                <div class="puzzle__title"></div>
                                <div class="puzzle__description"></div>
                            </div>
                            <div class="puzzle__tags">
                                <span class="badge puzzle__tag puzzle__tag__id"></span>
                                <span class="badge puzzle__tag puzzle__tag__games"></span>
                            </div>
                        </div>
                        <div class="puzzle__controls">
                            <button class="btn btn-xs btn-primary puzzle__button__edit">
                                <i class="fa fa-edit"></i>
                            </button>
                            <button class="btn btn-xs btn-danger puzzle__button__archive">
                                <i class="fa fa-archive"></i>
                            </button>
                            <button class="btn btn-xs btn-danger puzzle__button__delete">
                                <i class="fa fa-trash"></i>
                            </button>
                        </div>`;

                puzzleDiv.appendChild(watermark);
                puzzleDiv.querySelector('.puzzle__title').innerText = puzzle.title;
                puzzleDiv.querySelector('.puzzle__title').setAttribute('title', puzzle.title);
                puzzleDiv.querySelector('.puzzle__description').innerText = puzzle.description;
                puzzleDiv.querySelector('.puzzle__tag__id').innerText = '#' + puzzle.id;
                puzzleDiv.querySelector('.puzzle__tag__games').innerText =
                        puzzle.gameCount + ' game' + (puzzle.gameCount === 1 ? '' : 's');
                if (puzzle.gameCount > 0) {
                    puzzleDiv.querySelector('.puzzle__button__delete').disabled = true;
                }

                puzzleDiv.dataset.id = puzzle.id;
                return puzzleDiv;
            }

            function addPuzzleToChapter(puzzleElement, chapterElement) {
                chapterElement.querySelector('.puzzles').appendChild(puzzleElement);
            }

            function archivePuzzle(puzzleElement) {
                document.querySelector('.chapter.chapter-archived .puzzles').appendChild(puzzleElement);
            }

            function unassignPuzzle(puzzleElement) {
                document.querySelector('.chapter.chapter-unassigned .puzzles').appendChild(puzzleElement);
            }


            const container = document.querySelector('#puzzle-management .chapters');

            const unassignedChapter = createUnassignedChapter();
            container.parentElement.insertBefore(unassignedChapter, container);
            for (const puzzle of puzzlesPerChapter.get('unassigned')) {
                addPuzzleToChapter(createPuzzleElement(puzzle), unassignedChapter);
            }

            for (const chapter of chapters) {
                const chapterElement = createChapterElement(chapter);
                container.appendChild(chapterElement)
                const puzzles = puzzlesPerChapter.get(chapter.id) || [];
                for (const puzzle of puzzles) {
                    addPuzzleToChapter(createPuzzleElement(puzzle), chapterElement);
                }
            }

            const archivedChapter = createArchivedChapter();
            container.parentElement.appendChild(archivedChapter);
            for (const puzzle of puzzlesPerChapter.get('archived')) {
                addPuzzleToChapter(createPuzzleElement(puzzle), archivedChapter);
            }


            for (const list of document.getElementsByClassName('puzzles')) {
                Sortable.create(list, {
                    animation: 200,
                    group: 'puzzles'
                })
            }

            for (const list of document.getElementsByClassName('chapters')) {
                Sortable.create(list, {
                    animation: 200,
                    group: 'chapters',
                    handle: '.chapter__handle'
                })
            }
        </script>
    </jsp:body>
</p:main_page>


