var UPDATING_UI = false;
var SELECT_STATE = -1;
const SERIES_0_COLOR = '#53C621';
const SERIES_1_COLOR = '#000080';
const SERIES_HIDDEN_COLOR = '#dddddd';
const DATA_POINT_SIZE = 2;
const DATA_POINT_HIGHLIGHT_SIZE = 8.5;
const HIGHLIGHT_POINT_SIZE = 8;
const HIGHLIGHT_COLOR = '#FF8800';
const PLOT_X_PAD = 10;
const PLOT_Y_PAD = 20;

var firstDate = null;
var lastDate = null;


// Timer used to prevent event spamming during page resizes
var resizeEventTimer = null;

var intModel = Dygraph.defaultInteractionModel;
intModel.dblclick = function(e, x, points) {
  // Empty callback
};

var TIMESERIES_PLOT_OPTIONS = {
  colors: [SERIES_0_COLOR, SERIES_1_COLOR],
  drawPoints: true,
  strokeWidth: 0.0,
  labelsUTC: true,
  digitsAfterDecimal: 2,
  legend: 'never',
  selectMode: 'euclidian',
  animatedZooms: false,
  xRangePad: PLOT_X_PAD,
  yRangePad: PLOT_Y_PAD,
  xlabel: 'Date/Time',
  interactionModel: intModel,
  clickCallback: timeSeriesClick,
  pointSize: DATA_POINT_SIZE,
  highlightCircleSize: DATA_POINT_SIZE,
  axes: {
    x: {
      drawGrid: false
    },
    y: {
      drawGrid: true,
      gridLinePattern: [1, 3],
      gridLineColor: 'rbg(200, 200, 200)',
    }
  }
}

var OFFSET_PLOT_OPTIONS = {
  colors: ['#9999ff'],
  drawPoints: true,
  strokeWidth: 2,
  labelsUTC: true,
  digitsAfterDecimal: 2,
  legend: 'never',
  selectMode: 'euclidian',
  animatedZooms: false,
  xRangePad: PLOT_X_PAD,
  yRangePad: PLOT_Y_PAD,
  xlabel: 'Date/Time',
  ylabel: 'Offset (s)',
  highlightCircleSize: DATA_POINT_SIZE,
  axes: {
    x: {
      drawGrid: false
    },
    y: {
      drawGrid: true,
      gridLinePattern: [1, 3],
      gridLineColor: 'rbg(200, 200, 200)',
      includeZero: true
    }
  }
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
    position: $('#bottomHalf').width() - 425,
    onDragEnd: function() {
      resizeBottomHalf();
    }
  });

}

function resizeAllContent() {
  resizeTimeSeriesPlot();
  resizeBottomHalf();
}

function resizeBottomHalf() {
  resizeOffsetPlot();
  $('#offsetsTable').width('100%');
  $('#offsetsTable').height('100%');
  $('#offsetFormContainer').width('100%');
  $('#offsetFormContainer').height('100%');
}

// Draws the initial page data once loading is complete.
// Called by oncomplete of loadData() PF remoteCommand
function dataPrepared() {
  drawTimeSeriesPlot(true);
  drawOffsetsPlot();
  resizeAllContent();
  PF('pleaseWait').hide();
}

function newGroupSelected() {
  PF('pleaseWait').show();
  cancelOffset();
  changeGroup(); // PF RemoteCommand
}

function drawTimeSeriesPlot(resetZoom) {

  let xAxisRange = null;
  let yAxisRange = null;

  if (null != window['timeSeriesPlot']) {
	if (!resetZoom) {
      xAxisRange = window['timeSeriesPlot'].xAxisRange();
      yAxisRange = window['timeSeriesPlot'].yAxisRange();
	}

    window['timeSeriesPlot'].destroy();	
    window['timeSeriesPlot'] = null;
  }
  
  let plotOptions = Object.assign({}, TIMESERIES_PLOT_OPTIONS);
  plotOptions.series = {}
  plotOptions['series'][$('#timeSeriesForm\\:series1Name').val()] = {
    'axis': 'y2'
  };
  plotOptions.ylabel = $('#timeSeriesForm\\:series0Name').val();
  plotOptions.y2label = $('#timeSeriesForm\\:series1Name').val();
  plotOptions.labels = ['Date/Time', $('#timeSeriesForm\\:series0Name').val(),
  	$('#timeSeriesForm\\:series1Name').val()];
  plotOptions.underlayCallback = drawHighlights; // Function call

  if (!resetZoom) {
    plotOptions.dateWindow = xAxisRange;
    plotOptions.valueRange = yAxisRange;
    plotOptions.yRangePad = 0;
    plotOptions.xRangePad = 0;
  }

  window['timeSeriesPlot'] = new Dygraph(
    document.getElementById('timeSeriesPlotContainer'),
    makePlotData,
    plotOptions
  );

}

