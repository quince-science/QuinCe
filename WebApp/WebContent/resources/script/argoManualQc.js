const PROFILE_LIST_LOADING = 1 << 11;
const PROFILE_INFO_LOADING = 1 << 12;

window['SELECTED_PROFILE_ROW'] = 0;
window['mapSelectionColor'] = '#8582FF';

// Page-specific adjustment of plot height. See plotPage.js:resizePlot
window['plotShrinkHeight'] = 10;

mainTableSpitProportion = 0.65;
plotProfileSplitProportion = 0.4;
profileInfoSplitProportion = 0.7;
profileListSplitProportion = 0.6;

function dataLoadedLocal() {
  initMap(1, true);
  drawProfileListTable();
  
  // Highlight the first row as the current selection
  $($('#profileListTable').DataTable().row(window['SELECTED_PROFILE_ROW']).node()).addClass('selected');
  
  newProfileLoaded();
  
  itemNotLoading(PLOT1_LOADING);
  itemNotLoading(PLOT2_LOADING);
  itemNotLoading(MAP1_LOADING);
  
  // We drew the table ourselves
  return true;
}

function getInitialLoadingItems() {
	return TABLE_LOADING | PLOT1_LOADING | PLOT2_LOADING |
	  MAP1_LOADING | PROFILE_LIST_LOADING | PROFILE_INFO_LOADING;
}

// Lay out the overall page structure
function layoutPage() {

  // Splits the bottom table (in plot_page.xhtml)
  // from everything at the top
  $('#plotPageContent').split({
    orientation: 'horizontal',
    onDragEnd: function() {
      scaleTableSplit();
	}
  });

  $('#plotPageContent').split().position($('#plotPageContent').height() * mainTableSpitProportion);

  // Splits the two profile plots from the profile info
  // (profile info = profile list, map, details)
  $('#plots').split({
    orientation: 'vertical',
    onDragEnd: function() {
      resizeTopSection(true);
    }
  });
  
  $('#plots').split().position($('#plots').width() * plotProfileSplitProportion);

  // Splits the two plots
  $('#profilePlots').split({
    orientation: 'vertical',
    onDragEnd: function() {
      resizePlots(true);
	}
  });

  // Splits the profile map and list from the profile details
  $('#profiles').split({
    orientation: 'horizontal',
    onDragEnd: function() {
      resizeProfileInfo(true);
    }
  });

  // Splits the profile map and profile list
  $('#profileData').split({
    orientation: 'vertical',
    onDragEnd: function() {
      resizeProfileLists(true);
    }
  });
}

// Handle table/plot split adjustment
function scaleTableSplit() {
  tableSplitProportion = $('#plotPageContent').split().position() / $('#plotPageContent').height();
  resizePlots(false);
  resizeProfileInfo(false);
}

function resizeTopSection(updateSplitProportion) {
  if (!updateSplitProportion) {
    $('#plots').split().position($('#plots').width() * plotProfileSplitProportion);
  }
  
  resizePlots(false);
  resizeProfileInfo(false);
  
  if (updateSplitProportion) {
    plotProfileSplitProportion = $('#plots').split().position() / $('#plots').width();
  }
}

function resizeAllContent() {
  $('#plotPageContent').height(window.innerHeight - 73);
  
  // Main data table
  if (null != jsDataTable) {
    let tableHeight = $('#tableContent').height() - 33;
    $('#tableContent .dataTables_scrollBody').css('max-height', tableHeight);
    $('#tableContent .dataTables_scrollBody').css('height', tableHeight);
    jsDataTable.draw();
  }
  
  resizePlots(false);
  resizeProfileInfo(false);
  resizeProfileLists(false);
}

function resizePlots(updateSplitProportion) {
  $('#profilePlots').height('100%');
  
  if (!updateSplitProportion) {
	$('#profilePlots').split().position($('#profilePlots').width() * plotSplitProportion);
  }

  resizePlot(1);
  resizePlot(2);
  
  // Store the split proportion for later
  if (updateSplitProportion) {
    plotSplitProportion = $('#profilePlots').split().position() / $('#profilePlots').width();
  }
}

