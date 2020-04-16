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

// Draws the initial page data once loading is complete.
// Called by oncomplete of loadData() PF remoteCommand
function dataLoaded() {
  
  if (!errorCheck()) {
    drawAllContent();
    drawTable();
    PF('pleaseWait').hide();
  }
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
  
}

// Handle table/plot split adjustment 
function scaleTableSplit() {
  tableSplitProportion = $('#plotPageContent').split().position() / $('#plotPageContent').height();
  resizeAllContent();
}

// Handle split adjustment between the two plots
function resizePlots() {
  
}

// Adjust the size of all page elements after a window
// resize or split adjustment
function resizeAllContent() {
  
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
  let html = '<table id="dataTable" class="display compact nowrap" cellspacing="0" width="100%"><thead>';
  
  let headingGroups = JSON.parse($('#plotPageForm\\:columnHeadings').val());

  headingGroups.forEach(g => {
    
    g.headings.forEach(h => {
      html += '<th>';
      html += h;
      html += '</th>';

      html += '<th>';
      html += h + QCFLAG_COL_SUFFIX;
      html += '</th>';

      html += '<th>';
      html += h + QCMESSAGE_COL_SUFFIX;
      html += '</th>';
    });
  });

  html += '</thead></table>';
  
  // Replace the existing table with the new one
  $('#tableContent').html(html);
}