function resizeTimeSeriesPlot() {
  if (null != window['timeSeriesPlot'] && null != window['timeSeriesPlot'].maindiv_) {
    $('#timeSeriesContainer').width('100%');
    $('#timeSeriesContainer').height($('#timeSeries').height() - 40);
    window['timeSeriesPlot'].resize($('#timeSeriesContainer').width(), $('#timeSeriesContainer').height());
  }
}

function resizeOffsetPlot() {
  if (null != window['offsetsPlot'] && null != window['offsetsPlot'].maindiv_) {
    $('#offsetsPlotContainer').width('100%');
    $('#offsetsPlotContainer').height($('#bottomLeft').height() - 5);
    window['offsetsPlot'].resize($('#offsetsPlot').width(), $('#offsetsPlotContainer').height());
  }
}

function resetZoom(plotName) {
  window[plotName].updateOptions({
    yRangePad: PLOT_X_PAD,
    xRangePad: PLOT_Y_PAD
  });

  window[plotName].resetZoom();
}

function startAddOffset() {
  $('#offsetForm\\:firstTime').val('');
  $('#offsetForm\\:secondTime').val('');
  PF('firstSelect').uncheck();
  PF('secondSelect').uncheck();
  updateOffsetTimeText();
  PF('saveOffset').disable();
  $('#offsetsTable').hide();
  $('#offsetFormContainer').show();
}

function cancelOffset() {
  SELECT_STATE = -1;
  updateHighlightSettings();
  showOffsetsTable();
}

function showOffsetsTable() {
  $('#offsetFormContainer').hide();
  $('#offsetsTable').show();
}

function offsetsUpdated() {
  showOffsetsTable();
  drawTimeSeriesPlot(false);
  drawOffsetsPlot();
}

function updateOffsetTimeText() {
  let canCalculateOffset = true;

  if ($('#offsetForm\\:firstTime').val() == '') {
    $('#offsetForm\\:firstTimeText')[0].innerHTML = '&lt;Not selected&gt;';
    canCalculateOffset = false;
  } else {
	$('#offsetForm\\:firstTimeText')[0].innerHTML = new Date(parseInt($('#offsetForm\\:firstTime').val())).toISOString();
  }

  if ($('#offsetForm\\:secondTime').val() == '') {
    $('#offsetForm\\:secondTimeText')[0].innerHTML = '&lt;Not selected&gt;';
    canCalculateOffset = false;
  } else {
	$('#offsetForm\\:secondTimeText')[0].innerHTML = new Date(parseInt($('#offsetForm\\:secondTime').val())).toISOString();
  }

  if (!canCalculateOffset) {
	$('#offsetText').html('Not set');
  } else {
	let offset = (parseFloat($('#offsetForm\\:secondTime').val()) - parseFloat($('#offsetForm\\:firstTime').val())) / 1000;
    $('#offsetText').html(offset.toFixed(3) + ' s');
  }
}

function timeSeriesClick(e, x, points) {
  if (SELECT_STATE >= 0) {
    let selectedMillis = points[SELECT_STATE].xval;	

    if (SELECT_STATE == 0) {
	  $('#offsetForm\\:firstTime').val(selectedMillis);
    } else if (SELECT_STATE == 1) {
	  $('#offsetForm\\:secondTime').val(selectedMillis);
    }

    UPDATING_UI = true;
    PF('firstSelect').uncheck();
    PF('secondSelect').uncheck();
    updateAddOffsetButton();
    UPDATING_UI = false;

    updateHighlightSettings();
    updateOffsetTimeText();
  }
}

function updateAddOffsetButton() {
  if ($('#offsetForm\\:firstTime').val() == '' || $('#offsetForm\\:secondTime').val() == '') {
	PF('saveOffset').disable();	
  } else {
	PF('saveOffset').enable();
  }
}

function firstSelectClick() {
  if (!UPDATING_UI) {
    UPDATING_UI = true;
    PF('secondSelect').uncheck();
    UPDATING_UI = false;
  }

  if (PF('firstSelect').input[0].checked) {
    SELECT_STATE = 0;
    updateHighlightSettings();
  } else {
    SELECT_STATE = -1;	
    updateHighlightSettings();
  }
}

function secondSelectClick() {
  if (!UPDATING_UI) {
    UPDATING_UI = true;	
    PF('firstSelect').uncheck();
    UPDATING_UI = false;
  }

  if (PF('secondSelect').input[0].checked) {
    SELECT_STATE = 1;
    updateHighlightSettings();
  } else {
    SELECT_STATE = -1;
    updateHighlightSettings();
  }
}