function resizeProfileInfo(updateSplitProportion) {
  $('#profiles').height('100%');

  if (!updateSplitProportion) {
    $('#profiles').split().position($('#profiles').height() * profileInfoSplitProportion);
  }
  
  if (null != profileDetailsTable) {
    let tableHeight = $('#profileDetails').height();
    $('#profileListTable .dataTables_scrollBody').css('max-height', tableHeight);
    $('#profileListTable .dataTables_scrollBody').css('height', tableHeight);
    profileDetailsTable.draw();
  }
  
  if (updateSplitProportion) {
	profileInfoSplitProportion = $('#profiles').split().position() / $('#profiles').height();
  }
  
  resizeProfileLists(false);
}

function resizeProfileLists(updateSplitProportion) {
  $('#profileMap').height('100%');
  $('#profileList').height('100%');

  if (!updateSplitProportion) {
    $('#profileData').split().position($('#profileData').width() * profileListSplitProportion);
  }
  
  if (null != profileListTable) {
    let tableHeight = $('#profileList').height() - 30;
	$('#profileList .dataTables_scrollBody').css('max-height', tableHeight);
	$('#profileList .dataTables_scrollBody').css('height', tableHeight);
	profileListTable.draw();
  }
  
  if ($('#map1Container').width() != $('#profileMap').width() ||
    $('#map1Container').height() != $('#profileMap').height()) {
		
    $('#map1Container').width($('#profileMap').width());
    $('#map1Container').height($('#profileMap').height());
    initMap(1, false);
  }
  
  if (updateSplitProportion) {
    profileListSplitProportion = $('#profileData').split().position() / $('#profileData').width();
  }
}

function getPlotFormName(index) {
	return '#plot' + index + 'Form';
}

function getMapFormName(index) {
	// There's only one map, so we ignore the index
	return '#profileMapForm';
}

function drawProfileListTable() {

  let tableColumns = [];
  
  let profileColumnList = JSON.parse($('#profileListForm\\:profileListColumns').val());
  
  profileColumnList.forEach(column => {
	tableColumns.push({'title': column});
  });
	
  profileListTable = new DataTable('#profileListTable', {
	ordering: false,
	searching: false,
	paging: false,
	bInfo: false,
	scroller: false,
	scrollY: 400,
    columns : tableColumns,
    data: JSON.parse($('#profileListForm\\:profileListData').val()),
	drawCallback: function (settings) {
	  setupProfileTableClickHandlers();
	},
	rowCallback: function(row, data, index) {
	  if (index % 2 === 0) {
		$(row).addClass('profileListEvenRow');
	  } else {
	    $(row).addClass('profileListOddRow');
	  }
    }
	}
  )
  
  itemNotLoading(PROFILE_LIST_LOADING);
}

// Initialise the click event handlers for the table
function setupProfileTableClickHandlers() {
  // Remove any existing handlers
  $('#profileListTable').off('click', 'tbody tr');

  // Set click handler
  $('#profileListTable').on('click', 'tbody tr', function() {
    selectProfileClick(this);
  })
}

function selectProfileClick(row) {
  itemLoading(PROFILE_INFO_LOADING);
  itemLoading(PLOT1_LOADING);
  itemLoading(PLOT2_LOADING);
  itemLoading(TABLE_LOADING);
  clearSelection();
  $('#profileListForm\\:selectedProfile').val(row._DT_RowIndex);
  selectProfile(); // PF RemoteCommand
}

