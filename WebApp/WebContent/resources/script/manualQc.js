var sensorColums = [];
var calculationColumns = [];
var diagnosticColumns = [];

var plotSplitProportion = 0.5;

function start() {
  /*
   * This is a hack to get round a bug in PrimeFaces.
   * The selection buttons should be disabled up front, but
   * PrimeFaces then drops their onclick handlers. So they're
   * enabled when the page loads, and this will disable them.
   */
  postSelectionUpdated();
  $('#plots').split({orientation: 'vertical', onDragEnd: function(){resizePlots()}});
  plotSplitProportion = 0.5;
  drawPage();
}

function drawPage() {

  // The plot variables are read from the dialog, so to init we must
  // add them to the dialog first
  //TODO This is ugly and must be fixed up

  initPlot(1);
  initPlot(2);

  drawTable();
}

function initPlot(index) {
  var mode = $('[id^=plot' + index + 'Form\\:plotMode]:checked').val();

  var redraw = false;

  if (mode == 'plot') {
    setupPlotVariables(index);
    $('#map' + index + 'ScaleControlContainer').hide();
    $('#map' + index + 'Container').hide();
    $('#plot' + index + 'Container').show();

    if (null == window['plot' + index]) {
      redraw = true;
    }
  } else {
    setupMapVariables(index);
    $('#plot' + index + 'Container').hide();
    $('#map' + index + 'Container').show();
    $('#map' + index + 'ScaleControlContainer').show();

    if (null == window['map' + index]) {
      redraw = true;
    }
  }

  if (redraw) {
    variablesPlotIndex = index;
    applyVariables();
  }
}

function resizePlots() {
  // TODO See if we can make this work stuff out automatically
  // when the plots are stored in plotPage.js
  // See issue #564

  for (var index = 1; index <= 2; index++) {
    if (null != window['plot' + index]) {
      $('#plot' + index + 'Container').width('100%');
      $('#plot' + index + 'Container').height($('#plot' + index + 'Panel').height() - 40);
      window['plot' + index].resize($('#plot' + index + 'Container').width(), $('#plot' + index + 'Container').height());
    }

    if (null != window['map' + index]) {
      $('#map' + index + 'Container').width($('#plot' + index + 'Panel').width());
      $('#map' + index + 'Container').height($('#plot' + index + 'Panel').height() - 40);
      window['map' + index].updateSize();
    }
  }
}

/*
 * Show or hide columns as required.
 */
function renderTableColumns() {
  // ID
  jsDataTable.columns(0).visible(false, false);

  // Message columns are the last and third from last columns
  jsDataTable.columns([jsDataTable.columns()[0].length - 1, jsDataTable.columns()[0].length - 3]).visible(false, false);

  var tableMode = PF('tableModeSelector').getJQ().find(':checked').val();

  jsDataTable.columns(sensorColumns).visible(tableMode == "sensors", false);
  jsDataTable.columns(calculationColumns).visible(tableMode == "calculations", false);
  jsDataTable.columns(diagnosticColumns).visible(tableMode == "diagnostics", false);

  // TODO This is an ugly hack to ensure the final fCO2 is always displayed.
  if (tableMode != "diagnostics") {
    jsDataTable.columns($.inArray("fCO2", columnHeadings)).visible(true, true);
  }

  jsDataTable.columns.adjust();
}

/*
 * Formats etc for table columns
 */
function getColumnDefs() {
  var additionalData = JSON.parse($('#tableForm\\:additionalTableData').val());

  sensorColumns = [];
  var colIndex = 3; //Date/time, lon and lat are the first three columns so skip them
  for (i = 0; i < additionalData.sensorColumnCount; i++) {
    colIndex++;
    sensorColumns.push(colIndex);
  }

  calculationColumns = [];
  for (i = 0; i < additionalData.calculationColumnCount; i++) {
    colIndex++;
    calculationColumns.push(colIndex);
  }

  diagnosticColumns = [];
  for (i = 0; i < additionalData.diagnosticColumnCount; i++) {
    colIndex++;
    diagnosticColumns.push(colIndex);
  }

  var numericCols = $.merge($.merge($.merge([2, 3], sensorColumns), calculationColumns), diagnosticColumns);

  return [
        {"className": "centreCol", "targets": additionalData.flagColumns},
        {"className": "numericCol", "targets": numericCols},
        {"render":
          function (data, type, row) {
            return makeUTCDateTime(new Date(data));
          },
          "targets": 1
        },
        {"render":
          function (data, type, row) {
            return (null == data ? null : data.toFixed(3));
          },
          "targets": numericCols
        },
        {"render":
          function (data, type, row) {
                var output = '<div onmouseover="showInfoPopup(' + row[additionalData.flagColumns[0]] + ', \'' + row[additionalData.flagColumns[0] + 1] + '\', this)" onmouseout="hideInfoPopup()" class="';
                output += getFlagClass(data);
                output += '">';
                output += getFlagText(data);
                output += '</div>';
                return output;
            },
            "targets": additionalData.flagColumns[0]
        },
        {"render":
          function (data, type, row) {
            var output = '<div class="';
            output += getFlagClass(data);
            output += '">';
        output += getFlagText(data);
        output += '</div>';
        return output;
            },
            "targets": additionalData.flagColumns[1]
        }
  ];
}

