var FLAG_GOOD = 2;
var FLAG_ASSUMED_GOOD = -2;
var FLAG_QUESTIONABLE = 3;
var FLAG_BAD = 4;
var FLAG_FATAL = 44;
var FLAG_NEEDS_FLAG = -10;
var FLAG_IGNORED = -1002;

var SELECT_ACTION = 1;
var DESELECT_ACTION = 0;

var PLOT_POINT_SIZE = 2;
var PLOT_HIGHLIGHT_SIZE = 5;
var PLOT_FLAG_SIZE = 8;

var PLOT_X_AXIS_INDEX = 0;
var PLOT_MEASUREMENT_ID_INDEX = 1;
var PLOT_MANUAL_FLAG_INDEX = 2;
var PLOT_FIRST_Y_INDEX = 3;


var BASE_GRAPH_OPTIONS = {
    drawPoints: true,
    strokeWidth: 0.0,
    labelsUTC: true,
    labelsSeparateLine: true,
    digitsAfterDecimal: 2,
    animatedZooms: true,
    pointSize: PLOT_POINT_SIZE,
    highlightCircleSize: PLOT_HIGHLIGHT_SIZE,
    selectMode: 'euclidian',
    axes: {
      x: {
        drawGrid: false
      },
      y: {
        drawGrid: true,
        gridLinePattern: [1, 3],
        gridLineColor: 'rbg(200, 200, 200)',
      }
    },
    clickCallback: function(e, x, points) {
      scrollToTableRow(getRowId(e, x, points));
    }
};

var VARIABLES_DIALOG_ENTRY_HEIGHT = 35;

//Controller for input updates
var variablesUpdating = false;
var variablesPlotIndex = 1;


//The two plots
var plot1 = null;
var plot2 = null;

//Map variables
var map1 = null;
var map2 = null;

var map1ColorScale = new ColorScale([[0,'#FFFFD4'],[0.25,'#FED98E'],[0.5,'#FE9929'],[0.75,'#D95F0E'],[1,'#993404']]);
map1ColorScale.setFont('Noto Sans', 11);

var map2ColorScale = new ColorScale([[0,'#FFFFD4'],[0.25,'#FED98E'],[0.5,'#FE9929'],[0.75,'#D95F0E'],[1,'#993404']]);
map2ColorScale.setFont('Noto Sans', 11);

var map1DataLayer = null;
var map2DataLayer = null;

var map1Extent = null;
var map2Extent = null;

var redrawMap = true;

var scaleOptions = {
    outliers: 'b',
    outlierSize: 5,
    decimalPlaces: 3
};

var mapSource = new ol.source.Stamen({
  layer: "terrain",
  url: "https://stamen-tiles-{a-d}.a.ssl.fastly.net/terrain/{z}/{x}/{y}.png"
});

//The data table
var jsDataTable = null;

//Table selections
var selectedRows = [];

//The row number (in the data file) of the last selected/deselected row, and
//which action was performed.
var lastClickedRow = -1;
var lastClickedAction = DESELECT_ACTION;

//The callback function for the DataTables drawing call
var dataTableDrawCallback = null;

//Variables for highlighting selected row in table
var tableScrollRow = null;
var scrollEventTimer = null;
var scrollEventTimeLimit = 300;

//Keeps track of the split positions as a percentage of the
//full data area

var resizeEventTimer = null;
var tableSplitProportion = 0.5;

//Page Load function - kicks everything off
$(function() {
  // Make the panel splits
  $('#plotPageContent').split({orientation: 'horizontal', onDragEnd: function(){scaleTableSplit()}});
  tableSplitProportion = 0.5;

  if (typeof start == 'function') {
    start();
  }

  // When the window is resized, scale the panels
  $(window).resize(function() {
    clearTimeout(resizeEventTimer);
    resizeEventTimer = setTimeout(resizeContent, 100);
  });
});

function scaleTableSplit() {
  tableSplitProportion = $('#plotPageContent').split().position() / $('#plotPageContent').height();
  resizeContent();
}

