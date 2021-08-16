/*
 *********************************************
 * Controls for the Please Wait notification
 *********************************************
 */
const TABLE_LOADING = 1;
const PLOT1_LOADING = 1 << 1;
const PLOT2_LOADING = 1 << 2;
const MAP1_LOADING = 1 << 3;
const MAP2_LOADING = 1 << 4;
const UPDATE_DATA = 1 << 5;

// Initially the table and both plots are loading
var loadingItems = TABLE_LOADING | PLOT1_LOADING | PLOT2_LOADING;


function plotLoading(index, mode) {
  let item = 0;

  if (index == 1) {
    if (mode == PLOT_MODE_PLOT) {
      item = PLOT1_LOADING;
    } else if (mode == PLOT_MODE_MAP) {
      item = MAP1_LOADING;
    }
  } else if (index == 2) {
    if (mode == PLOT_MODE_PLOT) {
      item = PLOT2_LOADING;
    } else if (mode == PLOT_MODE_MAP) {
      item = MAP2_LOADING;
    }
  }

  itemLoading(item);
}

function itemLoading(item) {
  loadingItems = loadingItems | item;
  PF('pleaseWait').show();
}

function itemNotLoading(item) {
  loadingItems = loadingItems & ~item
  if (loadingItems == 0) {
    PF('pleaseWait').hide();
  }
}

/*
 *********************************************
 * Page variables
 *********************************************
 */

// PAGE CONSTANTS
const SELECT_ACTION = 1;
const DESELECT_ACTION = -1;
const FLAG_FLUSHING = -100;

/*
 * Variables for the plot/table layout
 */

// Timer used to prevent event spamming during page resizes
var resizeEventTimer = null;

// The plot/table ratio
var tableSplitProportion = 0.5;

// The plot split ratio
var plotSplitProportion = 0.5;

/*
 *********************************************
 * General data variables
 *********************************************
 */

// Column Headers for table and plots
var columnHeaders = null;
var extendedColumnHeaders = null;

/*
 *********************************************
 * Table variables
 *********************************************
 */

//The callback function for the DataTables drawing call
var dataTableDrawCallback = null;

//Variables for highlighting selected row in table
var tableScrollRow = null;
var scrollEventTimer = null;
var scrollEventTimeLimit = 300;

/*
 *********************************************
 * Plot variables
 *********************************************
 */

// PLOT CONSTANTS
const DATA_POINT_SIZE = 2;
const DATA_POINT_HIGHLIGHT_SIZE = 5;
const FLAG_POINT_SIZE = 8;
const SELECTION_POINT_SIZE = 8.5;

const VARIABLES_DIALOG_ENTRY_HEIGHT = 35;

const PLOT_MODE_PLOT = 0;
const PLOT_MODE_MAP = 1;

var currentPlot = 1;

// Plot data is passed through form inputs.
// On the page we move them to variables so the plot data isn't
// constantly sent back to the server with ajax requests.
var dataPlot1 = null;
var dataPlot1Data = null;
var flagPlot1 = null;
var flagPlot1Data = null;
var selectionPlot1 = null;

var dataPlot2 = null;
var dataPlot2Data = null;
var flagPlot2 = null;
var flagPlot2Data = null;
var selectionPlot2 = null;

var BASE_PLOT_OPTIONS = {
  drawPoints: true,
  strokeWidth: 0.0,
  labelsUTC: true,
  labelsSeparateLine: true,
  digitsAfterDecimal: 2
}

var updatingDialogButtons = false;

/*
 *********************************************
 * Page layout functions
 *********************************************
 */

// Initialise the whole page
function initPage() {

  PF('pleaseWait').show();

  // When the window is resized, scale the panels
  $(window).resize(function() {
    clearTimeout(resizeEventTimer);
    resizeEventTimer = setTimeout(resizeAllContent, 100);
  });

  // Draw the basic page layout
  layoutPage();

  // Trigger data loading on back end
  // PrimeFaces remoteCommand. Calls dataLoaded() when complete.
  loadData();
}

// Check for an error set in the page form and display a dialog if there is one.
function errorCheck() {

  let errorFound = false;

  if ($('#plotPageForm\\:error').val()) {
   errorFound = true;
   $('#errorMessageString').html($('#plotPageForm\\:error').val());
   PF('errorMessage').show();
  }

  return errorFound;
}

// Lay out the overall page structure
function layoutPage() {
  $('#plotPageContent').split({
    orientation: 'horizontal',
    onDragEnd: function(){
      scaleTableSplit()}
  });

  $('#plots').split({
    orientation: 'vertical',
    onDragEnd: function(){
      resizePlots()}
  });
}

// Handle table/plot split adjustment
function scaleTableSplit() {
  tableSplitProportion = $('#plotPageContent').split().position() / $('#plotPageContent').height();
  resizeAllContent();
}

// Handle split adjustment between the two plots
function resizePlots() {
  resizePlot(1);
  resizePlot(2);

//    if (null != window['map' + index]) {
//      $('#map' + index + 'Container').width($('#plot' + index + 'Panel').width());
//      $('#map' + index + 'Container').height($('#plot' + index + 'Panel').height() - 40);
//      window['map' + index].updateSize();
//    }
}

function resizePlot(index) {
  if (null != window['dataPlot' + index] && null != window['dataPlot' + index].maindiv_) {
    $('#plot' + index + 'Container').width('100%');
    $('#plot' + index + 'Container').height($('#plot' + index + 'Panel').height() - 40);
    window['dataPlot' + index].resize($('#plot' + index + 'Container').width(), $('#plot' + index + 'Container').height());

    if (null != window['flagPlot' + index]) {
      window['flagPlot' + index].resize($('#plot' + index + 'Container').width(), $('#plot' + index + 'Container').height());
    }

    if (null != window['selectionPlot' + index]) {
      window['selectionPlot' + index].resize($('#plot' + index + 'Container').width(), $('#plot' + index + 'Container').height());
    }

    syncZoom(index);
  }
}

