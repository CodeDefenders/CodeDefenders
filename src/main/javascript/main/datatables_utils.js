class DataTablesUtils {
    /**
     * Divide two numbers and format the result.
     * @param {number} dividend
     * @param {number} divisor
     * @param {string=} defaultValue A default string that is returned in case of a division-by-zero.
     * @param {number=} precision The precision used for formatting the number.
     */
    static formatDivision (dividend, divisor, defaultValue = '0', precision = 2) {
        return divisor === 0 ? defaultValue : (dividend / divisor).toFixed(precision);
    }

    /**
     * Divide two numbers and format the result as a percentage.
     * @param {number} part
     * @param {number} whole
     * @param {string=} defaultValue A default string that is returned in case of a division-by-zero.
     * @param {number=} precision The precision used for formatting the percentage.
     */
    static formatPercent (part, whole, defaultValue = '0.0%', precision = 1) {
        return whole === 0 ? defaultValue : ((part * 100 / whole).toFixed(precision) + '%');
    }

    /**
     * Return a string containing both the given number and the numbers's percentage of a given whole.
     * @param {number} part
     * @param {number} whole
     * @param {string=} defaultValue A default string that is returned in case of a division-by-zero.
     * @param {number=} precision The precision used for formatting the percentage.
     */
    static formatValueAndPercent (part, whole, defaultValue, precision) {
        return `${part} (${DataTablesUtils.formatPercent(part, whole, defaultValue, precision)})`;
    }

    /**
     * <p>Set up clickable table cells to toggle child rows in a DataTable.</p>
     *
     * <p>The table is supposed to have the following format:</p>
     *
     * <pre>
     * <table>
     *     <thead>
     *         <tr>
     *             <th class="toggle-all-details"><i class="toggle-details-icon fa fa-chevron-right text-muted"></i></th>
     *             ...
     *         </tr>
     *     </thead>
     * </table>
     *
     * table = new DataTable(..., {
     *     columns: [
     *         {
     *             className:       'toggle-details',
     *             orderable:       false,
     *             data:            null,
     *             defaultContent:  '<i class="toggle-details-icon fa fa-chevron-right"></i>'
     *         },
     *         ...
     *     ],
     *     ...
     * });
     * </pre>
     *
     * <p>
     *     Tables with child rows similar to those in admin analytics tables can use the CSS class ".child-row-details".
     * </p>
     *
     * @param {DataTable} table The DataTables object for the table.
     * @param {function} format A function that generates a child row from it's data.
     */
    static setupChildRows (table, format) {
        const setIcon = function (toggleDetailsEl, expand) {
            if (expand) {
                toggleDetailsEl.querySelector('.toggle-details-icon')
                        .classList
                        .replace('fa-chevron-right', 'fa-chevron-down');
            } else {
                toggleDetailsEl.querySelector('.toggle-details-icon')
                        .classList
                        .replace('fa-chevron-down', 'fa-chevron-right');
            }
        };

        table.table().container()
                .querySelector('tbody')
                .addEventListener('click', function (event) {

                    const toggleDetailsEl = event.target.closest('.toggle-details');
                    if (toggleDetailsEl === null) {
                        return;
                    }

                    const tr = toggleDetailsEl.closest('tr');
                    const row = table.row(tr);

                    /* Toggle the child row. */
                    if (row.child.isShown()) {
                        row.child.hide();
                        setIcon(toggleDetailsEl, false);
                    } else {
                        row.child(format(row.data())).show();
                        setIcon(toggleDetailsEl, true);
                    }
                });

        table.table().container()
                .querySelector('.toggle-all-details')
                .addEventListener('click', function (event) {

                    const expandedBefore = JSON.parse(this.dataset.expanded ?? false);
                    this.dataset.expanded = !expandedBefore;
                    setIcon(this, !expandedBefore);

                    /* Show or hide all children of rows in this table. */
                    for (const toggleDetailsEl of table.table().container()
                            .querySelectorAll('tbody .toggle-details')) {

                        const tr = toggleDetailsEl.closest('tr');
                        const row = table.row(tr);

                        /* Toggle the child row. */
                        if (expandedBefore) {
                            row.child.hide();
                            setIcon(toggleDetailsEl, false);
                        } else {
                            row.child(format(row.data())).show();
                            setIcon(toggleDetailsEl, true);
                        }
                    }
                });
    };

    /**
     * Returns the selected entries from a data table with the select extension enabled.
     * @param {DataTable} table The table.
     * @return {object[]} The data entries of the selected rows.
     */
    static getSelected (table) {
        const entries = [];
        table.rows({selected: true}).every(function () {
            const data = this.data();
            entries.push(data);
        });
        return entries;
    };

    /**
     * Registers a sorting method that sort DataTables rows by whether they are selected by the select extension.
     * The sort method has to specified for the column like
     * {
     *   name: ...,
     *   data: ...,
     *   orderDataType: 'select-extension'
     * }
     */
    static registerSortBySelected () {
        DataTable.ext.order['select-extension'] = function (settings, col) {
            return this.api().column(col, {order: 'index'}).nodes().map(function (td, i) {
                const tr = td.closest('tr');
                return tr.classList.contains('selected') ? '0' : '1';
            });
        };
    }
}


export default DataTablesUtils;
