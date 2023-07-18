function showUseDialog() {
  updateUseDialogControls();
  PF('useDialog').show();
}

function updateUseDialogControls() {
  if (PF('useCalibrationsWidget').getJQ().find(':checked').val() == 2) {
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

function calibrationUpdated() {
  errorCheck();
  
  PF('useDialog').hide();
  
  drawPlot(1, true, false);
  drawPlot(2, true, false);
  clearSelection();
  
  // Reload table data
  jsDataTable.ajax.reload(null, false);
  itemNotLoading(UPDATE_DATA);
}

function acceptAutoQc() {
  submitAutoQC(); // remoteCommand
}

function startUserQcFlags() {
  generateUserQCComments(); // remoteCommand
}