// Adjust the size of all page elements after a window
// resize or split adjustment
function resizeAllContent() {
  $('#plotPageContent').height(window.innerHeight - 73);

  $('#plotPageContent').split().position($('#plotPageContent').height() * tableSplitProportion);
  resizePlots();

  if (null != jsDataTable) {
    $('.dataTables_scrollBody').height(calcTableScrollY());
    jsDataTable.draw();
  }

  if (PF('variableDialog').isVisible()) {
    resizeVariablesDialog();
  }
}

function showQCMessage(qcFlag, qcMessage) {

  if (qcMessage != "") {

    let content = '';
    content += '<div class="qcInfoMessage ';

    switch (qcFlag) {
    case 3: {
      content += 'questionable';
      break;
    }
    case 4: {
      content += 'bad';
      break;
    }
    }

    content += '">';
    content += qcMessage;
    content += '</div>';

    $('#qcMessage').html(content);
    $('#qcControls').hide();
    $('#qcMessage').show();
  }
}

function hideQCMessage() {
  $('#qcMessage').hide();
  $('#qcControls').show();
}

/*
 *******************************************************
 * General data functions
 *******************************************************
 */

// Draws the initial page data once loading is complete.
// Called by oncomplete of loadData() PF remoteCommand
function dataLoaded() {

  if (!errorCheck()) {
    columnHeaders = JSON.parse($('#plotPageForm\\:columnHeadings').val());
    extendedColumnHeaders = JSON.parse($('#plotPageForm\\:extendedColumnHeadings').val());
    drawTable();
    initPlot(1);
    initPlot(2);

    if (typeof dataLoadedLocal === "function") {
    dataLoadedLocal();
    }
  }
}

// Get the index of the group that the specified column is in
function getColumnGroup(column) {

  let currentIndex = -1;
  let currentGroup = -1;
  while (currentIndex < column) {
    currentGroup++;
    currentIndex += columnHeaders[currentGroup].headings.length;
  }

  return currentGroup;
}

//Get the details of the specified column header
function getColumn(columnIndex) {

  let result = null;

  let currentIndex = 0;
  for (let i = 0; i < columnHeaders.length; i++) {
    let groupHeaders = columnHeaders[i].headings;
    if (currentIndex + groupHeaders.length > columnIndex) {
      result = groupHeaders[columnIndex - currentIndex];
      break;
    } else {
      currentIndex += groupHeaders.length;
    }
  }

  return result;
}

function getColumnById(columnId) {
  let result = null;

  let currentIndex = 0;
  for (let i = 0; i < extendedColumnHeaders.length; i++) {
    let groupHeaders = extendedColumnHeaders[i].headings;
    for (let j = 0; j < groupHeaders.length; j++) {
      if (groupHeaders[j].id == columnId) {
        result = groupHeaders[j];
        break;
      }
    }
  }

  return result;
}

function getColumnIndex(columnId) {
  return getColumnIndexWork(extendedColumnHeaders, columnId);
}

function getTableColumnIndex(columnId) {
  return getColumnIndexWork(columnHeaders, columnId);
}

function getColumnIndexWork(headers, columnId) {

  let result = null;

  let currentIndex = -1;
  for (let i = 0; i < headers.length; i++) {
    let groupHeaders = headers[i].headings;
    for (let j = 0; j < groupHeaders.length; j++) {
      currentIndex++;
      if (groupHeaders[j].id == columnId) {
        result = currentIndex;
        break;
      }
    }
  }

  return result;
}

function getColumnCount() {
  let count = 0;

  for (let i = 0; i < columnHeaders.length; i++) {
    count += columnHeaders[i].headings.length;
  }

  return count;
}

function getColumnIds() {
  return getColumnIdsWork(columnHeaders);
}

function getExtendedColumnIds() {
  return getColumnIdsWork(extendedColumnHeaders);
}

function getColumnIdsWork(headers) {
  let ids = [];

  for (let i = 0; i < headers.length; i++) {
    let groupHeaders = headers[i].headings;
    for (let j = 0; j < groupHeaders.length; j++) {
      ids.push(groupHeaders[j].id);
    }
  }

  return ids;

}

function getReferenceValue(index) {
  return getColumnById($('#plot' + index + 'Form\\:plot' + index + 'YAxis').val()).referenceValue;
}

function getYRange(index) {
  let result = null;

  let referenceValue = getReferenceValue(index);
  if (null != referenceValue) {
    let min = Number.MAX_VALUE;
    let max = Number.MIN_VALUE;

    $.each(window['dataPlot' + index + 'Data'], function(index, value) {
      if (null != value[2]) {
        if (value[2] < min) {
          min = value[2];
        }
        if (value[2] > max) {
          max = value[2];
        }
      }
      if (null != value[3]) {
        if (value[3] < min) {
          min = value[3];
        }
        if (value[3] > max) {
          max = value[3];
        }
      }
    });

    if (null != referenceValue) {
      if (referenceValue > max) {
        max = referenceValue;
      }
      if (referenceValue < min) {
        min = referenceValue;
      }
    }

    result = [min, max];
  }

  return result;
}

/*
 *******************************************************
 * Selection functions
 *
 * The selection details are maintained via form inputs
 *******************************************************
 */

// Get the selected column ID
function getSelectedColumn() {
  return getColumnById($('#selectionForm\\:selectedColumn').val());
}

// Set the selected column ID
function setSelectedColumn(column) {
  $('#selectionForm\\:selectedColumn').val(column);
}

