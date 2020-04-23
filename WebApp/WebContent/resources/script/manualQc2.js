function acceptAutoQc() {
  fillSelectionForm();
  submitAutoQC(); // remoteCommand
}

function qcFlagsAccepted() {
  errorCheck();
  
  PF('flagDialog').hide();
  
  clearSelection();
  // redrawPlot(1);
  // redrawPlot(2);

  // Reload table data
  jsDataTable.ajax.reload(null, false);
}

function startUserQcFlags() {
  fillSelectionForm();
  generateUserQCComments(); // remoteCommand
}

function showFlagDialog() {
  errorCheck();

  let woceRowHtml = selectedRows.length.toString() + ' row';
  if (selectedRows.length > 1) {
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
  fillSelectionForm();
  applyManualFlag(); // remoteCommand
}
