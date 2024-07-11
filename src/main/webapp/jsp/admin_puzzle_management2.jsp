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
            <div id="puzzle-management">
                <div id="puzzle-management-controls">
                    <div class="d-flex gap-2">
                        <button type="button" id="button-upload-chapters" class="btn btn-sm btn-outline-secondary">
                            Upload chapters
                            <i class="fa fa-upload ms-1"></i>
                        </button>
                        <button type="button" id="button-upload-puzzles" class="btn btn-sm btn-outline-secondary">
                            Upload puzzles
                            <i class="fa fa-upload ms-1"></i>
                        </button>
                        <button type="button" id="button-add-chapter" class="btn btn-sm btn-outline-secondary">
                            Add empty chapter
                            <i class="fa fa-plus ms-1"></i>
                        </button>
                    </div>
                    <button type="button" id="button-save" class="btn btn-primary btn-lg btn-highlight">
                        Save
                        <i class="fa fa-save ms-1"></i>
                    </button>
                </div>

                <div class="chapter" id="chapter-unassigned">
                    <div class="chapter__header">
                        <span class="chapter__title">Unassigned Puzzles</span>
                    </div>
                    <div class="puzzles"></div>
                </div>

                <div class="chapters"></div>

                <div class="chapter" id="chapter-archived">
                    <div class="chapter__header">
                        <span class="chapter__title">Archived Puzzles</span>
                    </div>
                    <div class="puzzles"></div>
                </div>
            </div>
        </div>

        <script type="module">
            import {Sortable} from '${url.forPath('/js/sortablejs.mjs')}';
            import {PuzzleAPI} from '${url.forPath("/js/codedefenders_main.mjs")}';


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

            let isUnsavedChanges = false;


            function createChapterElement(chapter) {
                const chapterDiv = document.createElement('div');
                chapterDiv.classList.add('chapter');
                chapterDiv.innerHTML =
                        `<div class="chapter__header">
                            <div class="chapter__info">
                                <div class="d-flex align-items-stretch">
                                    <span class="chapter__index"></span>
                                    <span class="chapter__title"></span>
                                </div>
                                <div class="chapter__description"></div>
                            </div>
                            <div class="chapter__controls">
                                <div class="chapter__handle me-3"></div>
                                <button class="btn btn-xs btn-primary btn-fixed chapter__button__edit" title="Edit">
                                    <i class="fa fa-edit"></i>
                                </button>
                                <button class="btn btn-xs btn-primary btn-fixed chapter__button__upload" title="Upload Puzzles">
                                    <i class="fa fa-upload"></i>
                                </button>
                                <button class="btn btn-xs btn-danger btn-fixed chapter__button__delete" title="Delete">
                                    <i class="fa fa-trash"></i>
                                </button>
                            </div>
                        </div>
                        <div class="puzzles"></div>`;

                chapterDiv.querySelector('.chapter__title').innerText = chapter.title;
                chapterDiv.querySelector('.chapter__description').innerText = chapter.description;

                chapterDiv.querySelector('.chapter__controls').firstElementChild
                        .insertAdjacentElement('afterend', createMoveDropdown('Move to position:'));

                chapterDiv.dataset.id = chapter.id;
                return chapterDiv;
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
                            <button class="btn btn-xs btn-primary btn-fixed puzzle__button__edit" title="Edit">
                                <i class="fa fa-edit"></i>
                            </button>

                            <button class="btn btn-xs btn-secondary btn-fixed puzzle__button__unassign" title="Unassign">
                                <i class="fa fa-times"></i>
                            </button>

                            <button class="btn btn-xs btn-secondary btn-fixed puzzle__button__archive" title="Archive">
                                <i class="fa fa-archive"></i>
                            </button>

                            <button class="btn btn-xs btn-danger btn-fixed puzzle__button__delete" title="Delete">
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
                    const deleteButton = puzzleDiv.querySelector('.puzzle__button__delete');
                    deleteButton.disabled = true;
                    deleteButton.title = "Puzzles with existing games can't be deleted";
                }
                puzzleDiv.querySelector('.puzzle__controls').firstElementChild
                        .insertAdjacentElement('afterend', createMoveDropdown('Move to chapter:'));

                puzzleDiv.dataset.id = puzzle.id;
                return puzzleDiv;
            }

            function createMoveDropdown(label) {
                const dropdown = document.createElement('div');
                dropdown.classList.add('dropdown', 'move', 'd-flex');
                dropdown.title = 'Move';
                dropdown.innerHTML =
                        `<button class="btn btn-xs btn-primary btn-fixed move__show"
                            data-bs-toggle="dropdown" data-bs-offset="0,8">
                        <i class="fa fa-arrow-right"></i>
                    </button>
                    <div class="dropdown-menu move__menu">
                        <div class="d-flex flex-column gap-1">
                            <div class="move__label"></div>
                            <div class="d-flex flex-row gap-2">
                                <select class="form-select form-select-sm move__position"></select>
                                <button type="button" class="btn btn-primary btn-xs move__confirm">
                                    Move
                                </button>
                            </div>
                        </div
                    </div>`;
                dropdown.querySelector('.move__label').innerText = label;
                return dropdown;
            }

            function createChapterSelectOptions(selectedChapter = null) {
                let options = document.createDocumentFragment();
                let index = 1;
                for (const chapter of document.querySelectorAll('.chapters .chapter')) {
                    const title = chapter.querySelector('.chapter__title').innerText;
                    const option = document.createElement('option');
                    option.value = String(index);
                    option.innerText = `\${index} | \${title}`;
                    if (chapter === selectedChapter) {
                        option.disabled = true;
                    }
                    options.appendChild(option);
                    index++;
                }
                return options;
            }

            function addPuzzleToChapter(puzzleElement, chapterElement) {
                chapterElement.querySelector('.puzzles').appendChild(puzzleElement);
            }

            function moveChapterToIndex(chapterElement, index) {
                index--; // 1-indexed
                const allChapters = document.querySelector('.chapters').children;
                const clampedIndex = Math.max(0, Math.min(allChapters.length - 1, index));

                let ownIndex = 0;
                for (const element of allChapters) {
                    if (element === chapterElement) {
                        break;
                    }
                    ownIndex++;
                }

                if (ownIndex > clampedIndex) {
                    allChapters.item(clampedIndex).insertAdjacentElement('beforebegin', chapterElement);
                } else if (ownIndex < clampedIndex) {
                    allChapters.item(clampedIndex).insertAdjacentElement('afterend', chapterElement);
                }
            }

            function movePuzzleToChapterIndex(puzzleElement, index) {
                index--; // 1-indexed
                const allChapters = document.querySelector('.chapters').children;
                const clampedIndex = Math.max(0, Math.min(allChapters.length - 1, index));
                const chapter = allChapters.item(clampedIndex);

                if (chapter !== puzzleElement.closest('.chapter')) {
                    addPuzzleToChapter(puzzleElement, chapter);
                }
            }

            function archivePuzzle(puzzleElement) {
                document.querySelector('#chapter-archived .puzzles').appendChild(puzzleElement);
            }

            function unassignPuzzle(puzzleElement) {
                document.querySelector('#chapter-unassigned .puzzles').appendChild(puzzleElement);
            }

            function getChapterData() {
                const data = {};

                const unassignedPuzzles = document.querySelector('#chapter-unassigned .puzzles').children;
                data.unassigned = Array.from(unassignedPuzzles).map(el => el.dataset.id);

                const archivedPuzzles = document.querySelector('#chapter-archived .puzzles').children
                data.archived = Array.from(archivedPuzzles).map(el => el.dataset.id);

                data.chapters = [];
                for (const chapter of document.querySelector('.chapters').children) {
                    const puzzles = chapter.querySelector('.puzzles').children;
                    data.chapters.push(Array.from(puzzles).map(el => el.dataset.id));
                }

                return data;
            }

            function init() {
                const unassignedChapter = document.getElementById('chapter-unassigned');
                for (const puzzle of puzzlesPerChapter.get('unassigned')) {
                    addPuzzleToChapter(createPuzzleElement(puzzle), unassignedChapter);
                }

                const container = document.querySelector('#puzzle-management .chapters');
                for (const chapter of chapters) {
                    const chapterElement = createChapterElement(chapter);
                    container.appendChild(chapterElement)
                    const puzzles = puzzlesPerChapter.get(chapter.id) || [];
                    for (const puzzle of puzzles) {
                        addPuzzleToChapter(createPuzzleElement(puzzle), chapterElement);
                    }
                }

                const archivedChapter = document.getElementById('chapter-archived');
                for (const puzzle of puzzlesPerChapter.get('archived')) {
                    addPuzzleToChapter(createPuzzleElement(puzzle), archivedChapter);
                }


                for (const list of document.getElementsByClassName('puzzles')) {
                    Sortable.create(list, {
                        animation: 200,
                        group: 'puzzles',
                        onMove: function() {
                            isUnsavedChanges = true;
                        }
                    })
                }

                for (const list of document.getElementsByClassName('chapters')) {
                    Sortable.create(list, {
                        animation: 200,
                        group: 'chapters',
                        handle: '.chapter__handle',
                        onMove: function() {
                            isUnsavedChanges = true;
                        }
                    })
                }

                document.getElementById('puzzle-management').addEventListener('show.bs.dropdown', function (event) {
                    const toggleButton = event.target.closest('.chapter__controls .move__show');
                    if (toggleButton === null) {
                        return;
                    }

                    const chapter = event.target.closest('.chapter');
                    const options = createChapterSelectOptions(chapter);

                    const select = chapter.querySelector('.move__position');
                    select.innerText = '';
                    select.appendChild(options);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const moveButton = event.target.closest('.chapter__controls .move__confirm');
                    if (moveButton === null) {
                        return;
                    }

                    const chapter = event.target.closest('.chapter');
                    const select = chapter.querySelector('.move__position');

                    const position = Number(select.value);
                    moveChapterToIndex(chapter, position);
                });

                document.getElementById('puzzle-management').addEventListener('show.bs.dropdown', function (event) {
                    const toggleButton = event.target.closest('.puzzle__controls .move__show');
                    if (toggleButton === null) {
                        return;
                    }

                    const puzzle = event.target.closest('.puzzle');
                    const chapter = puzzle.closest('.chapter');
                    const options = createChapterSelectOptions(chapter);

                    const select = puzzle.querySelector('.move__position');
                    select.innerText = '';
                    select.appendChild(options);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const moveButton = event.target.closest('.puzzle__controls .move__confirm');
                    if (moveButton === null) {
                        return;
                    }

                    const puzzle = event.target.closest('.puzzle');
                    const select = puzzle.querySelector('.move__position');

                    const position = Number(select.value);
                    movePuzzleToChapterIndex(puzzle, position);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const archiveButton = event.target.closest('.puzzle__button__archive');
                    if (archiveButton === null) {
                        return;
                    }

                    const puzzle = event.target.closest('.puzzle');
                    archivePuzzle(puzzle);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const unassignButton = event.target.closest('.puzzle__button__unassign');
                    if (unassignButton === null) {
                        return;
                    }

                    const puzzle = event.target.closest('.puzzle');
                    unassignPuzzle(puzzle);
                });
            }

            init();

            window.addEventListener('beforeunload', function(event) {
                if (isUnsavedChanges) {
                    event.preventDefault();
                }
            });

            document.getElementById('button-save').addEventListener('click', function(event) {
                isUnsavedChanges = false;
            });
        </script>
    </jsp:body>
</p:main_page>