function updateHighlightSettings() {

  if (!UPDATING_UI) {
    let seriesOpts = {}

    let series0Opts = {}
    if (SELECT_STATE == 1) {
  	  series0Opts.color = SERIES_HIDDEN_COLOR;	
    } else {
	  series0Opts.color = SERIES_0_COLOR;	
    }

    if (SELECT_STATE == 0) {
      series0Opts.highlightCircleSize = DATA_POINT_HIGHLIGHT_SIZE;	
    } else {
      series0Opts.highlightCircleSize = DATA_POINT_SIZE;
    }

    seriesOpts[$('#timeSeriesForm\\:series0Name').val()] = series0Opts;
    
    let series1Opts = {}
    if (SELECT_STATE == 0) {
      series1Opts.color = SERIES_HIDDEN_COLOR;
    } else {
      series1Opts.color = SERIES_1_COLOR;	
    }

    if (SELECT_STATE == 1) {
      series1Opts.highlightCircleSize = DATA_POINT_HIGHLIGHT_SIZE;	
    } else {
	  series1Opts.highlightCircleSize = DATA_POINT_SIZE;
    }

    seriesOpts[$('#timeSeriesForm\\:series1Name').val()] = series1Opts;

	// For some reason the graph likes to mess up the left Y axis
	// when we do this. Resetting the X axis range seems to stop it happening.
	// Ugly but I don't have time to investigate further.
    axisOpts = {};
    axisOpts.x = {};
    axisOpts.x.valueRange = window['timeSeriesPlot'].xAxisRange();

    window['timeSeriesPlot'].updateOptions({
      series: seriesOpts,
      axes: axisOpts
    });
  }
}

function deleteOffset(offsetTime) {
  $('#offsetForm\\:deleteTime').val(offsetTime);
  deleteOffsetAction(); // PF remoteCommand
}

// Make the data for the plots - combine data and offsets
function makePlotData() {
	
  data = JSON.parse($('#timeSeriesForm\\:plotData').val());

  data.forEach(row => {
    row[0] = new Date(row[0]);
  });
  
  firstDate = data[0][0];
  lastDate = data[data.length - 1][0];
  
  return data;
}

function drawHighlights(canvas, area, g) {

  if ($('#timeSeriesForm\\:offsetsData').val() != '') {
	let highlights = JSON.parse($('#timeSeriesForm\\:offsetsData').val());

    canvas.fillStyle = HIGHLIGHT_COLOR;
    canvas.strokeStyle = HIGHLIGHT_COLOR;
    canvas.lineWidth = 3;
    canvas.setLineDash([5, 3]);
	
	highlights.forEach((h) => {
      let x1 = g.toDomXCoord(new Date(h['firstTime']));
      let y1 = g.toDomYCoord(h['firstValue'], 0);
      let x2 = g.toDomXCoord(new Date(h['secondTime']));
      let y2 = g.toDomYCoord(h['secondValue'], 1);
		
	  drawHighlightCircle(canvas, g, x1, y1);
	  drawHighlightCircle(canvas, g, x2, y2);
	  
	  canvas.beginPath();
	  canvas.moveTo(x1, y1);
	  canvas.lineTo(x2, y2);
	  canvas.stroke();
  	});
  	
  	canvas.setLineDash([]);
  } 
}

function drawHighlightCircle(canvas, g, x, y) {  
  canvas.beginPath();
  canvas.arc(x, y, HIGHLIGHT_POINT_SIZE, 0, 2 * Math.PI, false);
  canvas.fill();  
}

function drawOffsetsPlot() {

  if (null != window['offsetsPlot']) {
    window['offsetsPlot'].destroy();
    window['offsetsPlot'] = null;
  }

  data = [];

  let offsets = [];
  
  let minOffset = 0;
  let maxOffset = 0;
  
  if ($('#timeSeriesForm\\:offsetsData').val() != '') {
    offsets = JSON.parse($('#timeSeriesForm\\:offsetsData').val());
  }
  
  if (offsets.length == 0) {
    data.push([firstDate, 0]);
    data.push([lastDate, 0]);
  } else {
    data.push([firstDate, offsets[0]['offset'] / 1000]);
    
    minOffset = Number.MAX_SAFE_INTEGER;
    maxOffset = Number.MIN_SAFE_INTEGER;
    
    offsets.forEach(offset => {
	  offsetSeconds = offset['offset'] / 1000;
      data.push([new Date(offset['firstTime']), offsetSeconds]);
      if (offsetSeconds < minOffset) {
        minOffset = offsetSeconds;
      }
      if (offsetSeconds > maxOffset) {
        maxOffset = offsetSeconds;
      }
    });
    
    data.push([lastDate, offsets[offsets.length - 1]['offset'] / 1000]);
  }

  let plotOptions = Object.assign({}, OFFSET_PLOT_OPTIONS);

  window['offsetsPlot'] = new Dygraph(
    document.getElementById('offsetsPlotContainer'),
    data, plotOptions
  );
}