// Get the selected rows as an array of row IDs
function getSelectedRows() {
  return JSON.parse($('#selectionForm\\:selectedRows').val())
}

// Set the selected rows as an array of row IDs.
// Sorts and uniques (via a Set) the rows before setting them
function setSelectedRows(rows) {
  $('#selectionForm\\:selectedRows').val(
    JSON.stringify([...new Set(rows.sort())])
  );
}

//Get the ID of the clicked row
function getClickedRow() {
  // Get as number
  return +$('#selectionForm\\:clickedRow').val();
}

// Set the ID of the clicked row
function setClickedRow(row) {
  $('#selectionForm\\:clickedRow').val(row);
}

//Get the ID of the previously clicked row
function getPrevClickedRow() {
  // Get as number
  return +$('#selectionForm\\:prevClickedRow').val();
}

// Set the ID of the previously clicked row
function setPrevClickedRow(row) {
  $('#selectionForm\\:prevClickedRow').val(row);
}

// Get the last selection action
function getLastSelectionAction() {
  return $('#selectionForm\\:lastSelectionAction').val();
}

// Set the last selection action
function setLastSelectionAction(action) {
  $('#selectionForm\\:lastSelectionAction').val(action);
}

function addRowsToSelection(rows) {
  setSelectedRows(getSelectedRows().concat(rows));
}

function removeRowsFromSelection(rows) {

  var selectedRows = getSelectedRows();

  var rowsIndex = 0;
  var selectionIndex = 0;

  while (selectionIndex < selectedRows.length && rowsIndex < rows.length) {
    while (selectedRows[selectionIndex] == rows[rowsIndex]) {
      selectedRows.splice(selectionIndex, 1);
      rowsIndex++;
      if (rowsIndex == rows.length || selectionIndex == selectedRows.length) {
        break;
      }
    }
    selectionIndex++;
  }

  setSelectedRows(selectedRows);
}

function clearSelection() {
  setSelectedColumn(-1);
  setSelectedRows([]);
  setPrevClickedRow(-1);
  selectionUpdated();
}

function selectionUpdated() {

  if (canEdit()) {
    drawTableSelection();
    drawSelectionPlot(1);
    drawSelectionPlot(2);

    if (getSelectedRows().length == 0) {
      $('#selectionActions :button').each(function(index, value) {
        $(value).prop('disabled', true).addClass('ui-state-disabled');
      });
    } else {
      $('#selectionActions :button').each(function(index, value) {
        $(value).prop('disabled', false).removeClass('ui-state-disabled');
      });
    }
  }

/*
  if (null != map1) {
    drawMap(1);
  }
  if (null != map2) {
    drawMap(2);
  }

  if (canEdit && typeof postSelectionUpdated == 'function') {
    postSelectionUpdated();
  }
*/
}


/*
 *******************************************************
 * Table functions
 *******************************************************
 */

// Draw the table. The table HTML is generated
// (as a header only) and replaces the current contents of the
// table div. Then the DataTable is constructed with ajax data loading
// and all related formatting/event handling
function drawTable() {

  // Construct the table header
  let html = '<table id="dataTable" class="cell-border compact nowrap" width="100%"><thead>';

  columnHeaders.forEach(g => {

    g.headings.forEach(h => {
      html += '<th ';
      if (h.numeric) {
        html += 'class="dt-head-right"';
      }
      html += '>';
      html += h.shortName;
      html += '</th>';
    });
  });

  html += '</thead></table>';

  // Replace the existing table with the new one
  $('#tableContent').html(html);

  // And initialise the table itself
  jsDataTable = $('#dataTable').DataTable( {
    ordering: false,
    searching: false,
    fixedColumns: {
      leftColumns: columnHeaders[0].headings.length
    },
    serverSide: true,
    scroller: {
      loadingIndicator: true
    },
    scrollX: true,
    scrollY: calcTableScrollY(),
    ajax: function ( data, callback, settings ) {
      // Since we've done a major scroll, disable the short
      // scroll timeout
      clearTimeout(scrollEventTimer);
      scrollEventTimer = null;

      // Store the callback
      dataTableDrawCallback = callback;

      // Fill in the form inputs
      $('#tableForm\\:tableDataDraw').val(data.draw);
      $('#tableForm\\:tableDataStart').val(data.start);
      $('#tableForm\\:tableDataLength').val(data.length);

      // Clear the existing table data to stop it being sent back to the server
      $('#tableForm\\:tableJsonData').val("");

      // Submit the query to the server
      tableGetData(); // PF remoteCommand
    },
    bInfo: false,
    drawCallback: function (settings) {
      if (null != tableScrollRow) {
        highlightRow(tableScrollRow);
        tableScrollRow = null;
        // drawTableSelection is called by highlightRow
      } else {
        drawTableSelection();
      }
      setupTableClickHandlers();
    },
    columnDefs: getColumnDefs()
  });

  resizeAllContent();
  clearSelection();

  // Large table scrolls trigger highlights when the table is redrawn.
  // This handles small scrolls that don't trigger a redraw.
  $('.dataTables_scrollBody').scroll(function() {
    if (null != tableScrollRow) {
      if (scrollEventTimer) {
        clearTimeout(scrollEventTimer);
      }

      scrollEventTimer = setTimeout(function() {
        highlightRow(tableScrollRow);
        tableScrollRow = null;
      }, scrollEventTimeLimit);
    }
  });

  itemNotLoading(TABLE_LOADING);
}

// Calculate the value of the scrollY entry for the data table
function calcTableScrollY() {
  return $('#tableContent').height() - $('#footerToolbar').outerHeight();
 }

// Initialise the click event handlers for the table
function setupTableClickHandlers() {
  // Remove any existing handlers
  $('.dataTable').off('click', 'tbody td');

  // Set click handler
  $('.dataTable').on('click', 'tbody td', function() {
    clickCellAction(this, event.shiftKey);
  })
}

