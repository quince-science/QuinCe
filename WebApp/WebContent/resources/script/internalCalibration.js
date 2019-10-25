
function start() {
  drawPage();
}

function drawPage() {
  initPlot();
  drawTable();
}

function initPlot(index) {
  setupPlotVariables(1);
  variablesPlotIndex = 1;
  applyVariables();
}

/*
 * Show or hide columns as required.
 */
function renderTableColumns() {
  jsDataTable.columns(0).visible(false, false);
  jsDataTable.columns(7).visible(false, false);
}

/*
 * Formats etc for table columns
 */
function getColumnDefs() {

  var columns = JSON.parse($('#plotPageForm\\:columnHeadings').val())

  var dateCol = [0];
  var valueCols = range(1, columns.length);

  return [
  {"render":
      function (data, type, row) {
        return formatForTable(new Date(data));
      },
      "targets": dateCol
    },
    {"defaultContent": "",
        "targets": valueCols
    },
    {"render":
      function (data, type, row) {
        // 0 = value
        // 1 = used
        // 2 = QC flag
        // 3 = Needs Flag
        // 4 = QC Comment
  
        var result = '';
  
        if (null != data) {
            var flagClass = null;
            switch (data[2]) {
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
  
            var classes = ['numericCol'];
            if (!data[1]) {
              classes.push('unused');
            }
  
            if (null != flagClass) {
              classes.push(flagClass);
            }
  
            if (data[3]) {
              classes.push('needsFlag');
            }
  
            result = '<div class="' + classes.join(' ') + '"';
  
            if (null != flagClass) {
              result += ' onmouseover="showQCMessage(' + data[2] + ', \''+ data[4] + '\')" onmouseout="hideQCMessage()"';
            }
  
            result += '>' + (null == data[0] ? "" : data[0].toFixed(3)) + '</div>';
            return result;
        }
      },
      "targets": valueCols
    }
  ];
}

function resizePlots() {
  // TODO See if we can make this work stuff out automatically
  // when the plots are stored in plotPage.js
  // See issue #564
  $('#plot1Container').width('100%');
  $('#plot1Container').height($('#plot1Panel').height() - 40);
}

function showUseDialog() {
  // Select the first radio button, which is "Yes"
  PF('useCalibrationsWidget').jq.find('input:radio[value=true]').parent().next().trigger('click.selectOneRadio');
  $(PF('useCalibrationsMessageWidget').jqId).val("");
  updateUseDialogControls();
  PF('useDialog').show();
}

function storeCalibrationSelection() {
  $('#selectionForm\\:selectedColumn').val(selectedColumn);
  $('#selectionForm\\:selectedRows').val(selectedRows);
  setUseCalibrations(); // PF remoteCommand
}

function calibrationUpdated() {
  PF('useDialog').hide();
  clearSelection();
  redrawPlot(1);

  //Reload table data
  jsDataTable.ajax.reload(null, false);
}

function postSelectionUpdated() {
  if (selectedRows.length == 0) {
    PF('useCalibrationsButton').disable();
  } else {
    PF('useCalibrationsButton').enable();
  }
}

function updateUseDialogControls() {
  if (PF('useCalibrationsWidget').getJQ().find(':checked').val() == 'true') {
    $('#reasonSection').css('visibility', 'hidden');
    PF('okButtonWidget').enable();
  } else {
    $('#reasonSection').css('visibility', 'visible');
    if ($(PF('useCalibrationsMessageWidget').jqId).val().trim() == '') {
      PF('okButtonWidget').disable();
    } else {
      PF('okButtonWidget').enable();
    }
  }
}

function getPlotMode(index) {
  return 'plot'; // No maps allowed!
}

function getPlotTargetValue(index) {
  var labelFields = getPlotLabels(index)[4].split(':');
  return calibrationJson[labelFields[0]][labelFields[1]];
}