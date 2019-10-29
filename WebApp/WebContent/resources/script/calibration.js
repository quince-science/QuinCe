function addDeployment() {
  $('#deploymentForm\\:deploymentId').val(-1);
  PF('deploymentDialog').show();
}

function editDeployment(item) {
  if (null != item.item) {

    // Find the selected item
    calibration = calibrationsJson.find(c => c.id == item.item);
    
    // Fill and show the deployment dialog
    $('#deploymentForm\\:deploymentId').val(calibration.id);
    PF('deploymentDate').setDate(calibration.start);
    PF('target').selectValue(calibration.target);
    
    for (var i = 0; i < calibration.coefficients.length; i++) {
      if (PF('coefficient-' + i)) {
        PF('coefficient-' + i).setValue(calibration.coefficients[i]);
      }
    }
    
    PF('deploymentDialog').show();
  }
}
