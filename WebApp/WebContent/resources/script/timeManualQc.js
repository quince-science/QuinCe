// Page-specific adjustment of plot height. See plotPage.js:resizePlot
window['plotShrinkHeight'] = 5;

function acceptAutoQc() {
  submitAutoQC(); // remoteCommand
}

function qcFlagsAccepted() {
  errorCheck();
  updateFlagCounts();

  PF('flagDialog').hide();

  drawPlot(1, true, true);
  drawPlot(2, true, true);
  clearSelection();

  // Reload table data
  jsDataTable.ajax.reload(null, false);
  itemNotLoading(UPDATE_DATA);
}

function startUserQcFlags() {
  generateUserQCComments(); // remoteCommand
}

function showFlagDialog() {
  errorCheck();

  if (!getSelectedColumn().badFlagOnly) {
    $('#selectionForm\\:questionableFlag').show();
    $('#selectionForm\\:questionableLabel').show();
  } else {
    $('#selectionForm\\:questionableFlag').hide();
    $('#selectionForm\\:questionableLabel').hide();
  }

  let woceRowHtml = getSelectedRows().length.toString() + ' row';
  if (getSelectedRows().length > 1) {
    woceRowHtml += 's';
  }
  $('#manualRowCount').html(woceRowHtml);

  updateFlagDialogControls();
  PF('flagDialog').show();
}

function updateFlagDialogControls() {
  var canSubmit = true;

  if (PF('flagMenu').getJQ().find(':checked').val() != 2) {
    if ($('#selectionForm\\:manualComment').val().trim().length == 0) {
      canSubmit = false;
    }
  }

  if (canSubmit) {
    PF('manualCommentOk').enable();
  } else {
    PF('manualCommentOk').disable();
  }
}

function saveManualComment() {
  applyManualFlag(); // remoteCommand
}

function updateFlagCounts() {
  let flagCounts = JSON.parse($('#statusForm\\:neededFlagCounts').val());
  if (null != flagCounts) {
    $('#totalFlagsNeeded').text(flagCounts['-1']);
  }

  for (colId in flagCounts) {
    if (colId != -1) {
      if (flagCounts[colId] == 0) {
        $('#varInfo-' + colId).html('');
      } else {
        $('#varInfo-' + colId).html(flagCounts[colId]);
      }
    }
  }
}

function dataLoadedLocal() {
  initPlot(1);
  initPlot(2);

  updateFlagCounts();
  
  return false;
}

function getInitialLoadingItems() {
	return TABLE_LOADING | PLOT1_LOADING | PLOT2_LOADING
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
  for (let i = 1; i <= 2; i++) {
    resizePlot(i);

    $('#map' + i + 'Container').width($('#plot' + i + 'Panel').width());
    $('#map' + i + 'Container').height($('#plot' + i + 'Panel').height() - 40);
  }
}

// Adjust the size of all page elements after a window
// resize or split adjustment
function resizeAllContent() {
  $('#plotPageContent').height(window.innerHeight - 73);

  $('#plotPageContent').split().position($('#plotPageContent').height() * tableSplitProportion);
  resizePlots();

  if (null != jsDataTable) {
    let tableHeight = calcTableScrollY();
    $('.dataTables_scrollBody').css('max-height',tableHeight);
    $('.dataTables_scrollBody').css('height', tableHeight);
    jsDataTable.draw();
  }

  if (PF('variableDialog').isVisible()) {
    resizeVariablesDialog();
  }
}

function getPlotFormName(index) {
	return '#plot' + index + 'Form';
}

function getMapFormName(index) {
	return getPlotFormName(index);
}

function mapClick(id) {
  scrollToTableRow(id);
}
