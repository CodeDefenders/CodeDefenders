/*
 * Divide two numbers, or return a default value when dividing by zero.
 */
function dtDiv(a, b, defaultValue, precision) {
    if (typeof defaultValue === 'undefined') defaultValue = 0;
    if (typeof precision === 'undefined') precision = 2;

    return b === 0 ? defaultValue : (a / b).toFixed(precision);
}

/*
 * Divide two numbers and format the result as a percentage, or return a default value when dividing by zero.
 */
function dtPerc(a, b, defaultValue, precision) {
    if (typeof defaultValue === 'undefined') defaultValue = '0%';
    if (typeof precision === 'undefined') precision = 1;

    return b === 0 ? defaultValue : ((a * 100 / b).toFixed(precision) + '%');
}

/*
 * Return a string containing "a" and the percentage of "(a / b)" in parentheses.
 */
function dtValAndPerc(a, b, defaultValue, precision) {
    return a + ' (' + dtPerc(a, b, defaultValue, precision) + ')';
}

/*
 * Set up clickable table cells to toggle child rows in a DataTable.
 *
 * tableSelector:   A selector with which the table can be selected, e.g. "#tableUsers"
 * table:           The DataTables object associated with the table
 * format:          The function to generate the child row from it's data
 *
 * The table is supposed to have the following format:
 *
 * <table>
 *     <thead>
 *         <tr>
 *             <th id="toggle-all-details"><span class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"></span></th>
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
 *             "defaultContent": '<span class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"></span>'
 *         },
 *         ...
 *     ],
 *     ...
 * });
 *
 * Tables in child rows should have the class "table-child-details".
 */
function setupChildRows(tableSelector, table, format) {
    $(tableSelector + ' tbody').on('click', '.toggle-details', function () {
        var tr = $(this).closest('tr');
        var row = table.row(tr);

        /* Toggle the child of the row. */
        if (row.child.isShown()) {
            row.child.hide();
            tr.removeClass('shown');
            $(this).find(".toggle-details-icon").removeClass('glyphicon-chevron-down').addClass('glyphicon-chevron-right');
        } else {
            row.child(format(row.data())).show();
            tr.addClass('shown');
            $(this).find(".toggle-details-icon").removeClass('glyphicon-chevron-right').addClass('glyphicon-chevron-down');
        }
    });

    $('#toggle-all-details').on('click', function () {
        $(this).toggleClass('shown');
        var shown = $(this).hasClass('shown');

        $(this).find(".toggle-details-icon").toggleClass('glyphicon-chevron-right').toggleClass('glyphicon-chevron-down');

        /* Show or hide all children of rows on this page. */
        $(tableSelector + ' tbody .toggle-details').each(function() {
            var tr = $(this).closest('tr');
            var row = table.row(tr);

            if (row.child.isShown() && !shown) {
                row.child.hide();
                tr.removeClass('shown');
                $(this).find(".toggle-details-icon").removeClass('glyphicon-chevron-down').addClass('glyphicon-chevron-right');
            } else if (!row.child.isShown() && shown){
                row.child(format(row.data())).show();
                tr.addClass('shown');
                $(this).find(".toggle-details-icon").removeClass('glyphicon-chevron-right').addClass('glyphicon-chevron-down');
            }
        });
    });
};