function resizeContent() {
  // Also change in plotPage.css
  $('#plotPageContent').height(window.innerHeight - 73);

  $('#plotPageContent').split().position($('#plotPageContent').height() * tableSplitProportion);

  if (null != jsDataTable) {
    $('.dataTables_scrollBody').height(calcTableScrollY());
  }

  if (typeof resizePlots == 'function') {
    resizePlots();
  }

  if (PrimeFaces.widgets['variableDialog'] && PF('variableDialog').isVisible()) {
    resizeVariablesDialog();
  }
}

function makeJSDates(data) {

  for (var i = 0; i < data.length; i++) {
    // Replace the milliseconds value with a Javascript date
    point_data = data[i];
    point_data[0] = new Date(point_data[0]);
    data[i] = point_data;
  }

  return data;
}

/*
 * Begins the redraw of the data table.
 * The HTML table is initialised (with header only), and
 * the DataTables object is created and configured to load
 * its data from the server using the hidden form.
 */
function drawTable() {
  html = '<table id="dataTable" class="display compact nowrap" cellspacing="0" width="100%"><thead>';

  columnHeadings.forEach(heading => {
    html += '<th>';
    html += heading;
    html += '</th>';
  });

  html += '</thead></table>';

  $('#tableContent').html(html);

  jsDataTable = $('#dataTable').DataTable( {
    ordering: false,
    searching: false,
    serverSide: true,
    scroller: {
      loadingIndicator: true
    },
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

      // Clear the existing table data
      $('#tableForm\\:tableJsonData').val("");

      // Submit the query to the server
      $('#tableForm\\:tableGetData').click();
    },
    bInfo: false,
    drawCallback: function (settings) {
      if (null != tableScrollRow) {
        highlightRow(tableScrollRow);
        tableScrollRow = null;
      }
    },
    rowCallback: function( row, data, index ) {
      if ($.inArray(data[0], selectedRows) > -1) {
        $(row).addClass('selected');
      }

      // For some reason, the click handler is added twice
      // on the first round of data loading (i.e. when the screen
      // is first drawn). I can't find why, so for now I'll just
      // remove all existing click handlers before adding this one.
      $(row).off('click');
      $(row).on('click', function(event) {
        clickRowAction(data[0], event.shiftKey);
      });
    },
    columnDefs: getColumnDefs()
  });

  renderTableColumns();
  resizeContent();
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

/*
 * Calculate the value of the scrollY entry for the data table
 */
function calcTableScrollY() {
  return $('#tableContent').height() - $('#footerToolbar').outerHeight();
 }

function getSelectableRows() {
  return JSON.parse($('#plotPageForm\\:selectableRows').val());
}
/*
 * Called when table data has been downloaded from the server.
 * The previously stored callback function is triggered with
 * the data from the server.
 */
function tableDataDownload(data) {
  var status = data.status;
  if (status == "success") {
    dataTableDrawCallback( {
      draw: $('#tableForm\\:tableDataDraw').val(),
      data: JSON.parse($('#tableForm\\:tableJsonData').val()),
      recordsTotal: $('#tableForm\\:recordCount').val(),
      recordsFiltered: $('#tableForm\\:recordCount').val()
    });
  }
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
  return getPlotData(plotIndex)[pointId][1];
}

/*
 * Scroll to the table row with the given ID
 */
function scrollToTableRow(rowId) {

  var tableRow = -1;

  if (null != rowId) {
    tableRow = JSON.parse($('#plotPageForm\\:tableRowIds').val()).indexOf(rowId);
  }

  if (tableRow >= 0) {
    jsDataTable.scroller().scrollToRow(tableRow - 2);

    // Because we scroll to the row - 2, we know that the
    // row we want to highlight is the third row
    tableScrollRow = tableRow;

    // The highlight is done as part of the table draw callback
  }
}

