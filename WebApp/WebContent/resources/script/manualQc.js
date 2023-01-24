function acceptAutoQc() {
  submitAutoQC(); // remoteCommand
}

function qcFlagsAccepted() {
  errorCheck();
  updateFlagCounts();

  PF('flagDialog').hide();

  drawFlagPlot(1);
  drawFlagPlot(2);
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

  if (getSelectedColumn().questionableAllowed) {
    $('#selectionForm\\:manualFlag_panel').find("[data-label='Questionable']").show()
  } else {
    $('#selectionForm\\:manualFlag_panel').find("[data-label='Questionable']").hide()
  }

  let woceRowHtml = getSelectedRows().length.toString() + ' row';
  if (getSelectedRows().length > 1) {
    woceRowHtml += 's';
  }
  $('#manualRowCount').html(woceRowHtml);

  $('#selectionForm\\:manualComment').val($('#selectionForm\\:userCommentList').val());

  PF('flagMenu').selectValue($('#selectionForm\\:worstSelectedFlag').val());
  updateFlagDialogControls();
  PF('flagDialog').show();
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
  updateFlagCounts();
}
