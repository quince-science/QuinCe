const ADD = 1;
const EDIT = 0;
const DELETE = -1;

function addDeployment() {
  hideSelectionDetails();
  $('#deploymentForm\\:calibrationId').val(-1);
  $('#deploymentForm\\:editAction').val(ADD);
  newCalibration();
}

function selectDeployment(item) {
  // Ignore dataset clicks (they have a string id)
  if (null != item.item && typeof(item.item) === 'number') {
    $('#deploymentForm\\:calibrationId').val(item.item);
    selectCalibration();
  }
}

function editSelection() {
  $('#deploymentForm\\:editAction').val(EDIT);
  PF('deploymentDialog').show();
}

function deleteSelection() {
  $('#deploymentForm\\:editAction').val(DELETE);
  PF('checkDatasetsButton').jq.click();
}

function showSelectionDetails() {
  PF('selectionDetails').jq.show();
}

function hideSelectionDetails() {
  PF('selectionDetails').jq.hide();
}

function showAffectedDatasets() {
  
  PF('deploymentDialog').hide();
  let affectedDatasetsStatus = parseInt(PF('affectedDatasetsStatus').jq.val());
    
  PF('affectedDatasetsDialog').show();
    
  if (affectedDatasetsStatus < 0) {
    PF('saveEditButton').disable();
  } else {
    PF('saveEditButton').enable();
  }
}
