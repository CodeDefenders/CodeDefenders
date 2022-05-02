import jquery from './jquery';
import dataTables from 'datatables.net';
import dataTablesSelect from 'datatables.net-select';
import dataTablesBs5 from 'datatables.net-bs5';


const DataTable = dataTables(window, jquery);
dataTablesSelect(window, jquery);
dataTablesBs5(window, jquery);

window.DataTable = DataTable;


export default DataTable;
