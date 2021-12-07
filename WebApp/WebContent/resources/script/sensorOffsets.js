var UPDATING_UI = false;
var SELECT_STATE = -1;
const SERIES_0_COLOR = '#018000';
const SERIES_1_COLOR = '#000080';
const SERIES_HIDDEN_COLOR = '#dddddd';
const DATA_POINT_SIZE = 2;
const DATA_POINT_HIGHLIGHT_SIZE = 8.5;
const HIGHLIGHT_POINT_SIZE = 8;
const PLOT_X_PAD = 10;
const PLOT_Y_PAD = 20;


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
  zoomCallback: function(xMin, xMax, yRange) {
    syncZoom();
  },
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

var HIGHLIGHT_PLOT_OPTIONS = {
  colors: ['#FFA42B', '#FFFF00'],
  drawPoints: true,
  strokeWidth: 0.0,
  labelsUTC: true,
  digitsAfterDecimal: 2,
  legend: 'never',
  selectMode: 'euclidian',
  animatedZooms: false,
  xRangePad: PLOT_X_PAD,
  yRangePad: PLOT_Y_PAD,
  xlabel: ' ',
  ylabel: ' ',
  interactionModel: null,
  pointSize: HIGHLIGHT_POINT_SIZE,
  highlightCircleSize: 0,
  axes: {
    x: {
      drawGrid: false
    },
    y: {
      drawGrid: false
    }
  },
  xAxisHeight: 20,
  axisLabelFontSize: 0
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
  resizeOffsetPlot();
  resizeBottomRight();
}

function resizeBottomHalf() {
  console.log('resizeBottomHalf');
}

function resizeBottomRight() {
  $('#offsetsTable').width('100%');
  $('#offsetsTable').height('100%');
  $('#offsetFormContainer').width('100%');
  $('#offsetFormContainer').height('100%');
}

function resizeOffsetPlot() {
  console.log('resizeOffsetPlot');
}

// Draws the initial page data once loading is complete.
// Called by oncomplete of loadData() PF remoteCommand
function dataPrepared() {
  makeHighlightPlotData();
  drawTimeSeriesPlot();
  drawHighlightPlot();
  resizeTimeSeriesPlots();
  PF('pleaseWait').hide();
}

function newGroupSelected() {
  PF('pleaseWait').show();
  cancelOffset();
  changeGroup(); // PF RemoteCommand
}

function drawTimeSeriesPlot() {
	
  if (null != window['timeSeriesPlot']) {
    window['timeSeriesPlot'].destroy();	
    window['timeSeriesPlot'] = null;
  }

  window['timeSeriesPlot'] = new Dygraph(
    document.getElementById('timeSeriesPlotContainer'),
    $('#timeSeriesForm\\:plotData').val(),
	TIMESERIES_PLOT_OPTIONS
  );
}

function drawHighlightPlot() {
  if (null != window['highlightPlot']) {
    window['highlightPlot'].destroy();
    window['highlightPlot'] = null;
  }

  if (null != window['highlightData'] && window['highlightData'].length > 0) {
    window['highlightPlot'] = new Dygraph(
      document.getElementById('highlightPlotContainer'),
      window['highlightData'],
      HIGHLIGHT_PLOT_OPTIONS
    );

    syncZoom();
  }
}

function resizeTimeSeriesPlots() {
  if (null != window['timeSeriesPlot'] && null != window['timeSeriesPlot'].maindiv_) {
    $('#timeSeriesContainer').width('100%');
    $('#timeSeriesContainer').height($('#timeSeries').height() - 40);
    window['timeSeriesPlot'].resize($('#timeSeriesContainer').width(), $('#timeSeriesContainer').height());
    
    if (null != window['highlightPlot']) {
      window['highlightPlot'].resize($('#timeSeriesContainer').width(), $('#timeSeriesContainer').height());	
    }

    syncZoom();
  }
}

function makeHighlightPlotData() {
  // Data is for 2 series - first is offsets, second is for 'live' highlights.
  // Here we just build the offsets - highlights are added as required later on.  

  let array = [];

  JSON.parse($('#timeSeriesForm\\:offsetsData').val()).forEach( o => {
	array.push([new Date(o['firstTime']), o['firstValue'], null]);   	
	array.push([new Date(o['secondTime']), o['secondValue'], null]);   	
  });

  window['highlightData'] = array;
}

function resetZoom(plotName) {
  window[plotName].updateOptions({
    yRangePad: PLOT_X_PAD,
    xRangePad: PLOT_Y_PAD
  });

  window[plotName].resetZoom();
  syncZoom();
}

function syncZoom() {
  if (null != window['highlightPlot']) {
    let zoomOptions = {
      dateWindow: window['timeSeriesPlot'].xAxisRange(),
      valueRange: window['timeSeriesPlot'].yAxisRange(),
      yRangePad: PLOT_X_PAD,
      xRangePad: PLOT_Y_PAD
    };

    window['highlightPlot'].updateOptions(zoomOptions);
  }
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
  makeHighlightPlotData();
  drawHighlightPlot();
  resizeTimeSeriesPlots();
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

    window['timeSeriesPlot'].updateOptions({
      series: seriesOpts	
    });
  }
}

function deleteOffset(offsetTime) {
  $('#offsetForm\\:deleteTime').val(offsetTime);
  deleteOffsetAction(); // PF remoteCommand
}