function clickCellAction(cell, shiftClick) {

  let immediateUpdate = true;

  let rowIndex = 0;
  let colIndex = 0;

  // Normal cells have a _DT_CellIndex property.

  // The frozen columns are actually in a separate table drawn over the
  // top of the jsDataTable, and have the ids in the cells' data elements.
  if (cell._DT_CellIndex) {
    rowIndex = cell._DT_CellIndex.row;
    colIndex = cell._DT_CellIndex.column;
  } else {
    rowIndex = $(cell).data().dtRow;
    colIndex = $(cell).data().dtColumn;
  }

  let rowId = jsDataTable.row(rowIndex).data()['DT_RowId'];
  setClickedRow(rowId);

  // If the cell isn't selectable do nothing.
  if (canSelectCell(rowIndex, colIndex)) {

    if (null == getSelectedColumn() || colIndex != getTableColumnIndex(getSelectedColumn().id)) {
      setSelectedColumn(getColumn(colIndex).id);
      setSelectedRows([rowId]);
      setPrevClickedRow(rowId);
      setLastSelectionAction(SELECT_ACTION);
    } else {

      let action = getLastSelectionAction();
      let actionRows = [rowId];

      if (!shiftClick) {
        if ($.inArray(rowId, getSelectedRows()) != -1) {
          action = DESELECT_ACTION;
        } else {
          action = SELECT_ACTION;
        }
      } else {
        // If the previous clicked row is outside the currently loaded
        // table range, we have to process it on the back end
        let loadedRows = jsDataTable.rows().ids();
        if (getPrevClickedRow() < loadedRows[0] ||
            getPrevClickedRow() > loadedRows[loadedRows.length - 1]) {

          immediateUpdate = false;
          selectRange(); // PF RemoteCommand
        } else {
          actionRows = getRowsInRange(getPrevClickedRow(), rowId, colIndex);
        }
      }

      // Only update the selection if it's not being done on the back end
      if (immediateUpdate) {
        if (action == SELECT_ACTION) {
          addRowsToSelection(actionRows);
        } else {
          removeRowsFromSelection(actionRows);
        }

        setPrevClickedRow(rowId);
        setLastSelectionAction(action);
      }
    }

    // Report that the selection is updated, unless it's been sent to the
    // back end for processing
    if (immediateUpdate) {
      selectionUpdated();
    }
  }
}

// Highlight the selected table cells
function drawTableSelection() {

  if (canEdit()) {
    // Clear all selection display. This clears both the main table
    // and the overlaid fixed columns table
    $("td.selected").removeClass('selected');

    // Highlight selected cells
    let selectedRows = getSelectedRows();

    if (selectedRows.length > 0) {

      // Highlight the rows
      let tableColumnIndex = getTableColumnIndex(getSelectedColumn().id);
      let rows = jsDataTable.rows()[0];

      for (let i = 0; i < rows.length; i++) {
        let row = jsDataTable.row(i);

        if ($.inArray(row.data()['DT_RowId'], getSelectedRows()) > -1) {

          if (isFixedColumn(getTableColumnIndex(getSelectedColumn().id))) {
            // Set the overlaid fixed columns table
            $($('[data-dt-row=' + i + '][data-dt-column=' + tableColumnIndex + ']')[0]).addClass('selected');
          } else {
            $(jsDataTable.cell({row : i, column : tableColumnIndex}).node()).addClass('selected');
          }
        }
      }

      $('#selectedColumnDisplay').html(getSelectedColumn().shortName);
      $('#selectedRowsCountDisplay').html(selectedRows.length);
    } else {
      $('#selectedColumnDisplay').html('None');
      $('#selectedRowsCountDisplay').html('');
    }
  }
}

function isFixedColumn(columnIndex) {
  // All columns in the first group are fixed
  return columnIndex < columnHeaders[0].headings.length;
}

// Formats for table columns
function getColumnDefs() {

  return [
    {"defaultContent": "&nbsp;",
      "targets": '_all'
    },
    {"render":
      function (data, type, row, meta) {

        let result = '';

        if (null != data) {
          let flagClass = null;
          switch (data['qcFlag']) {
          case 3: {
            flagClass = 'questionable';
              break;
          }
          case 4: {
            flagClass = 'bad';
              break;
          }
          case -100: {
            flagClass = 'ignore';
          }
        }

        let classes = ['plotPageCell'];

        // Cell coloring
        let columnGroup = getColumnGroup(meta.col);
        if (columnGroup % 2 == 0) {
          classes.push(meta.row % 2 == 0 ? 'evenColGroupEvenRow' : 'evenColGroupOddRow');
        } else {
          classes.push(meta.row % 2 == 0 ? 'oddColGroupEvenRow' : 'oddColGroupOddRow');
        }

        if ($.isNumeric(data['value'])) {
          classes.push('numericCol');
        }

        if (null != flagClass) {
          classes.push(flagClass);
        }

        if (data['type'] == 'I') {
          classes.push('interpolated');
        }

        if (data['flagNeeded']) {
          classes.push('needsFlag');
        }

        result = '<div class="' + classes.join(' ') + '"';

        if (null != flagClass) {
          result += ' onmouseover="showQCMessage(' + data['qcFlag'] + ', \''+ data['qcMessage'] + '\')" onmouseout="hideQCMessage()"';
        }

        result += '>';
        if (null == data['value'] || data['value'] == '') {
        result += '&nbsp;';
        } else {
          result += (data['value']);
        }
        result += '</div>';
        return result;
      }

      },
      "targets": '_all'
    }
  ];
}

/*
 * Called when table data has been downloaded from the server.
 * The previously stored callback function is triggered with
 * the data from the server.
 */