function newProfileLoaded() {
  // NB For some reason the order of operations here is important
	
  // Update the profile table row highlight
  $($('#profileListTable').DataTable().row(window['SELECTED_PROFILE_ROW']).node()).removeClass('selected');
  window['SELECTED_PROFILE_ROW'] = $('#profileListForm\\:selectedProfile').val();;
  $($('#profileListTable').DataTable().row(window['SELECTED_PROFILE_ROW']).node()).addClass('selected');

  // Redraw the profile details table
  drawProfileDetailsTable();
	
  loadPlot1(); // PF RemoteCommand
  loadPlot2(); // PF RemoteCommand

  // Redraw the main QC table
  drawTable();
  
  // Redraw the map to show the selected cycle
  getMapData(1);
}

function drawProfileDetailsTable() {
  let tableColumns = [];
  let columnList = ['Item', 'Value'];
  
  columnList.forEach(column => {
    tableColumns.push({'title': column});
  });

  profileDetailsTable = new DataTable('#profileDetailsTable', {
    ordering: false,
    searching: false,
    paging: false,
    bInfo: false,
    scrollY: 400,
    columns : tableColumns,
    data: JSON.parse($('#profileDetailsForm\\:profileDetailsData').val()),
    drawCallback: function() {
      $("#profileDetails thead").hide();
    },
    rowCallback: function(row, data, index) {
	  if (index % 2 === 0) {
		$(row).addClass('profileDetailsEvenRow');
      } else {
        $(row).addClass('profileDetailsOddRow');
      }
	}
  });
  
  itemNotLoading(PROFILE_INFO_LOADING);
}

function selectXAxis(index) {
  let xAxis = PF('plot' + index + 'XAxisPicker').input.val(); 
  window['plot' + index + 'XAxisVar'] = xAxis;
  if (xAxis != 0) {
    $(getPlotFormName(index) + '\\:plot' + index + 'XAxis').val(xAxis);
  }
  
  eval('loadPlot' + index + '()');
}

function mapsAllowed() {
  return false;
}

function getStrokeWidth() {
  return 1;
}

function getIsProfilePlot() {
  return true;
}

// Default y axis formatter does nothing
function formatYAxisLabel(value) {
  return value * -1;
}

// Default y axis value formatter does nothing
function formatYAxisValue(value) {
  return value * -1;
}

// Draw axes at zero
function getAxesAtZero() {
  return true;
}

// Always include zero
function getIncludeZero() {
  return true;
}

function legendFormatter(data) {
  return 'PRES: ' + formatYAxisValue(data.series[2].y) + '&nbsp;' + data.dygraph.user_attrs_.xlabel + ': ' + data.x;
}

function mapClick(id) {
  itemLoading(PROFILE_INFO_LOADING);
  itemLoading(PLOT1_LOADING);
  itemLoading(PLOT2_LOADING);
  itemLoading(TABLE_LOADING);
  $('#profileListForm\\:selectedProfile').val(id);
  selectProfile(); // PF remote command
  setTimeout(function() {
    $('#profileList').find('.selected').get(0).scrollIntoView();
  }, 1000);
}

function startUserQcFlags() {
  generateUserQCComments(); // remoteCommand
}

function showFlagDialog() {
  errorCheck();

  if (!getSelectedColumn().badFlagOnly) {
	$('#selectionForm\\:probablyGoodFlag').show();
	$('#selectionForm\\:probablyGoodLabel').show();
    $('#selectionForm\\:badCorrectableFlag').show();
    $('#selectionForm\\:badCorrectableLabel').show();
  } else {
	$('#selectionForm\\:probablyGoodFlag').hide();
	$('#selectionForm\\:probablyGoodLabel').hide();
    $('#selectionForm\\:badCorrectableFlag').hide();
    $('#selectionForm\\:badCorrectableLabel').hide();
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

function qcFlagsAccepted() {
  errorCheck();

  PF('flagDialog').hide();

  drawPlot(1, true, true);
  drawPlot(2, true, true);
  clearSelection();

  // Reload table data
  jsDataTable.ajax.reload(null, false);
  itemNotLoading(UPDATE_DATA);
}

