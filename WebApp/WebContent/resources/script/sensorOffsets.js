// Timer used to prevent event spamming during page resizes
var resizeEventTimer = null;

var intModel = Dygraph.defaultInteractionModel;
intModel.dblclick = function(e, x, points) {
  // Empty callback
};

var TIMESERIES_PLOT_OPTIONS = {
  colors: ['#018000', '#000080'],
  drawPoints: true,
  strokeWidth: 0.0,
  labelsUTC: true,
  digitsAfterDecimal: 2,
  legend: 'never',
  selectMode: 'euclidian',
  animatedZooms: false,
  xRangePad: 10,
  yRangePad: 10,
  xlabel: 'Date/Time',
  interactionModel: intModel
}

function initPage() {
  PF('pleaseWait').show();
  
  // When the window is resized, scale the panels
  $(window).resize(function() {
    clearTimeout(resizeEventTimer);
    resizeEventTimer = setTimeout(resizeAllContent, 100);
  });

  // Draw the basic page layout
  layoutPage();

  // Trigger data loading on back end
  // PrimeFaces remoteCommand. Calls dataLoaded() when complete.
  loadData();
}

function layoutPage() {
  // Top/bottom split
  $('#pageContent').split({
    orientation: 'horizontal',
    onDragEnd: function() {
      resizeAllContent();
    }
  });

  // Split bottom left/right
  $('#bottomHalf').split({
    orientation: 'vertical',
    onDragEnd: function() {
      resizeBottomHalf();
    }
  });

}

function resizeAllContent() {
  console.log('resizeAllContent');
  resizeTimeSeriesPlot();
  resizeBottomHalf();
}

function resizeBottomHalf() {
  console.log('resizeBottomHalf');
}

// Draws the initial page data once loading is complete.
// Called by oncomplete of loadData() PF remoteCommand
function dataPrepared() {
  drawTimeSeriesPlot();
  PF('pleaseWait').hide();
}

function newGroupSelected() {
  PF('pleaseWait').show();
  changeGroup(); // PF RemoteCommand
}

function drawTimeSeriesPlot() {
  window['timeSeriesPlot'] = new Dygraph(
    document.getElementById('timeSeriesPlot'),
    $('#timeSeriesForm\\:plotData').val(),
	TIMESERIES_PLOT_OPTIONS
  );

  resizeTimeSeriesPlot();
}

function resizeTimeSeriesPlot() {
  if (null != window['timeSeriesPlot'] && null != window['timeSeriesPlot'].maindiv_) {
    $('#timeSeriesContainer').width('100%');
    $('#timeSeriesContainer').height($('#timeSeries').height() - 40);
    window['timeSeriesPlot'].resize($('#timeSeriesContainer').width(), $('#timeSeriesContainer').height());
  }
}

function resetZoom(plotName) {
  window[plotName].updateOptions({
    yRangePad: 10,
    xRangePad: 10
  });

  window[plotName].resetZoom();
}