function tableDataDownload() {
  errorCheck();
  dataTableDrawCallback( {
    draw: $('#tableForm\\:tableDataDraw').val(),
    data: JSON.parse($('#tableForm\\:tableJsonData').val()),
    recordsTotal: $('#tableForm\\:recordCount').val(),
    recordsFiltered: $('#tableForm\\:recordCount').val()
  });
}

// Scroll to the specfied column in the table
function scrollToColumn(column) {
  console.log('Scrolling to column ' + column);
}

/*
 * Scroll to the table row with the given ID
 */
function scrollToTableRow(rowId) {

  var tableRow = -1;

  if (null != rowId) {
    tableRow = JSON.parse($('#plotPageForm\\:rowIDs').val()).indexOf(rowId);
  }

  if (tableRow >= 0) {
    jsDataTable.scroller().scrollToRow(tableRow - 2);

    // Because we scroll to the row - 2, we know that the
    // row we want to highlight is the third row
    tableScrollRow = rowId;

    // The highlight is done as part of the table draw callback
  }
}

function highlightRow(rowId) {
  if (null != rowId) {
    setTimeout(function() {
      var rowNode = $('#' + rowId)[0];
      $(rowNode).find('div').css('animationName', 'rowFlash').css('animationDuration', '1s');
      setTimeout(function() {
        $(rowNode).css('animationName', '');
        drawTableSelection();
      }, 1000);
    }, 100);
  }
}

function getRowsInRange(startRow, endRow, columnIndex) {

  let rows = [];

  let step = 1;
  if (endRow < startRow) {
    step = -1;
  }

  let rowIDs = JSON.parse($('#plotPageForm\\:rowIDs').val());

  let startIndex = rowIDs.indexOf(startRow);
  let currentIndex = startIndex;

  while (rowIDs[currentIndex] != endRow) {
    currentIndex = currentIndex + step;

    let rowIndex = jsDataTable.row('#' + rowIDs[currentIndex]).index();

    if (canSelectCell(rowIndex, columnIndex)) {
      rows.push(rowIDs[currentIndex]);
    }
  }

  if (step == -1) {
    rows = rows.reverse();
  }

  return rows;
}

// Determine whether or not a cell can be selected.
// Based on whether:
// * The column is editable
// * The cell contains data
// * The cell's flag is FLUSHING
function canSelectCell(rowIndex, colIndex) {

  let result = true;

  if (!getColumn(colIndex).editable) {
    result = false;
  } else {
    let cellData = jsDataTable.cell(rowIndex, colIndex).data();
    if (null == cellData || null == cellData.value || "" == cellData.value) {
      result = false;
    } else if (cellData.qcFlag == FLAG_FLUSHING || cellData.type == 'I') {
      result = false;
    }
  }

  return result;
}

//******************************************************
//
// Plot functions
//
//******************************************************

function initPlot(index) {
  eval('loadPlot' + index + '()'); // PF remoteCommand
}

function getPlotLabels(index) {
  return JSON.parse($('#plot' + index + 'Form\\:plot' + index + 'DataLabels').val());
}

function drawPlot(index, drawOtherPlots, resetZoom) {
  errorCheck();

  let plotVar = 'dataPlot' + index;

  // If there's new data, extract it
  let newPlotData = $('#plot' + index + 'Form\\:plot' + index + 'Data').val();
  if (newPlotData) {
    window['dataPlot' + index + 'Data'] = parseJsonWithDates(newPlotData);
    $('#plot' + index + 'Form\\:plot' + index + 'Data').val("");
  }

  let labels = getPlotLabels(index);

  var xAxisRange = null;
  var yAxisRange = null;

  if (!resetZoom && null != window[plotVar]) {
    xAxisRange = window[plotVar].xAxisRange();
    yAxisRange = window[plotVar].yAxisRange();
  }

  // Destroy the old plot
  if (null != window[plotVar]) {
    window[plotVar].destroy();
  }

  let data_options = Object.assign({}, BASE_PLOT_OPTIONS);
  // Ghost data and series data colors
  data_options.colors = ['#01752D', '#C0C0C0'];
  data_options.xlabel = labels[0];
  data_options.ylabel = labels[3];
  data_options.labels = labels;
  data_options.labelsDiv = 'plot' + index + 'Label';
  data_options.visibility = [false, true, true];
  data_options.pointSize = DATA_POINT_SIZE;
  data_options.highlightCircleSize = DATA_POINT_HIGHLIGHT_SIZE;
  data_options.selectMode = 'euclidian';
  data_options.animatedZooms = false;
  data_options.xRangePad = 10;
  data_options.yRangePad = 10;

  data_options.axes = {
    x: {
      drawGrid: false
    },
    y: {
      drawGrid: true,
      gridLinePattern: [1, 3],
      gridLineColor: 'rbg(200, 200, 200)',
    }
  };
  data_options.interactionModel = getInteractionModel(index);
  data_options.clickCallback = function(e, x, points) {
    scrollToTableRow(getRowId(e, x, points));
  };
  data_options.zoomCallback = function(xMin, xMax, yRange) {
    syncZoom(index);
  };
  data_options.drawCallback = function(g, initial) {
    resizePlot(index);
  };

  // Reference value
  let referenceValue = getColumnById($('#plot' + index + 'Form\\:plot' + index + 'YAxis').val()).referenceValue;
  if (null != referenceValue) {
    data_options.underlayCallback = function(canvas, area, g) {
      let xmin = g.toDomXCoord(g.xAxisExtremes()[0]);
      let xmax = g.toDomXCoord(g.xAxisExtremes()[1]);
      let ycoord = g.toDomYCoord(referenceValue);

      canvas.setLineDash([10, 5]);
      canvas.strokeStyle = '#FF0000';
      canvas.lineWidth = 3;
      canvas.beginPath();
      canvas.moveTo(xmin, ycoord);
      canvas.lineTo(xmax, ycoord);
      canvas.stroke();
      canvas.setLineDash([]);
    }
  }

  // Zoom
  if (!resetZoom) {
    data_options.dateWindow = xAxisRange;
    data_options.valueRange = yAxisRange;
    data_options.yRangePad = 0;
    data_options.xRangePad = 0;
  } else {
    let nonDefaultYRange = getYRange(index);
    if (null != nonDefaultYRange) {
      data_options.valueRange = getYRange(index);
    }
  }


  window[plotVar] = new Dygraph(
    document.getElementById('plot' + index + 'DataPlot'),
    window['dataPlot' + index + 'Data'],
    data_options
  );

  resizePlot(index);

  if (drawOtherPlots) {
    drawFlagPlot(index);
    drawSelectionPlot(index);
  }

  // Enable/disable the selection mode controls
  if (canEdit()) {
    let plotVariable = $('#plot' + index + 'Form\\:plot' + index + 'YAxis').val();
    if (getColumnById(plotVariable).editable) {
      PF('plot' + index + 'SelectMode').enable();
    } else {
      $('[id^=plot' + index + 'Form\\:plotSelectMode\\:0]').click() // Set to zoom mode
      PF('plot' + index + 'SelectMode').disable();
    }
  }

  // We can't use the window object here because consts don't get put there.
  if (index == 1) {
    itemNotLoading(PLOT1_LOADING);
  } else if (index == 2) {
    itemNotLoading(PLOT2_LOADING);
  }

}

