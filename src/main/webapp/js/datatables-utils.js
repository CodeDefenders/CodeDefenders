/*
 * Divide two numbers, or return a default value when dividing by zero.
 */
const dtDiv = function (a, b, defaultValue = '0', precision = 2) {
    return b === 0 ? defaultValue : (a / b).toFixed(precision);
}

/*
 * Divide two numbers and format the result as a percentage, or return a default value when dividing by zero.
 */
const dtPercent = function (a, b, defaultValue = '0.0%', precision = 1) {
    return b === 0 ? defaultValue : ((a * 100 / b).toFixed(precision) + '%');
};

/*
 * Return a string containing "a" and the percentage of "(a / b)" in parentheses.
 */
const dtValAndPercent = function(a, b, defaultValue, precision) {
    return a + ' (' + dtPercent(a, b, defaultValue, precision) + ')';
};

/*
 * Set up clickable table cells to toggle child rows in a DataTable.
 *
 * table:           The DataTables object associated with the table
 * format:          The function to generate the child row from it's data
 *
 * The table is supposed to have the following format:
 *
 * <table>
 *     <thead>
 *         <tr>
 *             <th class="toggle-all-details"><i class="toggle-details-icon fa fa-chevron-right text-muted"></i></th>
 *             ...
 *         </tr>
 *     </thead>
 * </table>
 *
 * var table = $('#tableUsers').DataTable({
 *      ...
 *     "columns": [
 *         {
 *             "className":      'toggle-details',
 *             "orderable":      false,
 *             "data":           null,
 *             "defaultContent": '<i class="toggle-details-icon fa fa-chevron-right"></i>'
 *         },
 *         ...
 *     ],
 *     ...
 * });
 *
 * Tables in child rows should have the class "table-child-details".
 */
const setupChildRows = function (table, format) {
    const setIcon = function(toggleDetailsEl, expand) {
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
