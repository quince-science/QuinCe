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
