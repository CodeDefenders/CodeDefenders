<%--

    Copyright (C) 2016-2025 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.GameLevel" %>

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
                    <button type="button" id="button-save" class="btn btn-primary btn-lg btn-highlight button-save">
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

                <div id="chapters"></div>

                <div class="chapter" id="chapter-archived">
                    <div class="chapter__header">
                        <span class="chapter__title">Archived Puzzles</span>
                    </div>
                    <div class="puzzles"></div>
                </div>
            </div>
        </div>
        <div id="toasts" class="position-fixed top-0 end-0 p-3 d-flex flex-column gap-1" style="z-index: 11"></div>

        <t:modal title="Unsaved changes" id="unsaved-changes-modal" closeButtonText="Cancel">
            <jsp:attribute name="content">
                You have unsaved changes. Please save first.
            </jsp:attribute>
            <jsp:attribute name="footer">
                <button type="button" class="btn btn-primary button-save">
                    Save
                    <i class="fa fa-save ms-1"></i>
                </button>
            </jsp:attribute>
        </t:modal>

        <form id="uploadPuzzles" name="uploadPuzzles" action="${url.forPath(Paths.ADMIN_PUZZLE_UPLOAD)}"
            method="post" enctype="multipart/form-data" autocomplete="off">
            <input type="hidden" name="formType" value="uploadPuzzles">
            <input type="hidden" name="chapterId" value="">

            <t:modal title="Upload Puzzles" id="upload-puzzle-modal" modalDialogClasses="modal-dialog-responsive">
                <jsp:attribute name="content">
                    <div style="width: 700px;">
                        <p>
                            Puzzles are uploaded in <code>.zip</code> archives.
                            You can upload multiple puzzle archives at once by selecting multiple in the file dialog.
                            For details on the convention, please expand the explanations.
                        </p>

                        <input class="form-control mb-4" type="file" id="fileUploadPuzzle" name="fileUploadPuzzle"
                               accept=".zip" multiple>

                        <details class="border rounded p-2 mb-3">
                            <summary>Puzzle Format Explanation</summary>
                            <div class="mt-3 p-1">
                                <t:import_puzzle_explanation/>
                            </div>
                        </details>
                        <details class="border rounded p-2 mb-3">
                            <summary>Puzzle Properties Explanation</summary>
                            <div class="mt-3 p-1">
                                <t:import_puzzle_properties_explanation/>
                            </div>
                        </details>
                        <details class="border rounded p-2">
                            <summary>Downloads</summary>
                            <div class="mt-3 p-1">
                                <ul>
                                    <li>
                                        Empty puzzle template:
                                        <a href="${url.forPath("puzzle-importer/puzzle_template.zip")}" download>
                                            puzzle_template.zip
                                        </a>
                                    </li>
                                    <li>
                                        Example properties file:
                                        <a href="${url.forPath("puzzle-importer/puzzle_properties.zip")}" download>
                                            puzzle.properties
                                        </a>
                                    </li>
                                    <li>
                                        Example attacker puzzle:
                                        <a href="${url.forPath("/puzzle-importer/puzzle_attacker.zip")}" download>
                                            puzzle_attacker.zip
                                        </a>
                                    </li>
                                    <li>
                                        Example defender puzzle:
                                        <a href="${url.forPath("puzzle-importer/puzzle_defender.zip")}" download>
                                            puzzle_defender.zip
                                        </a>
                                    </li>
                                    <li>
                                        Example puzzle with dependencies:
                                        <a href="${url.forPath("puzzle-importer/puzzle_deps.zip")}" download>
                                            puzzle_deps.zip
                                        </a>
                                    </li>
                                </ul>
                            </div>
                        </details>
                    </div>
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button class="btn btn-primary" type="submit" id="uploadPuzzlesButton"
                            onclick="this.form.submit(); this.disabled = true; this.innerText='Uploading...';">
                        Upload
                    </button>
                </jsp:attribute>
            </t:modal>
        </form>

        <form id="uploadChapters" name="uploadChapters" action="${url.forPath(Paths.ADMIN_PUZZLE_UPLOAD)}"
            class="form-width" method="post" enctype="multipart/form-data" autocomplete="off">
            <input type="hidden" name="formType" value="uploadPuzzleChapters">

            <t:modal title="Upload Chapters" id="upload-chapter-modal" modalDialogClasses="modal-dialog-responsive">
                <jsp:attribute name="content">
                    <div style="width: 700px;">
                        <p>
                            Chapters are uploaded in <code>.zip</code> archives.
                            You can upload multiple chapter archives at once by selecting multiple in the file dialog.
                            For details on the convention, please expand the explanation.
                        </p>

                        <p>
                            Uploading large chapters may take several minutes.
                        </p>

                        <input class="form-control mb-4" type="file" id="fileUploadChapter" name="fileUploadChapter"
                               accept=".zip" multiple>

                        <details class="border rounded p-2 mb-3">
                            <summary>Chapter Format Explanation</summary>
                            <div class="mt-3 p-1">
                                <t:import_puzzle_chapter_explanation/>
                            </div>
                        </details>
                        <details class="border rounded p-2 mb-3">
                            <summary>Puzzle Format Explanation</summary>
                            <div class="mt-3 p-1">
                                <t:import_puzzle_explanation/>
                            </div>
                        </details>
                        <details class="border rounded p-2 mb-3">
                            <summary>Puzzle Properties Explanation</summary>
                            <div class="mt-3 p-1">
                                <t:import_puzzle_properties_explanation/>
                            </div>
                        </details>
                        <details class="border rounded p-2">
                            <summary>Downloads</summary>
                            <div class="mt-3 p-1">
                                <ul>
                                    <li>
                                        Example chapter:
                                        <a href="${url.forPath("puzzle-importer/chapter_example.zip")}" download>
                                            chapter_example.zip
                                        </a>
                                    </li>
                                    <li>
                                        Example properties file:
                                        <a href="${url.forPath("puzzle-importer/chapter.properties")}" download>
                                            chapter.properties
                                        </a>
                                    </li>
                                    <li>
                                        Empty chapter template:
                                        <a href="${url.forPath("puzzle-importer/chapter_template.zip")}" download>
                                            chapter_template.zip
                                        </a>
                                    </li>
                                </ul>
                                <p>Puzzle Downloads:</p>
                                <ul>
                                    <li>
                                        Empty puzzle template:
                                        <a href="${url.forPath("puzzle-importer/puzzle_template.zip")}" download>
                                            puzzle_template.zip
                                        </a>
                                    </li>
                                    <li>
                                        Example properties file:
                                        <a href="${url.forPath("puzzle-importer/puzzle.properties")}" download>
                                            puzzle.properties
                                        </a>
                                    </li>
                                    <li>
                                        Example attacker puzzle:
                                        <a href="${url.forPath("/puzzle-importer/puzzle_attacker.zip")}" download>
                                            puzzle_attacker.zip
                                        </a>
                                    </li>
                                    <li>
                                        Example defender puzzle:
                                        <a href="${url.forPath("puzzle-importer/puzzle_defender.zip")}" download>
                                            puzzle_defender.zip
                                        </a>
                                    </li>
                                    <li>
                                        Example puzzle with dependencies:
                                        <a href="${url.forPath("puzzle-importer/puzzle_deps.zip")}" download>
                                            puzzle_deps.zip
                                        </a>
                                    </li>
                                </ul>
                            </div>
                        </details>
                    </div>
                </jsp:attribute>
                <jsp:attribute name="footer">
                        <button class="btn btn-primary" type="submit" id="uploadChaptersButton"
                                onclick="this.form.submit(); this.disabled = true; this.innerText='Uploading...';">
                            Upload
                        </button>
                </jsp:attribute>
            </t:modal>
        </form>

        <script type="module">
            import {Toast, Modal as BootstrapModal} from '${url.forPath('/js/bootstrap.mjs')}';
            import {Sortable} from '${url.forPath('/js/sortablejs.mjs')}';
            import {PuzzleAPI, Modal} from '${url.forPath("/js/codedefenders_main.mjs")}';

            const watermarkUrls = {
                ATTACKER: '${url.forPath("/images/achievements/codedefenders_achievements_1_lvl_0.png")}',
                DEFENDER: '${url.forPath("/images/achievements/codedefenders_achievements_2_lvl_0.png")}',
                EQUIVALENCE: '${url.forPath("/images/ingameicons/equivalence.png")}'
            }
            const puzzlePreviewUrl = '${url.forPath(Paths.ADMIN_PUZZLE_MANAGEMENT)}';

            // ==== Init Data ==========================================================================================

            const puzzleData = await PuzzleAPI.fetchPuzzleData();
            const puzzles = puzzleData.puzzles;
            const chapters = puzzleData.chapters;

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

            // ==== Globals ============================================================================================

            let archivedChapter;
            let unassignedChapter;
            const chaptersContainer = document.getElementById('chapters');
            let isUnsavedChanges = false;

            // ==== Components =========================================================================================

            class ChapterComponent {
                constructor(container = null) {
                    this.container = container === null
                        ? ChapterComponent._createElement()
                        : container;
                    this.title = this.container.querySelector('.chapter__title');
                    this.description = this.container.querySelector('.chapter__description');
                    this.puzzlesContainer = this.container.querySelector('.puzzles');
                    this.puzzles = this.puzzlesContainer.children;

                    Sortable.create(this.puzzlesContainer, {
                        animation: 200,
                        group: 'puzzles',
                        onMove: function() {
                            isUnsavedChanges = true;
                        },
                        onChoose: function(e) {
                            e.item.classList.add('shadow-none');
                        },
                        onUnchoose: function(e) {
                            e.item.classList.remove('shadow-none');
                        }
                    });

                    this.container.chapterComp = this;
                }

                static _createElement() {
                    const container = document.createElement('div');
                    container.classList.add('chapter');
                    container.innerHTML = `
                        <div class="chapter__header">
                            <div class="chapter__info">
                                <div class="d-flex align-items-stretch">
                                    <span class="chapter__index"></span>
                                    <span class="chapter__title"></span>
                                </div>
                                <div class="chapter__description"></div>
                            </div>
                            <div class="chapter__controls">
                                <div class="chapter__handle me-3" title="Drag to move chapter"></div>
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

                    const controls = container.querySelector('.chapter__controls');
                    controls.firstElementChild.insertAdjacentElement('afterend', createChapterSelectDropdown({
                        label: 'Move to position:',
                        tooltip: 'Move to position'
                    }));

                    return container;
                }

                static forChapter(chapter) {
                    const chapterComp = new ChapterComponent();
                    chapterComp.chapter = chapter;
                    chapterComp.container.dataset.id = chapter.id;

                    chapterComp.title.innerText = chapter.title;
                    chapterComp.title.title = chapter.title;
                    chapterComp.description.innerText = chapter.description;
                    chapterComp.description.description = chapter.description;
                    return chapterComp;
                }

                static fromChild(childElement) {
                    return childElement.closest('.chapter').chapterComp;
                }

                addPuzzle(puzzleComp) {
                    this.puzzlesContainer.appendChild(puzzleComp.container);
                }

                moveToIndex(index) {
                    index--; // 1-indexed

                    const clampedIndex = Math.max(0, Math.min(chaptersContainer.children.length - 1, index));
                    const ownIndex = [...chaptersContainer.children].indexOf(this.container);

                    if (ownIndex > clampedIndex) {
                        chaptersContainer.children.item(clampedIndex)
                                .insertAdjacentElement('beforebegin', this.container);
                    } else if (ownIndex < clampedIndex) {
                        chaptersContainer.children.item(clampedIndex)
                                .insertAdjacentElement('afterend', this.container);
                    }
                }
            }

            class PuzzleComponent {
                constructor() {
                    this.container = PuzzleComponent._createElement();
                    this.title = this.container.querySelector('.puzzle__title');
                    this.description = this.container.querySelector('.puzzle__description');
                    this.tags = {
                        id: this.container.querySelector('.puzzle__tag__id'),
                        games: this.container.querySelector('.puzzle__tag__games'),
                        level: this.container.querySelector('.puzzle__tag__level'),
                        equivalent: this.container.querySelector('.puzzle__tag__equivalent'),
                    };
                    this.container.puzzleComp = this;
                }

                static _createElement() {
                    const watermark = document.createElement('img');
                    watermark.classList.add('puzzle__watermark');

                    const container = document.createElement('div');
                    container.classList.add('puzzle')
                    container.innerHTML = `
                        <div class="puzzle__content">
                            <div class="puzzle__info">
                                <div class="puzzle__title"></div>
                                <div class="puzzle__description"></div>
                            </div>
                            <div class="puzzle__tags">
                                <span class="badge puzzle__tag puzzle__tag__id" title="Puzzle ID"></span>
                                <span class="badge puzzle__tag puzzle__tag__games" title="Number of games with the puzzle"></span>
                                <span class="badge puzzle__tag puzzle__tag__level" title="Puzzle level"></span>
                                <span class="badge puzzle__tag puzzle__tag__equivalent title="Mutant is equivalent" hidden">
                                    <i class="fa fa-flag"></i>
                                </span>
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

                    container.appendChild(watermark);
                    container.querySelector('.puzzle__controls').firstElementChild
                            .insertAdjacentElement('afterend', createChapterSelectDropdown({
                                label: 'Move to chapter:',
                                tooltip: 'Move to chapter'
                            }));
                    return container;
                }

                setPuzzle(puzzle) {
                    this.puzzle = puzzle;
                    this.container.dataset.id = puzzle.id;

                    this.title.innerText = puzzle.title;
                    this.title.title = puzzle.title;
                    this.description.innerText = puzzle.description;
                    this.description.title = puzzle.title;
                    this.tags.id.innerText = '#' + puzzle.id;
                    this.tags.games.innerText = puzzle.gameCount + ' game' + (puzzle.gameCount === 1 ? '' : 's');
                    this.tags.level.innerText = puzzle.level.toLowerCase();

                    if (puzzle.type === 'EQUIVALENCE' && puzzle.isEquivalent) {
                        this.tags.equivalent.removeAttribute('hidden');
                    } else {
                        this.tags.equivalent.setAttribute('hidden', '');
                    }

                    this.container.classList.add(`puzzle-\${puzzle.type.toLowerCase()}`);
                    this.container.querySelector('.puzzle__watermark').src = watermarkUrls[puzzle.type];

                    if (puzzle.gameCount > 0) {
                        const deleteButton = this.container.querySelector('.puzzle__button__delete');
                        deleteButton.disabled = true;
                        deleteButton.title = "Puzzles with existing games can't be deleted";
                    }
                }

                static forPuzzle(puzzle) {
                    const puzzleComp = new PuzzleComponent();
                    puzzleComp.setPuzzle(puzzle);
                    return puzzleComp;
                }

                static fromChild(childElement) {
                    return childElement.closest('.puzzle').puzzleComp;
                }

                moveToChapterIndex(index) {
                    index--; // 1-indexed

                    const clampedIndex = Math.max(0, Math.min(chaptersContainer.children.length - 1, index));
                    const chapterComp = chaptersContainer.children.item(clampedIndex).chapterComp;
                    chapterComp.addPuzzle(this);
                }
            }

            // ==== Functions ==========================================================================================

            /**
             * Creates a button that opens a dropdown menu for selecting chapters.
             * The menu contains a label, a select for the puzzle chapters, and a confirm button.
             */
            function createChapterSelectDropdown({
                     tooltip = 'Move',
                     label = 'Move to chapter:',
                     showButtonClasses = 'btn btn-xs btn-primary btn-fixed',
                     showButtonContent = '<i class="fa fa-arrow-right"></i>',
                     moveButtonContent = 'Move'
                }) {
                const dropdown = document.createElement('div');
                dropdown.classList.add('dropdown', 'chapter_select', 'd-flex');
                dropdown.title = tooltip;
                dropdown.innerHTML = `
                    <button class="\${showButtonClasses} chapter_select__show"
                            data-bs-toggle="dropdown" data-bs-offset="0,8">
                            \${showButtonContent}
                    </button>
                    <div class="dropdown-menu chapter_select__menu">
                        <div class="d-flex flex-column gap-1">
                            <div class="chapter_select__label">\${label}</div>
                            <div class="d-flex flex-row gap-2">
                                <select class="form-select form-select-sm chapter_select__position"></select>
                                <button type="button" class="btn btn-primary btn-sm chapter_select__confirm">
                                    \${moveButtonContent}
                                </button>
                            </div>
                        </div
                    </div>`;
                return dropdown;
            }

            /**
             * Creates the options for the chapter-select dropdown as a DocumentFragment.
             * @param currentChapter A ChapterComponent for the current chapter. Will be greyed out in the list.
             */
            function createChapterSelectOptions(currentChapter = null) {
                let options = document.createDocumentFragment();
                let index = 1;
                for (const chapterElem of chaptersContainer.children) {
                    const title = chapterElem.chapterComp.chapter.title;
                    const option = document.createElement('option');
                    option.value = String(index);
                    option.innerText = `\${index} | \${title}`;
                    if (chapterElem.chapterComp === currentChapter) {
                        option.disabled = true;
                    }
                    options.appendChild(option);
                    index++;
                }
                return options;
            }

            function showToast({colorClass = 'bg-primary', title = '', secondary = '', body = ''}) {
                const toastElem = document.createElement('div');
                toastElem.classList.add('toast', 'bg-white');
                toastElem.role = 'alert';
                toastElem.innerHTML = `
                    <div class="toast-header">
                        <div class="toast-color p-2 me-2 rounded-1"></div>
                        <strong class="toast-title me-auto"></strong>
                        <small class="toast-secondary text-body-secondary"></small>
                        <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                    </div>
                    <div class="toast-body"></div>`;

                toastElem.querySelector('.toast-color').classList.add(colorClass);
                toastElem.querySelector('.toast-title').innerText = title;
                toastElem.querySelector('.toast-secondary').innerText = secondary;
                toastElem.querySelector('.toast-body').innerText = body;

                document.getElementById('toasts').appendChild(toastElem);
                new Toast(toastElem).show();

                toastElem.addEventListener('hidden.bs.toast', () => {
                    setTimeout(() => toastElem.remove(), 1000);
                });
            }

            /**
             * Gathers the positions of unassigned puzzles, archived puzzles and all puzzle chapters.
             */
            function getPuzzlePositions() {
                const data = {};

                data.unassignedPuzzles = [...unassignedChapter.puzzles]
                        .map(puzzlesElem => PuzzleComponent.fromChild(puzzlesElem).puzzle.id);

                data.archivedPuzzles = [...archivedChapter.puzzles]
                        .map(puzzlesElem => PuzzleComponent.fromChild(puzzlesElem).puzzle.id);

                data.chapters = [];
                for (const chapterElem of chaptersContainer.children) {
                    const chapterComp = ChapterComponent.fromChild(chapterElem);
                    data.chapters.push({
                        id: chapterComp.chapter.id,
                        puzzles: [...chapterComp.puzzles]
                                .map(puzzlesElem => PuzzleComponent.fromChild(puzzlesElem).puzzle.id)
                    });
                }

                return data;
            }

            // ==== Init Code ==========================================================================================

            function initChaptersAndPuzzles() {
                unassignedChapter = new ChapterComponent(document.getElementById('chapter-unassigned'));
                for (const puzzle of puzzlesPerChapter.get('unassigned')) {
                    unassignedChapter.addPuzzle(PuzzleComponent.forPuzzle(puzzle));
                }

                for (const chapter of chapters) {
                    const chapterComp = ChapterComponent.forChapter(chapter);
                    chaptersContainer.appendChild(chapterComp.container);
                    const puzzles = puzzlesPerChapter.get(chapter.id) ?? [];
                    for (const puzzle of puzzles) {
                        chapterComp.addPuzzle(PuzzleComponent.forPuzzle(puzzle));
                    }
                }

                archivedChapter = new ChapterComponent(document.getElementById('chapter-archived'));
                for (const puzzle of puzzlesPerChapter.get('archived')) {
                    archivedChapter.addPuzzle(PuzzleComponent.forPuzzle(puzzle));
                }
            }

            function initChapterSelects() {
                // --- Init 'Scroll to chapter' ------------------------------------------------------------------------

                const scrollDropdown = createChapterSelectDropdown({
                    tooltip: 'Scroll to chapter',
                    label: 'Scroll to chapter:',
                    moveButtonContent: 'Go',
                    showButtonClasses: 'btn btn-sm btn-outline-secondary',
                    showButtonContent: 'Scroll to chapter <i class="fa fa-arrow-down ms-1"></i>',
                });
                document.getElementById('button-add-chapter').insertAdjacentElement('afterend', scrollDropdown);

                scrollDropdown.querySelector('.chapter_select__confirm').addEventListener('click', function (event) {
                    const select = event.target.closest('.chapter_select').querySelector('.chapter_select__position');
                    const index = Number(select.value) - 1;

                    chaptersContainer.children.item(index).scrollIntoView();
                });

                scrollDropdown.querySelector('.chapter_select__show').addEventListener('click', function (event) {
                    const options = createChapterSelectOptions();
                    const select = event.target.closest('.chapter_select').querySelector('.chapter_select__position');
                    select.innerText = '';
                    select.appendChild(options);
                });

                // --- Init 'Move chapter' -----------------------------------------------------------------------------

                document.getElementById('puzzle-management').addEventListener('show.bs.dropdown', function (event) {
                    const toggleButton = event.target.closest('.chapter__controls .chapter_select__show');
                    if (toggleButton === null) {
                        return;
                    }

                    const chapterComp = ChapterComponent.fromChild(event.target);
                    const options = createChapterSelectOptions(chapterComp);
                    const select = chapterComp.container.querySelector('.chapter_select__position');
                    select.innerText = '';
                    select.appendChild(options);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const moveButton = event.target.closest('.chapter__controls .chapter_select__confirm');
                    if (moveButton === null) {
                        return;
                    }

                    const chapterComp = ChapterComponent.fromChild(event.target);
                    const select = chapterComp.container.querySelector('.chapter_select__position');
                    const position = Number(select.value);
                    chapterComp.moveToIndex(position);
                    isUnsavedChanges = true;
                });

                // --- Init 'Move puzzle' ------------------------------------------------------------------------------

                document.getElementById('puzzle-management').addEventListener('show.bs.dropdown', function (event) {
                    const toggleButton = event.target.closest('.puzzle__controls .chapter_select__show');
                    if (toggleButton === null) {
                        return;
                    }

                    const puzzle = PuzzleComponent.fromChild(event.target);
                    const chapter = ChapterComponent.fromChild(puzzle.container);
                    const options = createChapterSelectOptions(chapter);

                    const select = puzzle.container.querySelector('.chapter_select__position');
                    select.innerText = '';
                    select.appendChild(options);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const moveButton = event.target.closest('.puzzle__controls .chapter_select__confirm');
                    if (moveButton === null) {
                        return;
                    }

                    const puzzle = PuzzleComponent.fromChild(event.target);
                    const select = puzzle.container.querySelector('.chapter_select__position');
                    const position = Number(select.value);
                    puzzle.moveToChapterIndex(position);
                    isUnsavedChanges = true;
                });
            }

            function initModals() {
                // Edit Chapter Modal
                chaptersContainer.addEventListener('click', function (event) {
                    const editButton = event.target.closest('.chapter__button__edit');
                    if (editButton === null) {
                        return;
                    }

                    const chapterComp = ChapterComponent.fromChild(event.target);
                    const chapter = chapterComp.chapter;

                    const modal = new Modal();
                    modal.body.innerHTML =`
                        <form class="" novalidate>
                            <div class="row mb-3">
                                <div class="form-group">
                                    <label class="form-label">Title</label>
                                    <input type="text" name="title" class="form-control" value=""
                                        placeholder="Title"
                                        required minlength="1" maxlength="100">
                                    <div class="invalid-feedback">Please enter a title.</div>
                                </div>
                            </div>

                            <div class="row mb-2">
                                <div class="form-group">
                                    <label class="form-label">Description</label>
                                    <input type="text" name="description" class="form-control" value=""
                                        placeholder="Description"
                                        maxlength="1000">
                                </div>
                            </div>
                        </form>`;
                    modal.title.innerText = 'Edit Chapter';
                    modal.footerCloseButton.innerText = 'Cancel';
                    modal.modal.dataset.id = chapter.id;
                    modal.body.querySelector('input[name="title"]').value = chapter.title;
                    modal.body.querySelector('input[name="description"]').value = chapter.description;

                    const saveButton = document.createElement('button');
                    saveButton.classList.add('btn', 'btn-primary');
                    saveButton.role = 'button';
                    saveButton.innerText = 'Save';
                    modal.footer.insertAdjacentElement('beforeend', saveButton);

                    saveButton.addEventListener('click', function(event) {
                        const form = modal.modal.querySelector('form');
                        if (!form.checkValidity()) {
                            form.classList.add('was-validated');
                            return;
                        }

                        const title = modal.body.querySelector('input[name="title"]').value;
                        const description = modal.body.querySelector('input[name="description"]').value;

                        PuzzleAPI.updatePuzzleChapter(chapter.id, {
                            title: title,
                            description: description
                        }).then(response => {
                            chapter.title = response.chapter.title;
                            chapter.description = response.chapter.description;
                            chapterComp.title.innerText = response.chapter.title;
                            chapterComp.description.innerText = response.chapter.description;
                            showToast({title: 'Success', body: response.message});
                        }).catch(async response => {
                            showToast({title: 'Error', body: (await response).message, colorClass: 'bg-danger'});
                        }).finally(() => {
                            modal.controls.hide();
                            setTimeout(() => modal.modal.remove(), 1000);
                        });
                    });

                    modal.controls.show();
                });

                // Edit Puzzle Modal
                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const editButton = event.target.closest('.puzzle__button__edit');
                    if (editButton === null) {
                        return;
                    }

                    const puzzleComp = PuzzleComponent.fromChild(event.target);
                    const puzzle = puzzleComp.puzzle;

                    const modal = new Modal();
                    modal.body.innerHTML = `
                        <form class="" novalidate>
                            <div class="row mb-3">
                                <div class="col-12">
                                    <label class="form-label">Title</label>
                                    <input type="text" class="form-control" value="" name="title"
                                        placeholder="Title"
                                        required minlength="1" maxlength="100">
                                    <div class="invalid-feedback">Please enter a title.</div>
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-12">
                                    <label class="form-label">Description</label>
                                    <input type="text" class="form-control" value="" name="description"
                                        placeholder="Description"
                                        maxlength="1000">
                                </div>
                            </div>

                            <div class="row mb-3">
                                <div class="col-12">
                                    <label class="form-label">Max. Assertions</label>
                                    <input type="number" class="form-control" value="" name="maxAssertionsPerTest"
                                        min="1">
                                </div>
                            </div>

                            <div class="row g-3 mb-2 editable-lines">
                                <div class="col-6">
                                    <label class="form-label">First Editable Line</label>
                                    <input type="number" class="form-control" value="" name="editableLinesStart"
                                        min="1">
                                </div>
                                <div class="col-6">
                                    <label class="form-label">Last Editable Line</label>
                                    <input type="number" class="form-control" value="" name="editableLinesEnd"
                                        min="1">
                                </div>
                            </div>

                            <div class="row g-3 mb-2 difficulty-level">
                                <div class="col-12">
                                    <label class="form-label">Game Level</label>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" id="level-radio-easy" name="level"
                                               value="${GameLevel.EASY}" required>
                                        <label class="form-check-label" for="level-radio-easy">Easy</label>
                                        <div class="invalid-feedback">Please select a level.</div>
                                    </div>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" id="level-radio-hard" name="level"
                                               value="${GameLevel.HARD}" required
                                               checked>
                                        <label class="form-check-label" for="level-radio-hard">Hard</label>
                                    </div>
                                </div>
                            </div>

                            <div class="row g-3 mb-2 mutant-equivalent">
                                <div class="col-12">
                                    <label class="form-label">Equivalence</label>
                                    <div class="form-check">
                                        <input class="form-check-input" type="checkbox" value="" name="mutantEquivalent" id="mutantEquivalent">
                                        <label class="form-check-label" for="mutantEquivalent">
                                            Mutant equivalent
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </form>`;
                    modal.title.innerText = 'Edit Puzzle';
                    modal.footerCloseButton.innerText = 'Cancel';
                    modal.modal.dataset.id = puzzle.id;

                    const form = modal.body.querySelector("form");
                    form["title"].value = puzzle.title;
                    form["description"].value = puzzle.description;
                    form["maxAssertionsPerTest"].value = puzzle.maxAssertionsPerTest;
                    form["level"].value = puzzle.level;
                    if (puzzle.type === 'DEFENDER') {
                        modal.body.querySelector('.mutant-equivalent').remove();
                        modal.body.querySelector('.editable-lines').remove();
                    } else if (puzzle.type === 'ATTACKER') {
                        modal.body.querySelector('.mutant-equivalent').remove();
                        form["editableLinesStart"].value = puzzle.editableLinesStart;
                        form["editableLinesEnd"].value = puzzle.editableLinesEnd;
                    } else if (puzzle.type === 'EQUIVALENCE') {
                        modal.body.querySelector('.editable-lines').remove();
                        form["mutantEquivalent"].checked = puzzle.isEquivalent;
                    }

                    const saveButton = document.createElement('button');
                    saveButton.classList.add('btn', 'btn-primary');
                    saveButton.role = 'button';
                    saveButton.innerText = 'Save';
                    modal.footer.insertAdjacentElement('beforeend', saveButton);

                    saveButton.addEventListener('click', function(event) {
                        const form = modal.modal.querySelector('form');
                        if (!form.checkValidity()) {
                            form.classList.add('was-validated');
                            return;
                        }

                        const title = form["title"].value;
                        const description = form["description"].value;
                        const level = form["level"].value;
                        let maxAssertionsPerTest = form["maxAssertionsPerTest"].value;
                        maxAssertionsPerTest = Number(maxAssertionsPerTest);
                        let editableLinesStart = form["editableLinesStart"]?.value;
                        editableLinesStart = editableLinesStart ? Number(editableLinesStart) : null;
                        let editableLinesEnd = form["editableLinesEnd"]?.value;
                        editableLinesEnd = editableLinesEnd ? Number(editableLinesEnd) : null;
                        const isEquivalent = form["mutantEquivalent"]?.checked;

                        PuzzleAPI.updatePuzzle(puzzle.id, {
                            title,
                            description,
                            level,
                            maxAssertionsPerTest,
                            editableLinesStart,
                            editableLinesEnd,
                            isEquivalent
                        }).then(response => {
                            puzzle.title = response.puzzle.title;
                            puzzle.description = response.puzzle.description;
                            puzzle.level = response.puzzle.level;
                            puzzle.maxAssertionsPerTest = response.puzzle.maxAssertionsPerTest;
                            puzzle.editableLinesStart = response.puzzle.editableLinesStart;
                            puzzle.editableLinesEnd = response.puzzle.editableLinesEnd;
                            puzzle.isEquivalent = response.puzzle.isEquivalent;
                            puzzleComp.setPuzzle(puzzle);
                            showToast({title: 'Success', body: response.message});
                        }).catch(async response => {
                            showToast({title: 'Error', body: (await response).message, colorClass: 'bg-danger'});
                        }).finally(() => {
                            modal.controls.hide();
                            setTimeout(() => modal.modal.remove(), 1000);
                        });
                    });

                    modal.controls.show();
                });

                // Delete Chapter Modal
                chaptersContainer.addEventListener('click', function (event) {
                    const deleteButton = event.target.closest('.chapter__button__delete');
                    if (deleteButton === null) {
                        return;
                    }

                    const chapterComp = ChapterComponent.fromChild(event.target);
                    const chapter = chapterComp.chapter;

                    const modal = new Modal();
                    modal.body.innerHTML = `
                        <div>
                            Are you sure you want to delete chapter
                            <span class="px-1 border rounded-1 edit-chapter-title"></span>?
                            Any puzzles left in the chapter will be moved to
                            <span class="px-1 border rounded-1">Unassigned</span>.
                        </div>`;

                    modal.title.innerText = 'Delete Chapter';
                    modal.footerCloseButton.innerText = 'Cancel';
                    modal.modal.dataset.id = chapter.id;
                    modal.body.querySelector('.edit-chapter-title').innerText = chapter.title;

                    const saveButton = document.createElement('button');
                    saveButton.classList.add('btn', 'btn-danger');
                    saveButton.role = 'button';
                    saveButton.innerText = 'Delete';
                    modal.footer.insertAdjacentElement('beforeend', saveButton);

                    saveButton.addEventListener('click', function(event) {
                        isUnsavedChanges = true;
                        PuzzleAPI.deletePuzzleChapter(chapter.id)
                                .then(response => {
                                    for (const puzzleElem of [...chapterComp.puzzles]) {
                                        unassignedChapter.addPuzzle(PuzzleComponent.fromChild(puzzleElem));
                                    }
                                    chapterComp.container.remove();
                                    showToast({title: 'Success', body: response.message});
                                }).catch(async response => {
                                    showToast({title: 'Error', body: (await response).message, colorClass: 'bg-danger'});
                                }).finally(() => {
                                    modal.controls.hide();
                                    setTimeout(() => modal.modal.remove(), 1000);
                                });
                    });

                    modal.controls.show();
                });

                // Delete Puzzle Modal
                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const deleteButton = event.target.closest('.puzzle__button__delete');
                    if (deleteButton === null) {
                        return;
                    }

                    const puzzleComp = PuzzleComponent.fromChild(event.target);
                    const puzzle = puzzleComp.puzzle;

                    const modal = new Modal();
                    modal.body.innerHTML = `
                        <div>
                            Are you sure you want to permanently delete puzzle
                            <span class="px-1 border rounded-1 edit-puzzle-title"></span>?
                        </div>`;

                    modal.title.innerText = 'Delete Puzzle';
                    modal.footerCloseButton.innerText = 'Cancel';
                    modal.modal.dataset.id = puzzle.id;
                    modal.body.querySelector('.edit-puzzle-title').innerText = puzzle.title;

                    const saveButton = document.createElement('button');
                    saveButton.classList.add('btn', 'btn-danger');
                    saveButton.role = 'button';
                    saveButton.innerText = 'Delete';
                    modal.footer.insertAdjacentElement('beforeend', saveButton);

                    saveButton.addEventListener('click', function(event) {
                        PuzzleAPI.deletePuzzle(puzzle.id)
                                .then(response => {
                                    puzzleComp.container.remove();
                                    showToast({title: 'Success', body: response.message});
                                }).catch(async response => {
                                    showToast({title: 'Error', body: (await response).message, colorClass: 'bg-danger'});
                                }).finally(() => {
                                    modal.controls.hide();
                                    setTimeout(() => modal.modal.remove(), 1000);
                                });
                    });

                    modal.controls.show();
                });

                // Add Chapter Modal
                document.getElementById('button-add-chapter').addEventListener('click', function (event) {
                    const modal = new Modal();
                    modal.body.innerHTML =`
                            <form class="" novalidate>
                                <div class="row mb-3">
                                    <div class="form-group">
                                        <label class="form-label">Title</label>
                                        <input type="text" name="title" class="form-control" value=""
                                            placeholder="Title"
                                            required minlength="1" maxlength="100">
                                        <div class="invalid-feedback">Please enter a title.</div>
                                    </div>
                                </div>

                                <div class="row mb-2">
                                    <div class="form-group">
                                        <label class="form-label">Description</label>
                                        <input type="text" name="description" class="form-control" value=""
                                            placeholder="Description"
                                            maxlength="1000">
                                    </div>
                                </div>
                            </form>`;
                    modal.title.innerText = 'Create New Chapter';
                    modal.footerCloseButton.innerText = 'Cancel';

                    const saveButton = document.createElement('button');
                    saveButton.classList.add('btn', 'btn-primary');
                    saveButton.role = 'button';
                    saveButton.innerText = 'Save';
                    modal.footer.insertAdjacentElement('beforeend', saveButton);

                    saveButton.addEventListener('click', function(event) {
                        const form = modal.modal.querySelector('form');
                        if (!form.checkValidity()) {
                            form.classList.add('was-validated');
                            return;
                        }

                        const title = modal.body.querySelector('input[name="title"]').value;
                        const description = modal.body.querySelector('input[name="description"]').value;

                        PuzzleAPI.createPuzzleChapter({
                            title: title,
                            description: description,
                        }).then(response => {
                            const chapterComp = ChapterComponent.forChapter({
                                id: response.chapter.id,
                                title: response.chapter.title,
                                description: response.chapter.description
                            });
                            chaptersContainer.appendChild(chapterComp.container);
                            showToast({title: 'Success', body: response.message});
                        }).catch(async response => {
                            showToast({title: 'Error', body: (await response).message, colorClass: 'bg-danger'});
                        }).finally(() => {
                            modal.controls.hide();
                            setTimeout(() => modal.modal.remove(), 1000);
                        });
                    });

                    modal.controls.show();
                });

                // Preview Puzzle Modal
                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const puzzleContent = event.target.closest('.puzzle__content');
                    if (puzzleContent === null) {
                        return;
                    }

                    const puzzleComp = PuzzleComponent.fromChild(event.target);
                    const puzzle = puzzleComp.puzzle;

                    const modal = new Modal();
                    modal.title.innerText = puzzle.title;
                    modal.title.classList.add('d-flex', 'align-items-center', 'gap-2');
                    modal.footerCloseButton.innerText = 'Close';
                    modal.modal.dataset.id = puzzle.id;
                    modal.dialog.classList.add('modal-dialog-responsive');
                    modal.body.classList.add('d-flex', 'p-0');

                    const tag = document.createElement('span');
                    tag.innerText = `#\${puzzle.id}`;
                    tag.classList.add('badge', 'bg-secondary');
                    modal.title.appendChild(tag);

                    const iframe = document.createElement('iframe');
                    iframe.src = `\${puzzlePreviewUrl}?previewPuzzleId=\${puzzle.id}`;
                    iframe.style.width = '50rem';
                    iframe.style.height = '700px';
                    modal.body.appendChild(iframe);

                    modal.modal.addEventListener('hidden.bs.modal', () => {
                        setTimeout(() => modal.modal.remove(), 1000);
                    });

                    modal.controls.show();
                });
            }

            function init() {
                initChaptersAndPuzzles();
                initChapterSelects();
                initModals();

                window.addEventListener('beforeunload', function(event) {
                    if (isUnsavedChanges) {
                        event.preventDefault();
                    }
                });

                Sortable.create(chaptersContainer, {
                    animation: 200,
                    group: 'chapters',
                    handle: '.chapter__handle',
                    onMove: function() {
                        isUnsavedChanges = true;
                    }
                });

                for (const puzzlesContainer of [unassignedChapter.puzzlesContainer, archivedChapter.puzzlesContainer]) {
                    Sortable.create(puzzlesContainer, {
                        animation: 200,
                        group: 'puzzles',
                        onMove: function() {
                            isUnsavedChanges = true;
                        }
                    });
                }

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const archiveButton = event.target.closest('.puzzle__button__archive');
                    if (archiveButton === null) {
                        return;
                    }

                    const puzzle = PuzzleComponent.fromChild(event.target);
                    archivedChapter.addPuzzle(puzzle);
                    isUnsavedChanges = true;
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const unassignButton = event.target.closest('.puzzle__button__unassign');
                    if (unassignButton === null) {
                        return;
                    }

                    const puzzle = PuzzleComponent.fromChild(event.target)
                    unassignedChapter.addPuzzle(puzzle);
                    isUnsavedChanges = true;
                });

                const saveButtons = document.querySelectorAll('.button-save');
                for (const saveButton of saveButtons) {
                    saveButton.addEventListener('click', function (event) {
                        for (const btn of saveButtons) {
                            btn.disabled = true;
                            btn.innerText = 'Saving...';
                        }
                        PuzzleAPI.batchUpdatePuzzlePositions(getPuzzlePositions())
                                .then(response => {
                                    isUnsavedChanges = false
                                    showToast({title: 'Success', body: response.message});
                                }).catch(async response => {
                                    showToast({title: 'Error', body: (await response).message, colorClass: 'bg-danger'});
                                    alert('Could not save changes.');
                                }).finally(function() {
                                    unsavedChangesModal.hide();
                                    for (const btn of saveButtons) {
                                        btn.disabled = false;
                                        btn.innerText = 'Save';
                                    }
                                });
                    });
                }

                const unsavedChangesModal = new BootstrapModal(document.getElementById('unsaved-changes-modal'));


                const uploadChapterModal = new BootstrapModal(document.getElementById("upload-chapter-modal"));
                document.getElementById('button-upload-chapters').addEventListener('click', function(event) {
                    if (isUnsavedChanges) {
                        unsavedChangesModal.show();
                    } else {
                        uploadChapterModal.show();
                    }
                });


                const uploadPuzzleModal = new BootstrapModal(document.getElementById("upload-puzzle-modal"));
                const uploadPuzzleTitle = document.querySelector("#upload-puzzle-modal .modal-title");
                const uploadPuzzleChapterId = document.querySelector('#uploadPuzzles input[name="chapterId"]');

                document.getElementById('button-upload-puzzles').addEventListener('click', function(event) {
                    if (isUnsavedChanges) {
                        unsavedChangesModal.show();
                    } else {
                        uploadPuzzleTitle.innerText = 'Upload Puzzles';
                        uploadPuzzleChapterId.value = '';
                        uploadPuzzleModal.show();
                    }
                });
                chaptersContainer.addEventListener('click', function (event) {
                    const uploadButton = event.target.closest('.chapter__button__upload');
                    if (uploadButton === null) {
                        return;
                    }

                    const chapter = ChapterComponent.fromChild(uploadButton).chapter;
                    uploadPuzzleTitle.innerText = `Upload Puzzles to \${chapter.title}`;
                    uploadPuzzleChapterId.value = String(chapter.id);
                    uploadPuzzleModal.show();
                });
            }

            init();
        </script>
    </jsp:body>
</p:main_page>
