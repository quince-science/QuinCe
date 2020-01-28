function addDeployment() {
  $('#deploymentForm\\:calibrationId').val(-1);
  loadCalibration(); // PF RemoteCommand
}

function editDeployment(item) {
  // Ignore dataset clicks (they have a string id)
  if (null != item.item && typeof(item.item) === 'number') {
    $('#deploymentForm\\:calibrationId').val(item.item);
    loadCalibration(); // PF RemoteCommand
  }
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
