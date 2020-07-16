function showUseDialog() {
  // Select the first radio button, which is "Yes"
  PF('useCalibrationsWidget').jq.find('input:radio[value=true]').parent().next().trigger('click.selectOneRadio');
  $(PF('useCalibrationsMessageWidget').jqId).val("");
  updateUseDialogControls();
  PF('useDialog').show();
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

function calibrationUpdated() {
  errorCheck();
  
  PF('useDialog').hide();
  
  drawFlagPlot(1);
  drawFlagPlot(2);
  clearSelection();
  
  // Reload table data
  jsDataTable.ajax.reload(null, false);
  itemNotLoading(UPDATE_DATA);
}