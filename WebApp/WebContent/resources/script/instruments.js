const SHARE_ADD = 1;

$(document).ready(function() {
  $(window).keydown(function(event){
    if(event.keyCode == 13) {
      event.preventDefault();
      return false;
    }
  });
});

function goToStandards(id) {
    $('#instrumentListForm\\:standardsInstrumentId').val(id);
    $('#instrumentListForm\\:showStandardsLink').click();
    return false;
  }

function goToDiagnosticsQC(id) {
  $('#instrumentListForm\\:diagnosticQCInstrumentId').val(id);
  $('#instrumentListForm\\:diagnostQCLink').click();
  return false;
}

function goToCalibrations(id) {
  $('#instrumentListForm\\:calibrationsInstrumentId').val(id);
  $('#instrumentListForm\\:showCalibrationsLink').click();
  return false;
}

function goToCalculationCoefficients(id) {
  $('#instrumentListForm\\:calculationCoefficientsInstrumentId').val(id);
  $('#instrumentListForm\\:showCalculationCoefficientsLink').click();
  return false;
}

function confirmInstrumentDelete(id, name) {
  $('#instrumentListForm\\:deleteInstrumentId').val(id);
  $('#deleteInstrumentName')[0].innerHTML = name;
  PF('confirmDelete').show();
}

function deleteInstrument() {
  $('#instrumentListForm\\:deleteInstrumentLink').click();
  return false;
}

function showOwnershipDialog(id) {
  $('#shareForm\\:ownershipInstrId').val(id);
  loadOwnership(); // PF Remotecommand
  return false;
}

function startAddShare() {
  $('#shareForm\\:shareAction').val(SHARE_ADD);
  PF('shareEmail').jq.val('');
  PF('shareEmailMessage').jq.hide();
  PF('addShareDialog').show();
  return false;
}

function shareSaveComplete() {
  if ($('#shareForm\\:ajaxOK').val() == 'true') {
    PF('addShareDialog').hide();
  }
}
