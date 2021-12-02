var UPDATING_UI = false;
var SELECT_STATE = -1;
const SERIES_0_COLOR = '#018000';
const SERIES_1_COLOR = '#000080';
const SERIES_HIDDEN_COLOR = '#dddddd';
const DATA_POINT_SIZE = 2;
const DATA_POINT_HIGHLIGHT_SIZE = 5;


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
  xRangePad: 10,
  yRangePad: 20,
  xlabel: 'Date/Time',
  interactionModel: intModel,
  clickCallback: timeSeriesClick,
  pointSize: DATA_POINT_SIZE,
  highlightCircleSize: DATA_POINT_SIZE
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

function startAddOffset() {
  $('#offsetForm\\:firstTime').val('');
  $('#offsetForm\\:secondTime').val('');
  updateOffsetTimeText();
  PF('saveOffset').disable();
  $('#offsetsTable').hide();
  $('#offsetFormContainer').show();
}

function cancelOffset() {
  showOffsetsTable();
}

function showOffsetsTable() {
  $('#offsetFormContainer').hide();
  $('#offsetsTable').show();
}

function offsetsUpdated() {
  showOffsetsTable();
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