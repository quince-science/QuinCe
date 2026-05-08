function processNewDataSet(eventType) {

  // Check the limits of the values and set them
  let start = Number(PF('start').getValue());
  let end = Number(PF('end').getValue());

  let minStart = Number($('#referenceForm\\:minStart').val());
  let maxEnd = Number($('#referenceForm\\:maxEnd').val());
	
  if (eventType == 'start') {
    if (start < minStart) {
      PF('start').setValue(minStart);
    } else if (start > end) {
      PF('start').setValue(end);
    }
  }

  if (eventType == 'end') {	
    if (end > maxEnd) {
      PF('end').setValue(maxEnd);
    } else if (end < start) {
      PF('end').setValue(start);
    }
  }
  
  if (null != eventType) {
	$('#newDatasetForm\\:dataSetName').val(PF('start').getValue() + '-' + PF('end').getValue());
  }
}
