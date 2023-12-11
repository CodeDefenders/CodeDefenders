import DataTable from '../thirdparty/datatables';
import {Popover} from '../thirdparty/bootstrap';
import {InfoApi, LoadingAnimation, Modal} from '../main';


class TestAccordion {

    /**
     * @param {object[]} categories
     *      Given by [JSON.parse('${testAccordion.categoriesAsJSON}')].
     * @param {Map<number, object>} tests
     *      Given by [new Map(JSON.parse('${testAccordion.testsAsJSON}'))].
     * @param {TestEditor | false} cloneTo
     *      The test editor to clone tests to, or false if no clone button should be shown.
     */
    constructor(categories, tests, cloneTo = false) {
        /**
         * The categories of tests to display, i.e. one category per method + all.
         * @type {TestAccordionCategory[]}
         */
        this._categories = categories;

        /**
         * Maps test ids to their test DTO.
         * @type {Map<number, TestDTO>}
         */
        this._tests = tests;

        /**
         * The test editor to clone tests to, or false if no clone button should be shown.
         * @type {TestEditor | false}
         * @private
         */
        this._cloneTo = cloneTo;

        /**
         * Maps test ids to the modal that show the test's code.
         * @type {Map<number, Modal>}
         */
        this._testModals = new Map();

        this._init();
    }

    /**
     * Sets up a popover trigger on the given element.
     * @param {HTMLElement} triggerElement A DOM element to be used as the popover trigger.
     * @param {object} data The data of the row, as given by datatables.
     * @param {function} renderTitle A function to render the heading of the popover with.
     * @param {function} renderContent A function to render the body of the popover with.
     * @private
     */
    _setupPopover (triggerElement, data, renderTitle, renderContent) {
        new Popover(triggerElement, {
            container: document.body,
            template:
                `<div class="popover" role="tooltip">
                    <div class="popover-arrow"></div>
                    <h3 class="popover-header"></h3>
                    <div class="popover-body px-3 py-2" style="max-width: 250px;"></div>
                </div>`,
            placement: 'top',
            trigger: 'hover',
            html: true,
            title: () => renderTitle(data),
            content: () => renderContent(data)
        });
    };

