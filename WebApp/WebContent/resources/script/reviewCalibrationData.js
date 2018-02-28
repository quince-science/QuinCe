
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
  jsDataTable.columns(5).visible(false, false);
}

/*
 * Formats etc for table columns
 */
function getColumnDefs() {
  return [
        {"className": "noWrap", "targets": [0]},
        {"className": "centreCol", "targets": [2, 4]},
        {"className": "numericCol", "targets": [3]},
        {"render":
          function (data, type, row) {
            return $.format.date(data, 'yyyy-MM-dd HH:mm:ss');
          },
          "targets": 1
        },
        {"render":
          function (data, type, row) {
            return data.toFixed(3);
          },
          "targets": 3
        },
        {"render":
          function (data, type, row) {
            var output = '<div onmouseover="showInfoPopup(' + 4 + ', \'' + row[5] + '\', this)" onmouseout="hideInfoPopup()" class="';
              output += data ? 'good' : 'bad';
              output += '">';
              output += data ? 'Yes' : 'No';
              output += '</div>';
              return output;
          },
          "targets": 4
        },
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
  $('#plotPageForm\\:selectedRows').val(selectedRows);
  $('#plotPageForm\\:setUseCalibrations').click();

  // Update the table data
  var rows = jsDataTable.rows()[0];
  for (var i = 0; i < rows.length; i++) {
    var row = jsDataTable.row(i);
    if ($.inArray(row.data()[0], selectedRows) > -1) {
      jsDataTable.cell(i, 4).data(PF('useCalibrationsWidget').getJQ().find(':checked').val() == 'true');
      jsDataTable.cell(i, 5).data($(PF('useCalibrationsMessageWidget').jqId).val());
    }
  }

  clearSelection();
  PF('useDialog').hide();
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
  return 'plot';
}