function postSelectionUpdated() {
  if (selectedRows.length == 0) {
    PF('acceptQcButton').disable();
    PF('overrideQcButton').disable();
  } else {
    PF('acceptQcButton').enable();
    PF('overrideQcButton').enable();
  }
}

function updateFlagDialogControls() {
  var canSubmit = true;

  if (PF('flagMenu').input.val() != 2) {
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

function acceptAutoQc() {
  $('#selectionForm\\:selectedRows').val(selectedRows);
  $('#selectionForm\\:acceptAutoQc').click();
}

function qcFlagsAccepted() {
  var additionalData = JSON.parse($('#tableForm\\:additionalTableData').val());

  var autoFlagColumn = additionalData.flagColumns[0];
  var autoMessageColumn = autoFlagColumn + 1;
  var userFlagColumn= additionalData.flagColumns[1];
  var userMessageColumn = userFlagColumn + 1;

  var rows = jsDataTable.rows()[0];
  for (var i = 0; i < rows.length; i++) {
    var row = jsDataTable.row(i);
    if ($.inArray(row.data()[0], selectedRows) > -1) {
      jsDataTable.cell(i, userMessageColumn).data(jsDataTable.cell(row, autoMessageColumn).data());
      jsDataTable.cell(i, userFlagColumn).data(jsDataTable.cell(row, autoFlagColumn).data());
    }
  }

  clearSelection();

  // Reload the plots
  $('#plot1Form\\:plotGetData').click();
  $('#plot2Form\\:plotGetData').click();
}

function startUserQcFlags() {
  $('#selectionForm\\:selectedRows').val(selectedRows);
  $('#selectionForm\\:generateUserQcComments').click();
}

function showFlagDialog() {
  var woceRowHtml = selectedRows.length.toString() + ' row';
  if (selectedRows.length > 1) {
    woceRowHtml += 's';
  }
  $('#manualRowCount').html(woceRowHtml);

    var commentsString = '';
    var comments = JSON.parse($('#selectionForm\\:userCommentList').val());
    for (var i = 0; i < comments.length; i++) {
      var comment = comments[i];
      commentsString += comment[0];
      commentsString += ' (' + comment[2] + ')';
      if (i < comments.length - 1) {
        commentsString += '\n';
      }
    }
    $('#selectionForm\\:manualComment').val(commentsString);

    PF('flagMenu').selectValue($('#selectionForm\\:worstSelectedFlag').val());
    updateFlagDialogControls();
    PF('flagDialog').show();
}

function saveManualComment() {
  $('#selectionForm\\:selectedRows').val(selectedRows);
  $('#selectionForm\\:applyManualFlag').click();
  PF('flagDialog').hide();
}

function manualFlagsUpdated() {

  var additionalData = JSON.parse($('#tableForm\\:additionalTableData').val());

  var userFlagColumn= additionalData.flagColumns[1];
  var userMessageColumn = userFlagColumn + 1;

  var rows = jsDataTable.rows()[0];
  for (var i = 0; i < rows.length; i++) {
    var row = jsDataTable.row(i);
    if ($.inArray(row.data()[0], selectedRows) > -1) {
      jsDataTable.cell(i, userMessageColumn).data($('#selectionForm\\:manualComment').val());
      jsDataTable.cell(i, userFlagColumn).data(PF('flagMenu').input.val());
    }
  }

  clearSelection();

  // Reload the plots
  $('#plot1Form\\:plotGetData').click();
  $('#plot2Form\\:plotGetData').click();
}

function getPlotMode(index) {
  return $('[id^=plot' + index + 'Form\\:plotMode]:checked').val();
}
