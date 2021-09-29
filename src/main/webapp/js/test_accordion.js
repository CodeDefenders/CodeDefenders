/* Wrap in a function to avoid polluting the global scope. */
(function () {

class TestAccordion {

    /**
     * @param {object[]} categories
     *      Given by [JSON.parse('${testAccordion.categoriesAsJSON}')].
     * @param {Map<number, object>} tests
     *      Given by [new Map(JSON.parse('${testAccordion.testsAsJSON}'))].
     */
    constructor (categories, tests) {
        /** The categories of tests to display. */
        this.categories = categories;

        /** Maps test ids to the corresponding tests. */
        this.tests = tests

        /** Maps test ids to modals that show the tests' code. */
        this.testModals = new Map();

        this._init();
    }

    /**
     * Sets up a popover trigger on the given element.
     * @param {HTMLElement} triggerElement A DOM element to be used as the popover trigger.
     * @param {object} data The data of the row, as given by datatables.
     * @param {function} renderTitle A function to render the heading of the popover with.
     * @param {function} renderContent A function to render the body of the popover with.
     */
    _setupPopover (triggerElement, data, renderTitle, renderContent) {
        new bootstrap.Popover(triggerElement, {
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
     * References to created models are cached in a map so they don't need to be generated again.
     * @param {object} test The test DTO to display.
     */
    _viewTestModal (test) {
        let modal = this.testModals.get(test.id);
        if (modal !== undefined) {
            modal.modal('show');
            return;
        }

        modal = $(
                `<div class="modal fade" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-responsive">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Test ${test.id} (by ${test.creator.name})</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <div class="card">
                                    <div class="card-body p-0 codemirror-expand codemirror-test-modal-size">
                                        <pre class="m-0"><textarea></textarea></pre>
                                    </div>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div>`);
        modal.appendTo(document.body);
        this.testModals.set(test.id, modal);

        const textarea = modal.find('textarea').get(0);
        const editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: "text/x-java",
            readOnly: true,
            autoRefresh: true
        });

        CodeDefenders.classes.InfoApi.setTestEditorValue(editor, test.id);
        modal.modal('show');
    };

    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Loop through the categories and create a test table for each one. */
        for (const category of this.categories) {
            const rows = category.testIds
                    .sort()
                    .map(this.tests.get, this.tests);

            /* Create the DataTable. */
            const tableElement = $('#ta-table-' + category.id);
            const dataTable = tableElement.DataTable({
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
    }

    _renderId (data) {
        return `Test ${data.id}`;
    }

    _renderCreator (data) {
        return data.creator.name;
    }

    _renderPoints (data) {
        return `<span class="ta-column-name">Points:</span> ${data.points}`;
    }

    _renderCoveredMutants (data) {
        return `<span class="ta-covered-link"><span class="ta-column-name">Covered:</span> ${data.coveredMutantIds.length}</span>`;
    }

    _renderKilledMutants (data) {
        return `<span class="ta-killed-link"><span class="ta-column-name">Killed:</span> ${data.killedMutantIds.length}</span>`;
    }

    _renderViewButton (data) {
        return data.canView
                ? '<button class="ta-view-button btn btn-xs btn-primary">View</button>'
                : '';
    }

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

    _renderCoveredMutantsPopoverTitle (data) {
        return data.coveredMutantIds.length > 0
                ? 'Covered Mutants'
                : '';
    }

    _renderCoveredMutantsPopoverBody (data) {
        return data.coveredMutantIds.length > 0
                ? data.coveredMutantIds.join(', ')
                : 'No mutants are covered by this test.';
    }

    _renderKilledMutantsPopoverTitle (data) {
        return data.killedMutantIds.length > 0
                ? 'Killed Mutants'
                : '';
    }

    _renderKilledMutantsPopoverBody (data) {
        return data.killedMutantIds.length > 0
                ? data.killedMutantIds.join(', ')
                : 'No mutants were killed by this test.';
    }

    _renderSmellsPopoverTitle (data) {
        return data.smells.length > 0
                ? 'Test Smells'
                : '';
    }

    _renderSmellsPopoverBody (data) {
        return data.smells.length > 0
                ? data.smells.join('<br>')
                : 'This test does not have any smells.'
    }
}

CodeDefenders.classes.TestAccordion = TestAccordion;

})();
