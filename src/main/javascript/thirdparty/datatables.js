import dataTables from 'datatables.net';
import dataTablesSelect from 'datatables.net-select';
import dataTablesBs5 from 'datatables.net-bs5';

const DataTable = dataTables();
dataTablesSelect();
dataTablesBs5();

window.DataTable = DataTable;
export default DataTable;