    /**
     * Creates a modal to display the given test and shows it.
     * References to created models are cached so they don't need to be generated again.
     * @param {TestDTO} test The test DTO to display.
     * @private
     */
    async _viewTestModal (test) {
        let modal = this._testModals.get(test.id);
        if (modal !== undefined) {
            modal.controls.show();
            return;
        }

        /* Create a new modal. */
        modal = new Modal();
        modal.title.innerText = `Test ${test.id} (by ${test.creator.name})`;
        modal.body.innerHTML =
                `<div class="card">
                    <div class="card-body p-0 codemirror-expand codemirror-test-modal-size">
                        <pre class="m-0"><textarea></textarea></pre>
                    </div>
                </div>`;
        modal.dialog.classList.add('modal-dialog-responsive');
        modal.body.classList.add('loading', 'loading-bg-gray', 'loading-size-200');
        this._testModals.set(test.id, modal);

        /* Initialize the editor. */
        const textarea = modal.body.querySelector('textarea');
        const editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: 'text/x-java',
            readOnly: true,
            autoRefresh: true
        });
        editor.getWrapperElement().classList.add('codemirror-readonly');

        /* Add clone button */
        if (this._cloneTo) {
            const cloneButton = document.createElement('button');
            cloneButton.classList.add('btn', 'btn-outline-primary');
            cloneButton.innerText = 'Clone to editor';
            cloneButton.addEventListener('click', () => {
                this._cloneTest(editor.getValue());
                modal.controls.hide(); // it feels weird (as if nothing happened) if the modals stays open after clicking the button
            });
            modal.footer.insertBefore(cloneButton, modal.footer.firstChild);
        }

        modal.controls.show();

        await InfoApi.setTestEditorValue(editor, test.id);
        LoadingAnimation.hideAnimation(modal.body);
    };

    /** @private */
    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Loop through the categories and create a test table for each one. */
        for (const category of this._categories) {
            const rows = category.testIds
                    .sort()
                    .map(this._tests.get, this._tests);

            /* Create the DataTable. */
            const tableElement = document.getElementById(`ta-table-${category.id}`);
            const dataTable = new DataTable(tableElement, {
                data: rows,
                columns: [
                    { data: null, title: '', defaultContent: '' },
                    { data: this._renderId.bind(this), title: '' },
                    { data: this._renderCreator.bind(this), title: '' },
                    { data: this._renderCoveredMutants.bind(this), title: '' },
                    { data: this._renderKilledMutants.bind(this), title: '' },
                    { data: this._renderPoints.bind(this), title: '' },
                    { data: this._renderSmells.bind(this), title: '' },
                    { data: this._renderViewButton.bind(this), title: '' }
                ],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {
                    emptyTable: category.id === 'all'
                            ? 'No tests.'
                            : 'No tests cover this method.'
                },
                createdRow: function (row, data, index) {
                    self._setupPopover(
                            row.querySelector('.ta-covered-link'),
                            data,
                            self._renderCoveredMutantsPopoverTitle.bind(self),
                            self._renderCoveredMutantsPopoverBody.bind(self)
                    );

                    self._setupPopover(
                            row.querySelector('.ta-killed-link'),
                            data,
                            self._renderKilledMutantsPopoverTitle.bind(self),
                            self._renderKilledMutantsPopoverBody.bind(self)
                    );

                    self._setupPopover(
                            row.querySelector('.ta-smells-link'),
                            data,
                            self._renderSmellsPopoverTitle.bind(self),
                            self._renderSmellsPopoverBody.bind(self)
                    );

                    const element = row.querySelector('.ta-view-button');
                    if (element !== null) {
                        element.addEventListener('click', function (event) {
                            self._viewTestModal(data);
                        });
                    }
                }
            });
        }

        LoadingAnimation.hideAnimation(document.getElementById('tests-accordion'));
    }

    /** @private */
    _renderId (data) {
        return `Test ${data.id}`;
    }

    /** @private */
    _renderCreator (data) {
        return data.creator.name;
    }

    /** @private */
    _renderPoints (data) {
        return `<span class="ta-column-name">Points:</span> ${data.points}`;
    }

    /** @private */
    _renderCoveredMutants (data) {
        return `<span class="ta-covered-link"><span class="ta-column-name">Covered:</span> ${data.coveredMutantIds.length}</span>`;
    }

    /** @private */
    _renderKilledMutants (data) {
        return `<span class="ta-killed-link"><span class="ta-column-name">Killed:</span> ${data.killedMutantIds.length}</span>`;
    }

    /** @private */
    _renderViewButton (data) {
        return data.canView
                ? '<button class="ta-view-button btn btn-xs btn-primary">View</button>'
                : '';
    }

    /** @private */
    _renderSmells (data) {
        const numSmells = data.smells.length;
        let smellLevel;
        let smellColor;
        if (numSmells >= 3) {
            smellLevel = 'Bad';
            smellColor = 'btn-danger';
        } else if (numSmells >= 1) {
            smellLevel = 'Fishy';
            smellColor = 'btn-warning';
        } else {
            smellLevel = 'Good';
            smellColor = 'btn-success';
        }
        return `<a class="ta-smells-link btn btn-xs ${smellColor}">${smellLevel}</a>`;
    }

    /** @private */
    _renderCoveredMutantsPopoverTitle (data) {
        return data.coveredMutantIds.length > 0
                ? 'Covered Mutants'
                : '';
    }

    /** @private */
    _renderCoveredMutantsPopoverBody (data) {
        return data.coveredMutantIds.length > 0
                ? data.coveredMutantIds.join(', ')
                : 'No mutants are covered by this test.';
    }

    /** @private */
    _renderKilledMutantsPopoverTitle (data) {
        return data.killedMutantIds.length > 0
                ? 'Killed Mutants'
                : '';
    }

    /** @private */
    _renderKilledMutantsPopoverBody (data) {
        return data.killedMutantIds.length > 0
                ? data.killedMutantIds.join(', ')
                : 'No mutants were killed by this test.';
    }

    /** @private */
    _renderSmellsPopoverTitle (data) {
        return data.smells.length > 0
                ? 'Test Smells'
                : '';
    }

    /** @private */
    _renderSmellsPopoverBody (data) {
        return data.smells.length > 0
                ? data.smells.join('<br>')
                : 'This test does not have any smells.'
    }

    /** @private */
    _cloneTest(testcode) {
        const setEditableLines = (a, b) => {
            this._cloneTo.editableLinesStart = a;
            this._cloneTo.editableLinesEnd = b;
        };
        const {editableLinesStart, editableLinesEnd} = this._cloneTo;
        setEditableLines(0, this._cloneTo.initialNumLines);
        this._cloneTo.editor.setValue(testcode);
        setEditableLines(editableLinesStart, editableLinesEnd);
    }
}


export default TestAccordion;
