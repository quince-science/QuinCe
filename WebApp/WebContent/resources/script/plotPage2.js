/*
 *********************************************
 * Constants
 *********************************************
 */

// DATA CONSTANTS

// PAGE CONSTANTS
var SELECT_ACTION = 1;
var DESELECT_ACTION = 0;
var SELECTION_POINT = -100;

// TABLE CONSTANTS

/*
 *********************************************
 * Page variables
 *********************************************
 */

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

// Selection details
selectedColumn = -1;
var selectedRows = [];

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

//Table selections
var selectedColumn = -1;
var selectedRows = [];

//The row number (in the data file) of the last selected/deselected row, and
//which action was performed.
var lastClickedRow = -1;
var lastClickedAction = DESELECT_ACTION;

/*
 *********************************************
 * Page layout functions
 *********************************************
 */

// Initialise the whole page
function initPage() {
  
  // Draw the basic page layout
  layoutPage();
  
  // Trigger data loading on back end
  PF('pleaseWait').show();
  
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

// Draw all page components
function drawAllContent() {
  console.log('drawAllContent');
}

// Handle table/plot split adjustment 
function scaleTableSplit() {
  tableSplitProportion = $('#plotPageContent').split().position() / $('#plotPageContent').height();
  resizeAllContent();
}

// Handle split adjustment between the two plots
function resizePlots() {
  console.log('Resize plots');
}

// Adjust the size of all page elements after a window
// resize or split adjustment
function resizeAllContent() {
  console.log('resizeAllContent');
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
    drawAllContent();
    drawTable();
    PF('pleaseWait').hide();
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

function clearSelection() {
  selectedColumn = -1;
  selectedRows = [];
  selectionUpdated();
}

function selectionUpdated() {

  drawTableSelection();
  
/*
  // Redraw the plots to show selection
  if (null != plot1) {
    drawPlot(1, false);
  }
  if (null != plot2) {
    drawPlot(2, false);
  }
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

// Get the details of the specified column header
function getColumn(colIndex) {
  
  let result = null;
  
  let currentIndex = 0;
  for (let i = 0; i < columnHeaders.length; i++) {
    let groupHeaders = columnHeaders[i].headings;
    if (currentIndex + groupHeaders.length > colIndex) {
      result = groupHeaders[colIndex - currentIndex];
      break;
    } else {
      currentIndex += groupHeaders.length;
    }
  }
  
  return result;
}


function fillSelectionForm() {
  $('#selectionForm\\:selectedColumn').val(getColumn(selectedColumn).id);
  $('#selectionForm\\:selectedRows').val(selectedRows);
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
  let html = '<table id="dataTable" class="cell-border stripe compact nowrap" width="100%"><thead>';
  
  columnHeaders.forEach(g => {
    
    g.headings.forEach(h => {
      html += '<th ';
      if (h.numeric) {
        html += 'class="dt-head-right"';
      }
      html += '>';
      html += h.heading;
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
      }
      setupTableClickHandlers();
      drawTableSelection();
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
    clickCellAction(this._DT_CellIndex, event.shiftKey);
  })
}

function clickCellAction(cellIndex, shiftClick) {

  let rowId = jsDataTable.row(cellIndex.row).data()['DT_RowId'];
  let columnIndex = cellIndex.column;

  // If the cell isn't selectable, or has no value, do nothing.
  if (canSelectCell(rowId, columnIndex) &&
    null != jsDataTable.cell(cellIndex).data() &&
    null != jsDataTable.cell(cellIndex).data().value &&
    '' != jsDataTable.cell(cellIndex).data().value) {

    if (columnIndex != selectedColumn) {
      selectedColumn = columnIndex;
      selectedRows = [rowId];
      lastClickedRow = rowId;
      lastClickedAction = SELECT_ACTION;
    } else {

      let action = lastClickedAction;
      let actionRows = [rowId];

      if (!shiftClick) {
        if ($.inArray(rowId, selectedRows) != -1) {
          action = DESELECT_ACTION;
        } else {
          action = SELECT_ACTION;
        }
      } else {
        actionRows = getRowsInRange(lastClickedRow, rowId, columnIndex);
      }

      if (action == SELECT_ACTION) {
        addRowsToSelection(actionRows);
      } else {
        removeRowsFromSelection(actionRows);
      }

      lastClickedRow = rowId;
      lastClickedAction = action;
    }

    selectionUpdated();
  }
}

// Highlight the selected table cells
function drawTableSelection() {
  // Clear all selection display
  $(jsDataTable.table().node()).find('.selected').removeClass('selected');

  // Highlight selected cells
  var rows = jsDataTable.rows()[0];
  for (var i = 0; i < rows.length; i++) {
    var row = jsDataTable.row(i);
    var col = jsDataTable.cell({row:i, column:selectedColumn});

    if ($.inArray(row.data()['DT_RowId'], selectedRows) > -1) {
      $(jsDataTable.cell({row : i, column : selectedColumn}).node()).addClass('selected')
    }
  }

  // Update the selection summary
  if (selectedRows.length == 0) {
    $('#selectedColumnDisplay').html('None');
    $('#selectedRowsCountDisplay').html('');
  } else {
    $('#selectedColumnDisplay').html(getColumn(selectedColumn).heading);
    $('#selectedRowsCountDisplay').html(selectedRows.length);
  }
}

function highlightRow(row) {
  console.log('highlightRow');
} 

// Formats for table columns
function getColumnDefs() {

  return [
    {"defaultContent": "",
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

        let classes = []
        if ($.isNumeric(data['value'])) {
          classes.push('numericCol');
        }

        if (!data['used']) {
          classes.push('unused');
        }

        if (null != flagClass) {
          classes.push(flagClass);
        }

        if (data['flagNeeded']) {
          classes.push('needsFlag');
        }
        
        result = '<div class="' + classes.join(' ') + '"';

        if (null != flagClass) {
          result += ' onmouseover="showQCMessage(' + data['qcFlag'] + ', \''+ data['qcMessage'] + '\')" onmouseout="hideQCMessage()"';
        }

        result += '>';
        if (null != data['value']) {
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

function addRowsToSelection(rows) {
  selectedRows = selectedRows.concat(rows).sort((a, b) => a - b);
}

function removeRowsFromSelection(rows) {

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
    var cellData = jsDataTable.cell({row:rowIndex, column:columnIndex}).data();
    if (null != cellData && null != cellData.value && '' != cellData.value) {
      rows.push(rowIDs[currentIndex]);
    }
  }

  if (step == -1) {
    rows = rows.reverse();
  }

  return rows;
}

function canSelectCell(rowID, colIndex) {

  let result = true;

  if (!getColumn(colIndex).editable) {
    result = false;
  }

  return result;
}

