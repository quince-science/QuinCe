var sensorColums = [];
var calculationColumns = [];
var diagnosticColumns = [];

var plotSplitProportion = 0.5;

// Stepped range calculator
const range = (start, stop, step = 1) =>
  Array(Math.ceil((stop - start) / step)).fill(start).map((x, y) => x + y * step)

function start() {
  /*
   * This is a hack to get round a bug in PrimeFaces.
   * The selection buttons should be disabled up front, but
   * PrimeFaces then drops their onclick handlers. So they're
   * enabled when the page loads, and this will disable them.
   */
  if (canEdit) {
    postSelectionUpdated();
  }
  $('#plots').split({orientation: 'vertical', onDragEnd: function(){resizePlots()}});
  plotSplitProportion = 0.5;
  drawPage();
}

function drawPage() {

  // The plot variables are read from the dialog, so to init we must
  // add them to the dialog first
  //TODO This is ugly and must be fixed up

  //initPlot(1);
  //initPlot(2);

  drawTable();
}

function initPlot(index) {
  var mode = $('[id^=plot' + index + 'Form\\:plotMode]:checked').val();

  var redraw = false;

  if (mode == 'plot') {
    setupPlotVariables(index);
    $('#map' + index + 'ScaleControlContainer').hide();
    $('#map' + index + 'Scale').hide();
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
    if (window['map' + index + 'ScaleVisible']) {
      $('#map' + index + 'Scale').show();
    }

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
 * Formats etc for table columns
 */
function getColumnDefs() {

  var columns = JSON.parse($('#plotPageForm\\:columnHeadings').val())

  var dateCol = [0];
  var valueCols = range(1, columns.length, 5);

  return [
    {"render":
      function (data, type, row) {
        return makeUTCDateTime(new Date(data));
      },
      "targets": dateCol
    },
    {"render":
      function (data, type, row) {

      var col = arguments[3]['col'];

      var used = row[col + 1];

      var flagClass = null;
      switch (row[col + 2]) {
      case 3: {
        flagClass = 'questionable';
          break;
      }
      case 4: {
        flagClass = 'bad';
          break;
      }
      }

      var needsFlag = row[col + 3];

      var classes = ['numericCol'];
      if (!used) {
        classes.push('unused');
      }

      if (null != flagClass) {
        classes.push(flagClass);
      }

      if (needsFlag) {
        classes.push('needsFlag');
      }

      var result = '<div class="' + classes.join(' ') + '"';

      if (null != flagClass) {
        result += ' onmouseover="showQCMessage(' + row[col + 2] + ', \''+ row[col + 4] + '\')" onmouseout="hideQCMessage()"';
      }

      result += '>' + (null == data ? "" : data.toFixed(3)) + '</div>';
      return result;
      },
      "targets": valueCols
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
