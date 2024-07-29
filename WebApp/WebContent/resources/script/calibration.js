const ADD = 1;
const EDIT = 0;
const DELETE = -1;

var TIMELINE_OPTIONS = {
  showCurrentTime: false,
  selectable: false,
  editable: false,
   zoomMin: 3600000,
  moment: function(date) {
   return vis.moment(date).utc();
  },
  format: {
    minorLabels: {
      week: 'D',
    }
  }
};

var timeline = null;

function drawTimeline() {
  if (null != timeline) {
    timeline.destroy();
  }
	
  let targets = JSON.parse($('#timelineData\\:targetsJson').val());
  let timelineJson = JSON.parse($('#timelineData\\:timelineJson').val());
  // Treat dates as dates in the dataset
  timelineJson.map(function (item) {
    item.start = new Date(item.start);
  });

  let calibrations =  new vis.DataSet(timelineJson);
  timeline = new vis.Timeline(timelineContainer, calibrations,
    targets, TIMELINE_OPTIONS);
  timelineContainer.onclick = function (event) {
    selectDeployment(timeline.getEventProperties(event));
  }
}


function addDeployment() {
  hideSelectionDetails();
  $('#deploymentForm\\:calibrationId').val(new Date().getTime() * -1);
  $('#deploymentForm\\:action').val(ADD);
  newCalibration(); // PF remoteCommand in calibration.xhtml
}

function selectDeployment(item) {
  // Ignore dataset clicks (they have a string id)
  if (null != item.item && typeof(item.item) === 'number') {
    $('#deploymentForm\\:calibrationId').val(item.item);
    selectCalibration(); // PF RemoteCommand
  }
}

function editSelection() {
  $('#deploymentForm\\:action').val(EDIT);
}

function deleteSelection() {
  $('#deploymentForm\\:action').val(DELETE);
  PF('saveCalibrationButton').jq.click();
}

function showSelectionDetails() {
  PF('selectionDetails').jq.show();
}

function hideSelectionDetails() {
  PF('selectionDetails').jq.hide();
}

function calibrationSaveComplete() {
  drawTimeline();
  hideSelectionDetails();
  PF('deploymentDialog').hide();
}