function drawFlagPlot(index) {
  window['flagPlot' + index + 'Data'] =
    parseJsonWithDates($('#plot' + index + 'Form\\:plot' + index + 'Flags').val());

  $('#plot' + index + 'Form\\:plot' + index + 'Flags').val("");

  if (null != window['flagPlot' + index]) {
    window['flagPlot' + index].destroy();
  }

  if (window['flagPlot' + index + 'Data'].length == 0) {
    window['flagPlot' + index] = null;
  } else {
    let flag_options = Object.assign({}, BASE_PLOT_OPTIONS);
    // Flag colors
    flag_options.colors = ['#FF0000', '#FFA42B', '#817FFF'];
    flag_options.xlabel = ' ';
    flag_options.ylabel = ' ';
    flag_options.labels = JSON.parse($('#plot' + index + 'Form\\:plot' + index + 'FlagLabels').val());
    flag_options.pointSize = FLAG_POINT_SIZE;
    flag_options.highlightCircleSize = 0;
    flag_options.selectMode = 'euclidian';
    flag_options.xRangePad = 0;
    flag_options.yRangePad = 0;
    flag_options.axes = {
      x: {
        drawGrid: false
      },
      y: {
        drawGrid: false
      }
    };
    flag_options.axisLabelFontSize = 0;
    flag_options.xAxisHeight = 20;
    flag_options.interactionModel = null;
    flag_options.animatedZooms = false;

    window['flagPlot' + index] = new Dygraph(
      document.getElementById('plot' + index + 'FlagPlot'),
      window['flagPlot' + index + 'Data'],
      flag_options
    );

    resizePlot(index);
  }
}

function drawSelectionPlot(index) {

  if (canEdit()) {
    let plotVar = 'selectionPlot' + index;

    if (null != window[plotVar]) {
      window[plotVar].destroy();
      window[plotVar] = null;
    }

    let selectionData = getSelectionPlotData(index);

    if (selectionData.length > 0) {

      let plotLabels = getPlotLabels(index);

      // Convert X values to dates if required
      if (plotLabels[0] == "Time") {
        for (let i = 0; i < selectionData.length; i++) {
          selectionData[i][0] = new Date(selectionData[i][0]);
        }
      }


      let selection_options = Object.assign({}, BASE_PLOT_OPTIONS);
      selection_options.colors = ['#FFFF00'];
      selection_options.xlabel = ' ';
      selection_options.ylabel = ' ';
      selection_options.labels = [' ', ' '];
      selection_options.pointSize = SELECTION_POINT_SIZE;
      selection_options.highlightCircleSize = 0;
      selection_options.selectMode = 'euclidian';
      selection_options.xRangePad = 0;
      selection_options.yRangePad = 0;
      selection_options.axes = {
        x: {
          drawGrid: false
        },
        y: {
          drawGrid: false
        }
      };
      selection_options.axisLabelFontSize = 0;
      selection_options.xAxisHeight = 20;
      selection_options.interactionModel = null;
      selection_options.animatedZooms = false;

      window[plotVar] = new Dygraph(
        document.getElementById('plot' + index + 'SelectionPlot'),
        selectionData,
        selection_options
      );

      resizePlot(index);
    }
  }
}

function syncZoom(index) {

  let zoomOptions = {
      dateWindow: window['dataPlot' + index].xAxisRange(),
      valueRange: window['dataPlot' + index].yAxisRange(),
      yRangePad: 0,
      xRangePad: 0
    };

  if (null != window['flagPlot' + index]) {
    window['flagPlot' + index].updateOptions(zoomOptions);
  }

  if (null != window['selectionPlot' + index]) {
    window['selectionPlot' + index].updateOptions(zoomOptions);
  }
}


//Get the interaction model for a plot
function getInteractionModel(index) {
  var plot = window['plot' + index];
  var selectMode = $('[id^=plot' + index + 'Form\\:plotSelectMode]:checked').val();

  var interactionModel = null;

  if (selectMode == 'select') {
    interactionModel = {
      mousedown: selectModeMouseDown,
      mouseup: selectModeMouseUp,
      mousemove: selectModeMouseMove
    }
  } else {
  // Use the default interaction model, but without
  // double-click. We use the clickCallback property defined
  // in BASE_GRAPH_OPTIONS above
  interactionModel = Dygraph.defaultInteractionModel;
    interactionModel.dblclick = null;
  }

  return interactionModel;
}

