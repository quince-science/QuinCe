/*
 *********************************************
 * Constants
 *********************************************
 */

// DATA CONSTANTS
const QCFLAG_COL_SUFFIX = '_FLAG';
const QCMESSAGE_COL_SUFFIX = '_MESSAGE';

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
var columnHeaders = null;

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

    var content = '';
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
}

// Calculate the value of the scrollY entry for the data table
function calcTableScrollY() {
  return $('#tableContent').height() - $('#footerToolbar').outerHeight();
 }

// Initialise the click event handlers for the table
function setupTableClickHandlers() {
  console.log('setupTableClickHandlers');
}

// Highlight the selected table cells
function drawTableSelection() {
  console.log('drawTableSelection');
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

        var classes = []
        if ($.isNumeric(data['value'])) {
          classes.push('numericCol');
        }

        if (!data['used']) {
          classes.push('unused');
        }

        if (null != flagClass) {
          classes.push(flagClass);
        }

        if (data['needsFlag']) {
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