function highlightRow(tableRow) {
  if (null != tableRow) {
    setTimeout(function() {
      var rowNode = $('#row' + tableRow)[0];
      $(rowNode).css('animationName', 'rowFlash').css('animationDuration', '1s');
      setTimeout(function() {
        $(rowNode).css('animationName', '');
      }, 1000);
    }, 100);
  }
}

/*
 * Process row clicks as selections/deselections
 */
function clickRowAction(rowId, shiftClick) {
  // We only do something if the row is selectable
  if ($.inArray(rowId, getSelectableRows()) != -1) {

    var action = lastClickedAction;
    var actionRows = [rowId];

    if (!shiftClick) {
      if ($.inArray(rowId, selectedRows) != -1) {
        action = DESELECT_ACTION;
      } else {
        action = SELECT_ACTION;
      }
    } else {
      actionRows = getRowsInRange(lastClickedRow, rowId);
    }

    if (action == SELECT_ACTION) {
      addRowsToSelection(actionRows);
    } else {
      removeRowsFromSelection(actionRows);
    }

    selectionUpdated();
    lastClickedRow = rowId;
    lastClickedAction = action;
  }
}

function addRowsToSelection(rows) {
  var rowsIndex = 0;
  var selectionIndex = 0;

  while (selectionIndex < selectedRows.length && rowsIndex < rows.length) {
    while (selectedRows[selectionIndex] > rows[rowsIndex]) {
      selectedRows.splice(selectionIndex, 0, rows[rowsIndex]);
      selectionIndex++;
      rowsIndex++;
    }

    if (selectedRows[selectionIndex] == rows[rowsIndex]) {
      rowsIndex++;
    }
    selectionIndex++;
  }

  if (rowsIndex < rows.length) {
    selectedRows = selectedRows.concat(rows.slice(rowsIndex));
  }
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

function getRowsInRange(startRow, endRow) {

  var rows = [];

  var step = 1;
  if (endRow < startRow) {
    step = -1;
  }

  var startIndex = $.inArray(startRow, getSelectableRows());
  var currentIndex = startIndex;

  while (getSelectableRows()[currentIndex] != endRow) {
    currentIndex = currentIndex + step;
    rows.push(getSelectableRows()[currentIndex]);
  }

  if (step == -1) {
    rows = rows.reverse();
  }

  return rows;
}

function selectionUpdated() {

  // Update the displayed rows
  var rows = jsDataTable.rows()[0];
  for (var i = 0; i < rows.length; i++) {
    var row = jsDataTable.row(i);
    if ($.inArray(row.data()[0], selectedRows) > -1) {
      $(row.node()).addClass('selected');
    } else {
      $(row.node()).removeClass('selected');
    }
  }

  // Update the selected rows counter
  $('#selectedRowsCount').html(selectedRows.length);

  // Redraw the plots to show selection
  if (null != plot1) {
    drawPlot(1);
  }
  if (null != plot2) {
    drawPlot(2);
  }

  if (typeof postSelectionUpdated == 'function') {
    postSelectionUpdated();
  }
}

function clearSelection() {
  jsDataTable.rows(selectedRows).deselect();
  selectedRows = [];
  selectionUpdated();
}

function showInfoPopup(qcFlag, qcMessage, target) {

  $('#infoPopup').stop(true, true);

  if (qcMessage != "") {

    var content = '';
    content += '<div class="qcInfoMessage ';

    switch (qcFlag) {
    case 3: {
      content += 'questionable';
      break;
    }
    case 4:
    case 44: {
      content += 'bad';
      break;
    }
    }

    content += '">';
    content += qcMessage;
    content += '</div>';

    $('#infoPopup')
    .html(content)
    .css({"left": 0, "top": 0})
    .offset({"left": $(target).position().left - $('#infoPopup').width() - 10, "top": $(target).offset().top - 3})
    .show('slide', {direction: 'right'}, 100);
  }
}

function hideInfoPopup() {
  $('#infoPopup').stop(true, true);
  $('#infoPopup').hide('slide', {direction: 'right'}, 100);
}

function drawPlot(index) {

  var plotVar = 'plot' + index;

  // Existing zoom information
  var existingXLabel = null;
  var existingYLabel = null;
  var existingXZoom = null;
  var existingYZoom = null;

  if (null != window[plotVar]) {
    existingXLabel = window[plotVar].getOption('xlabel');
    existingYLabel = window[plotVar].getOption('ylabel');
    existingXZoom = window[plotVar].xAxisRange();
    existingYZoom = window[plotVar].yAxisRange(0);
  }

  // Remove the existing plot
  if (null != window[plotVar]) {
    window[plotVar].destroy();
  }

  var interactionModel = Dygraph.defaultInteractionModel;
  interactionModel.dblclick = null;

  var labels = getPlotLabels(index);
  var xLabel = labels[0];
  var yLabels = labels.slice(3);
  var yLabel = yLabels[0];

  if (yLabels.length > 1) {
    var yIds = JSON.parse($('#plot' + index + 'Form\\:yAxis').val());
    yLabel = getVariableGroup(yIds[0]);
  }

  var graph_options = Object.assign({}, BASE_GRAPH_OPTIONS);
  graph_options.labels = getPlotLabels(index);
  graph_options.xlabel = xLabel;
  graph_options.ylabel = yLabel;
  graph_options.visibility = getPlotVisibility(index);
  graph_options.interactionModel = interactionModel;
  graph_options.width = $('#plot' + index + 'Panel').width();
  graph_options.height = $('#plot' + index + 'Panel').height() - 40;
  graph_options.labelsDiv = 'plot' + index + 'Label';

  if (typeof customiseGraphOptions == 'function') {
    graph_options = customiseGraphOptions(graph_options);
  }


  // Preserve zoom settings where possible
  if (null != existingXZoom && existingXLabel == xLabel) {
    graph_options.dateWindow = existingXZoom;
  }

  if (null != existingYZoom && existingYLabel == yLabel) {
    graph_options.valueRange = existingYZoom;
  }

  // Get the plot data
  var plotData = null;
  // TODO 0 = date - but we need to make it a proper lookup
  if ($('#plot' + index + 'Form\\:xAxis').val() == 0) {
    plotData = makeJSDates(getPlotData(index));
  } else {
    plotData = getPlotData(index);
  }

  var plotHighlights = makeHighlights(index, plotData);
  if (plotHighlights.length > 0) {
    graph_options.underlayCallback = function(canvas, area, g) {
      // Selected
      for (var i = 0; i < plotHighlights.length; i++) {
      var fillStyle = null;

        if (plotHighlights[i][3]) {
          fillStyle = 'rgba(255, 221, 0, 1)';
        } else if (null != plotHighlights[i][2]) {
          fillStyle = plotHighlights[i][2];
        }

        if (null != fillStyle) {
          var xPoint = g.toDomXCoord(plotHighlights[i][0]);
          var yPoint = g.toDomYCoord(plotHighlights[i][1]);
          canvas.fillStyle = fillStyle;
          canvas.beginPath();
          canvas.arc(xPoint, yPoint, PLOT_FLAG_SIZE, 0, 2 * Math.PI, false);
          canvas.fill();
        }
      }
    }
  } else {
    graph_options.underlayCallback = null;
  }

  window[plotVar] = new Dygraph (
      document.getElementById('plot' + index + 'Container'),
      plotData,
      graph_options
  );
}

function getPlotVisibility(index) {

  var labels = getPlotLabels(index);

  var visibility = [];

  for (var i = 1; i < labels.length; i++) {
    switch (labels[i]) {
    case 'ID':
    case 'Manual Flag': {
      visibility.push(false);
      break;
    }
    default: {
      visibility.push(true);
    }
    }
  }

  return visibility;
}

function getPlotLabels(index) {
  return JSON.parse($('#plot' + index + 'Form\\:plotLabels').val());
}

function getPlotData(index) {
  return JSON.parse($('#plot' + index + 'Form\\:plotData').val());
}

function getFlagText(flag) {
  var flagText = "";

  if (flag == '-1001') {
    flagText = 'Needs Flag';
  } else if (flag == '-1002') {
    flagText = 'Ignore';
  } else if (flag == '-2') {
    flagText = 'Assumed Good';
  } else if (flag == '2') {
    flagText = 'Good';
  } else if (flag == '3') {
    flagText = 'Questionable';
  } else if (flag == '4') {
    flagText = 'Bad';
  } else if (flag == '44') {
    flagText = 'Fatal';
  } else {
    flagText = 'Needs Flag';
  }

  return flagText;
}

function getFlagClass(flag) {
  var flagClass = "";

  if (flag == '-1001') {
    flagClass = 'needsFlagging';
  } else if (flag == '-1002') {
    flagClass = 'ignore';
  } else if (flag == '-2') {
    flagClass = 'assumedGood';
  } else if (flag == '2') {
    flagClass = 'good';
  } else if (flag == '3') {
    flagClass = 'questionable';
  } else if (flag == '4' || flag == '44') {
    flagClass = 'bad';
  } else {
    flagClass = 'needsFlagging';
  }

  return flagClass;
}

function showVariableDialog(plotIndex) {

  variablesPlotIndex = plotIndex;

  var mode = getPlotMode(plotIndex);

  if (mode == 'plot') {
    setupPlotVariables(plotIndex);
  } else if (mode == 'map') {
    setupMapVariables(plotIndex);
  }

  PF('variableDialog').show();
  resizeVariablesDialog();
}

function setupPlotVariables(plotIndex) {
  $("[id$=mapVarCheckbox]").hide();
  $("[id$=AxisButton]").show();
  updateXAxisButtons($('#plot' + plotIndex + 'Form\\:xAxis').val());
  populateYAxisButtons(JSON.parse($('#plot' + plotIndex + 'Form\\:yAxis').val()));
}

//Select the specified X Axis in the dialog
function updateXAxisButtons(variable) {

  if (!variablesUpdating) {
    variablesUpdating = true;

    for (var i = 0; i < variableCount; i++) {
      var widget = PrimeFaces.widgets['xAxis-' + i];
      if (null != widget) {
        if (i == variable) {
          widget.check();
        } else {
          widget.uncheck();
        }
      }
    }

    variablesUpdating = false;
  }
}

function populateYAxisButtons(variables) {
  if (!variablesUpdating) {
    variablesUpdating = true;


    for (var i = 0; i < variableCount; i++) {
      var widget = PrimeFaces.widgets['yAxis-' + i];
      if (null != widget) {
        if ($.inArray(i, variables) > -1) {
          widget.check();
        } else {
          widget.uncheck();
        }
      }
    }

    variablesUpdating = false;
  }
}

function updateYAxisButtons(variable) {
  if (!variablesUpdating) {
    variablesUpdating = true;

    var finished = false;
    var index = 0;

    while (!finished) {
      var widget = PrimeFaces.widgets['yAxis-' + index];
      if (null == widget) {
        finished = true;
      } else if (index != variable && !inSameGroup(index, variable)) {
        widget.uncheck();
      }

      index++;
    }

    // If no Y axis is selected, disabled the OK button
    if ($('[id$=yAxisButton_input]:checked').length == 0) {
      PF('variableOk').disable();
    } else {
      PF('variableOk').enable();
    }

    variablesUpdating = false;
  }
}

//See if two ids are in the same variable group
function inSameGroup(id1, id2) {
  var result = false;

  if (id1 == id2) {
    result = true;
  } else {
    for (var i = 0; i < variableGroups.length; i++) {
      var groupMembers = variableGroups[i];
      if ($.inArray(id1, groupMembers) > -1 && $.inArray(id2, groupMembers) > -1) {
        result = true;
      }
    }
  }

  return result;
}

function getVariableGroup(varIndex) {

  var label = '';
  for (var i = 0; i < variableGroups.length; i++) {

    if ($.inArray(varIndex, variableGroups[i]) > -1) {
      label = variableGroupNames[i];
      break
    }
  }

  return label;
}

function setupMapVariables(plotIndex) {
  $("[id$=AxisButton]").hide();
  $("[id$=mapVarCheckbox]").show();
  updateMapCheckboxes($('#plot' + plotIndex + 'Form\\:mapVariable').val());
}

//Select the specified variable in the dialog
function updateMapCheckboxes(variable) {

  if (!variablesUpdating) {
    variablesUpdating = true;

    var finished = false;
    var index = 0;

    while (!finished) {
      var widget = PrimeFaces.widgets['mapVar-' + index];
      if (null == widget) {
        finished = true;
      } else if (index == variable) {
        widget.check();
      } else {
        widget.uncheck();
      }

      index++;
    }

    variablesUpdating = false;
  }
}

function resizeVariablesDialog() {
  var varList = $('#variablesList');
  varList.width(200);

  var maxHeight = $(window).innerHeight() - 200;

  var varsPerColumn = Math.ceil(maxHeight / VARIABLES_DIALOG_ENTRY_HEIGHT);

  var columns = Math.ceil(variableCount / varsPerColumn);

  if (columns == 1 && variableCount < 5) {
    varsPerColumn = variableCount;
  } else if (columns < 2 && variableCount > 5) {
    columns = 2;
    varsPerColumn = Math.ceil(variableCount / 2);
  }

  varList.height(varsPerColumn * VARIABLES_DIALOG_ENTRY_HEIGHT + 30);

  PF('variableDialog').jq.width(varList.prop('scrollWidth') + 50);
  PF('variableDialog').initPosition();

  variablesDialogSized = true;
}

function applyVariables() {
  if (PrimeFaces.widgets['variableDialog']) {
    PF('variableDialog').hide();
  }
  updatePlotInputs(variablesPlotIndex);

  var mode = getPlotMode(variablesPlotIndex);

  if (mode == 'plot') {
    // Clear the current plot data
  $('#plot' + variablesPlotIndex + 'Form\\:plotData').val("");
  $('#plot' + variablesPlotIndex + 'Form\\:mapData').val("");
  $('#plot' + variablesPlotIndex + 'Form\\:plotGetData').click();
  } else if (mode == 'map') {
    initMap(variablesPlotIndex);
  }

}

function updatePlotInputs(plotIndex) {
  var mode = getPlotMode(plotIndex);

  if (mode == 'plot') {
    // If no X Axis is selected, keep the current selection
    var xAxis = getSelectedXAxis();
    if (-1 != xAxis) {
      $('#plot' + plotIndex + 'Form\\:xAxis').val(getSelectedXAxis());
    }

    $('#plot' + plotIndex + 'Form\\:yAxis').val(getSelectedYAxis());
  } else if (mode == 'map') {
    $('#plot' + plotIndex + 'Form\\:mapVariable').val(getSelectedMapVar());
  }
}

function getSelectedXAxis() {

  var result = -1;

  for (var i = 0; i < variableCount; i++) {
    var widget = PrimeFaces.widgets['xAxis-' + i];
    if (null != widget) {
      if (widget.input.prop('checked')) {
        result = i;
        break;
      }
    }
  }

  return result;
}

function getSelectedYAxis() {

  var result = '[';

  for (var i = 0; i < variableCount; i++) {
    var widget = PrimeFaces.widgets['yAxis-' + i];
    if (null != widget && widget.input.prop('checked')) {
      result += i;
      result += ',';
    }
  }

  if (result.length > 1) {
    result = result.substring(0, result.length - 1);
  }
  result += ']';

  return result;
}

function getSelectedMapVar() {

  var result = -1;

  var id = 0;
  var finished = false;

  while (!finished) {
    var widget = PrimeFaces.widgets['mapVar-' + id];
    if (null == widget) {
      finished = true;
    } else {
      if (widget.input.prop('checked')) {
        result = id;
        finished = true;
      }
    }

    id++;
  }

  return result;
}

function updatePlot1(data) {
  if (data.status == "success") {
    updatePlot(1);
  }
}

function updatePlot2(data) {
  if (data.status == "success") {
    updatePlot(2);
  }
}

function updateMap1(data) {
  if (data.status == "success") {
    drawMap(1);
  }
}

function updateMap2(data) {
  if (data.status == "success") {
    drawMap(2);
  }
}

function updatePlot(plotIndex) {

  if (PrimeFaces.widgets['variableDialog']) {
    PF('variableDialog').hide();
  }

  var mode = getPlotMode(plotIndex);

  if (mode == "plot") {
    drawPlot(plotIndex);
  } else {
    initMap(plotIndex);
  }
}

function redrawPlot(index) {
  variablesPlotIndex = index;
  applyVariables();
}

function initMap(index) {

  $('#map' + index + 'Container').width($('#plot' + index + 'Panel').width());
  $('#map' + index + 'Container').height($('#plot' + index + 'Panel').height() - 40);

  var mapVar = 'map' + index;
  var extentVar = mapVar + 'Extent';

  var bounds = JSON.parse($('#plotPageForm\\:dataBounds').val());

  if (null == window[mapVar]) {
    var initialView = new ol.View({
      center: ol.proj.fromLonLat([bounds[4], bounds[5]]),
      zoom: 4,
      minZoom: 2,
    });

    window[mapVar] = new ol.Map({
      target: 'map' + index + 'Container',
      layers: [
        new ol.layer.Tile({
          source: mapSource
        }),
        ],
        controls: [
          new ol.control.Zoom()
          ],
          view: initialView
    });

    window[extentVar] = ol.proj.transformExtent(bounds.slice(0, 4), "EPSG:4326", initialView.getProjection());


    window[mapVar].on('moveend', function(event) {
      mapMoveGetData(event);
    });
    window[mapVar].on('pointermove', function(event) {
      displayMapFeatureInfo(event, window[mapVar].getEventPixel(event.originalEvent));
    });
    window[mapVar].on('click', function(event) {
      mapClick(event, window[mapVar].getEventPixel(event.originalEvent));
    });
  }

  $('#plot' + index + 'Form\\:mapUpdateScale').val(true);
  redrawMap = true;
  getMapData(index);
}


function mapMoveGetData(event) {
  getMapData(getMapIndex(event));
  redrawMap = false;
}

function getMapData(index) {
  var mapVar = 'map' + index;
  var extent = ol.proj.transformExtent(window[mapVar].getView().calculateExtent(), window[mapVar].getView().getProjection(), "EPSG:4326");
  $('#plot' + index + 'Form\\:mapBounds').val('[' + extent + ']');
  $('#plot' + variablesPlotIndex + 'Form\\:plotData').val("");
  $('#plot' + variablesPlotIndex + 'Form\\:mapData').val("");
  $('#plot' + index + 'Form\\:mapGetData').click();
}

function drawMap(index) {
  var mapVar = 'map' + index;
  var dataLayerVar = mapVar + 'DataLayer';
  var colorScaleVar = mapVar + 'ColorScale';

  if (null != window[dataLayerVar]) {
    window[mapVar].removeLayer(window[dataLayerVar]);
    window[dataLayerVar] = null;
  }

  var mapData = JSON.parse($('#plot' + index + 'Form\\:mapData').val());

  var scaleLimits = JSON.parse($('#plot' + index + 'Form\\:mapScaleLimits').val());
  window[colorScaleVar].setValueRange(scaleLimits[0], scaleLimits[1]);

  var layerFeatures = new Array();

  for (var i = 0; i < mapData.length; i++) {
    var featureData = mapData[i];

    var feature = new ol.Feature({
      geometry: new ol.geom.Point([featureData[0], featureData[1]]).transform(ol.proj.get("EPSG:4326"), mapSource.getProjection())
    });

    feature.setStyle(new ol.style.Style({
      image: new ol.style.Circle({
        radius: 5,
        fill: new ol.style.Fill({
          color: window[colorScaleVar].getColor(featureData[5])
        })
      })
    }));

    feature['data'] = featureData;
    feature['tableRow'] = featureData[2];

    layerFeatures.push(feature);
  }

  window[dataLayerVar] = new ol.layer.Vector({
    source: new ol.source.Vector({
      features: layerFeatures
    })
  })

  window[mapVar].addLayer(window[dataLayerVar]);
  window[colorScaleVar].drawScale($('#map' + index + 'Scale'), scaleOptions);

  if (redrawMap) {
    $('#plot' + variablesPlotIndex + 'Form\\:mapUpdateScale').val(false);

    var bounds = JSON.parse($('#plot' + index + 'Form\\:mapBounds').val());
    window['map' + index + 'Extent'] = ol.proj.transformExtent(bounds.slice(0, 4), "EPSG:4326", window[mapVar].getView().getProjection());
    resetZoom(index);
  }

  // Destroy the plot, which is no longer visible
  window['plot' + index] = null;
}

function displayMapFeatureInfo(event, pixel) {
  var index = getMapIndex(event);

  var feature = window['map' + index].forEachFeatureAtPixel(pixel, function(feature) {
    return feature;
  });

  var featureInfo = '';

  if (feature) {
    featureInfo += '<b>Position:</b> '
      featureInfo += feature['data'][0];
    featureInfo += ' ';
    featureInfo += feature['data'][1];
    featureInfo += ' ';
    featureInfo += ' <b>Value:</b> '
      featureInfo += feature['data'][5];
  }

  $('#map' + index + 'Value').html(featureInfo);
}

function mapClick(event, pixel) {
  var index = getMapIndex(event);
  var feature = window['map' + index].forEachFeatureAtPixel(pixel, function(feature) {
    return feature;
  });

  if (feature) {
    scrollToTableRow(feature['data'][3]);
  }
}

function resetZoom(index) {
  var mode = getPlotMode(index)

  if (mode == 'map') {
    var bounds = JSON.parse($('#plotPageForm\\:dataBounds').val());
    window['map' + index + 'Extent'] = ol.proj.transformExtent(bounds.slice(0, 4), "EPSG:4326", window['map' + index].getView().getProjection());
    window['map' + index].getView().fit(window['map' + index + 'Extent'], window['map' + index].getSize());
  } else {
    window['plot' + index].updateOptions({
      dateWindow: null,
      valueRange: null
    });
  }
}

function getMapIndex(event) {
  var containerName = event.target.getTarget();
  return containerName.match(/map([0-9])/)[1];

}

function toggleScale(index) {
  $('#map' + index + 'Scale').toggle(100);
}

function makeHighlights(index, plotData) {
  var highlights = [];

  var currentFlag = FLAG_GOOD;
  var highlightColor = null;

  for (var i = 0; i < plotData.length; i++) {
    var selected = ($.inArray(plotData[i][PLOT_MEASUREMENT_ID_INDEX], selectedRows) > -1);

    if (selected || Math.abs(plotData[i][PLOT_MANUAL_FLAG_INDEX]) != FLAG_GOOD) {

      switch (plotData[i][PLOT_MANUAL_FLAG_INDEX]) {
      case FLAG_BAD:
      case FLAG_FATAL: {
        highlightColor = 'rgba(255, 0, 0, 1)';
        break;
      }
      case FLAG_QUESTIONABLE: {
        highlightColor = 'rgba(216, 177, 0, 1)';
        break;
      }
      case FLAG_NEEDS_FLAG: {
        highlightColor = 'rgba(129, 127, 255, 1)';
        break;
      }
      case FLAG_IGNORED: {
        highlightColor = 'rgba(225, 225, 225, 1)';
        break;
      }
      default: {
        highlightColor = null;
      }
      }

      for (j = PLOT_FIRST_Y_INDEX; j < plotData[i].length; j++) {
        if (plotData[i][j] != null) {
          highlights.push([plotData[i][0], plotData[i][j], highlightColor, selected]);
        }
      }
    }
  }

  return highlights;
}