function resetZoom(index) {
  window['dataPlot' + index].updateOptions({
    yRangePad: 10,
    xRangePad: 10
  });

  window['dataPlot' + index].resetZoom();
  let nonDefaultYRange = getYRange(index);
  if (null != nonDefaultYRange) {
    window['dataPlot' + index].updateOptions({
    valueRange: nonDefaultYRange
    });
  }

  syncZoom(index);
}

/*
 * Get the Row ID from a given graph click event
 *
 * For now, this just looks up the row using the X value. This will
 * work for dates, but will need to be more intelligent for non-date plots.
 */
function getRowId(event, xValue, points) {
  var containerId = $(event.target).
  parents().
  filter(function() {
    return this.id.match(/plot[1-2]Container/)
  })[0]['id'];

  var plotIndex = containerId.substring(4, 5);
  var pointId = points[0]['idx'];

  return window['dataPlot' + plotIndex + 'Data'][pointId][1];
}

// Get the data for the selection plot
function getSelectionPlotData(index) {
  let selectionData = [];

  let selectedIds = getSelectedRows();

  let plotDataVar = 'dataPlot' + index + 'Data';

  if (null != window[plotDataVar]) {
    for (let i = 0; i < window[plotDataVar].length; i++) {
      if ($.inArray(window[plotDataVar][i][1], selectedIds) != -1) {
        selectionData.push([window[plotDataVar][i][0], window[plotDataVar][i][3]]);
        if (selectionData.length == selectedIds.length) {
          break;
        }
      }
    }
  }

  return selectionData;
}

// Parse a JSON string, converting any string values to dates.
function parseJsonWithDates(json) {
  return JSON.parse(json, (key, value) => {
    let val = value;
    if (typeof value === 'string') {
      val = new Date(value);
    }
    return val;
  });
}

function showVariableDialog(plotIndex) {

  currentPlot = plotIndex;

  let mode = getPlotMode(plotIndex);

  if (mode == PLOT_MODE_PLOT) {
    setupPlotVariables(plotIndex);
  } else if (mode == PLOT_MODE_MAP) {
    setupMapVariables(plotIndex);
  }

  PF('variableDialog').show();
  resizeVariablesDialog();
}

function setupPlotVariables(plotIndex) {
  getExtendedColumnIds().forEach(id => {
    let xWidget = PrimeFaces.widgets['xAxis-' + id];
    if (xWidget) {
      xWidget.jq.show();
    }

    let yWidget = PrimeFaces.widgets['yAxis-' + id];
    if (yWidget) {
        yWidget.jq.show();
      }

    let mapWidget = PrimeFaces.widgets['mapVar-' + id];
    if (mapWidget) {
      mapWidget.jq.hide();
    }
  });

  updateAxisButtons('x', $('#plot' + plotIndex + 'Form\\:plot' + plotIndex + 'XAxis').val());
  updateAxisButtons('y', $('#plot' + plotIndex + 'Form\\:plot' + plotIndex + 'YAxis').val());
}

//Select the specified axis variable in the dialog
function updateAxisButtons(axis, variable) {


  if (!updatingDialogButtons) {
    updatingDialogButtons = true;

    getExtendedColumnIds().forEach(id => {
      let widget = PrimeFaces.widgets[axis + 'Axis-' + id];

      // Not all variables will have an axis button
      if (widget) {
        if (id == variable) {
          widget.check();
        } else {
          widget.uncheck();
        }
      }
    });

    updatingDialogButtons = false;
  }
}

function setupMapVariables(plotIndex) {
  currentPlot = plotIndex;

  getExtendedColumnIds().forEach(id => {
    let xWidget = PrimeFaces.widgets['xAxis-' + id];
    if (xWidget) {
      xWidget.jq.hide();
    }

    let yWidget = PrimeFaces.widgets['yAxis-' + id];
    if (yWidget) {
        yWidget.jq.hide();
      }

    let mapWidget = PrimeFaces.widgets['mapVar-' + id];
    if (mapWidget) {
      mapWidget.jq.show();
    }
  });
  updateMapCheckboxes(plotIndex, $('#plot' + plotIndex + 'Form\\:mapVariable').val());
}

//Select the specified variable in the dialog
function updateMapCheckboxes(variable) {

  if (!updatingDialogButtons) {
    updatingDialogButtons = true;

    getExtendedColumnIds().forEach(id => {
        let widget = PrimeFaces.widgets['mapVar-' + id];

        // Not all variables will have an axis button
        if (widget) {
          if (id == variable) {
            widget.check();
            $('#plot' + currentPlot + 'Form\\:mapVariable').val(variable);
          } else {
            widget.uncheck();
          }
        }
      });

    updatingDialogButtons = false;
  }
}

function resizeVariablesDialog() {
  let varList = $('#variablesList');
  varList.width(200);

  let maxHeight = $(window).innerHeight() - 200;
  let varsPerColumn = Math.ceil(maxHeight / VARIABLES_DIALOG_ENTRY_HEIGHT);

  let variableCount = getColumnCount();
  let columns = Math.ceil(variableCount / varsPerColumn);

  if (columns == 1 && variableCount < 5) {
    varsPerColumn = variableCount;
  } else if (columns < 2 && variableCount > 5) {
    columns = 2;
    varsPerColumn = Math.ceil(variableCount / 2);
  }

  varList.height(varsPerColumn * VARIABLES_DIALOG_ENTRY_HEIGHT + 30);

  PF('variableDialog').jq.width(varList.prop('scrollWidth') + 50);
  PF('variableDialog').initPosition();
}

function getPlotMode(index) {
  return +$('[id^=plot1Form\\:plotMode]:checked').val();
}

function applyVariables() {
  if (PrimeFaces.widgets['variableDialog']) {
    PF('variableDialog').hide();
  }

  var mode = getPlotMode(currentPlot);

  plotLoading(currentPlot, mode);

  if (mode == PLOT_MODE_PLOT) {
    setPlotAxes(currentPlot);
    initPlot(currentPlot);
  } else if (mode == PLOT_MODE_MAP) {
    eval('map' + currentPlot + 'GetData()'); // PF remoteCommand
    initMap(currentPlot);
  }

}

function getSelectedXAxis() {
  return getSelectedCheckbox('xAxis');
}

function getSelectedYAxis() {
  return getSelectedCheckbox('yAxis');
}

function getSelectedCheckbox(prefix) {

  let axis = 0;

  let columnIds = getExtendedColumnIds();
  for (let i = 0; i < columnIds.length; i++) {
    let widget = PrimeFaces.widgets[prefix + '-' + columnIds[i]];
    if (widget && widget.input[0].checked) {
      axis = columnIds[i];
      break;
    }
  }

  return axis;
}


function setPlotAxes(plotIndex) {
  $('#plot' + plotIndex + 'Form\\:plot' + plotIndex + 'XAxis').val(getSelectedXAxis());
  $('#plot' + plotIndex + 'Form\\:plot' + plotIndex + 'YAxis').val(getSelectedYAxis());
}

function setPlotSelectMode(index) {
  drawPlot(index, false, false);
}

function selectModeMouseDown(event, g, context) {
  context.isZooming = true;
  context.dragStartX = dragGetX(g, event);
  context.dragStartY = dragGetY(g, event);
  context.dragEndX = context.dragStartX;
  context.dragEndY = context.dragStartY;
  context.prevEndX = null;
  context.prevEndY = null;
}

function selectModeMouseMove(event, g, context) {
  if (context.isZooming) {
    context.dragEndX = dragGetX(g, event);
    context.dragEndY = dragGetY(g, event);
    drawSelectRect(g, context);
    context.prevEndX = context.dragEndX;
    context.prevEndY = context.dragEndY;
  }
}

function selectModeMouseUp(event, g, context) {

  g.clearZoomRect_();

  let plotIndex = g.maindiv_.id.substring(4,5);
  let plotVar = $('#plot' + plotIndex + 'Form\\:plot' + plotIndex + 'YAxis').val();

  if (getColumn(getColumnIndex(plotVar)).editable) {
    let minX = g.toDataXCoord(context.dragStartX);
    let maxX = g.toDataXCoord(context.dragEndX);
    if (maxX < minX) {
      minX = maxX;
      maxX = g.toDataXCoord(context.dragStartX);
    }

    let minY = g.toDataYCoord(context.dragStartY);
    let maxY = g.toDataYCoord(context.dragEndY);
    if (maxY < minY) {
      minY = maxY;
      maxY = g.toDataYCoord(context.dragStartY);
    }

    // If we've only moved the mouse by a small amount,
    // interpret it as a click
    let xDragDistance = Math.abs(context.dragEndX - context.dragStartX);
    let yDragDistance = Math.abs(context.dragEndY - context.dragStartY);

    if (xDragDistance <= 3 && yDragDistance <= 3) {
      let closestPoint = g.findClosestPoint(context.dragEndX, context.dragEndY, undefined);
      let pointId = closestPoint.point['idx'];
      let row = window['dataPlot' + plotIndex + 'Data'][pointId][1];
      scrollToTableRow(row);
    } else {
      selectPointsInRect(window['dataPlot' + plotIndex + 'Data'], plotVar, minX, maxX, minY, maxY);
    }
  }
}

function drawSelectRect(graph, context) {
  let ctx = graph.canvas_ctx_;

  if (null != context.prevEndX && null != context.prevEndY) {
    ctx.clearRect(context.dragStartX, context.dragStartY,
      (context.prevEndX - context.dragStartX),
      (context.prevEndY - context.dragStartY))
  }

  ctx.fillStyle = "rgba(128,128,128,0.33)";
  ctx.fillRect(context.dragStartX, context.dragStartY,
    (context.dragEndX - context.dragStartX),
    (context.dragEndY - context.dragStartY))
}

function dragGetX(graph, event) {
  return  event.clientX - graph.canvas_.getBoundingClientRect().left;
}

function dragGetY(graph, event) {
  return event.clientY - graph.canvas_.getBoundingClientRect().top;
}

function selectPointsInRect(data, variableId, minX, maxX, minY, maxY) {
  let pointsToSelect = [];

  for (var i = 0; i < data.length; i++) {
    if (data[i][0] > maxX) {
      break;
    } else if (data[i][0] >= minX) {
      // See if any of the Y values are in range
      for (let y = 2; y < data[i].length; y++) {
        if (null != data[i][y] && data[i][y] >= minY && data[i][y] <= maxY) {
          pointsToSelect.push(data[i][1]);
          break;
        }
      }
    }
  }

  newSelectionColumn = getTrueSelectionColumn(variableId);
  if (null == getSelectedColumn() || newSelectionColumn != getSelectedColumn().id) {
    setSelectedRows(pointsToSelect);
    setSelectedColumn(newSelectionColumn);
  } else {
    addRowsToSelection(pointsToSelect);
  }

  selectionUpdated();
}

function getTrueSelectionColumn(columnId) {
  return getColumnById(columnId).selectionColumn;
}

function canEdit() {
  return $('#plotPageForm\\:canEdit').val() === 'true';
}
