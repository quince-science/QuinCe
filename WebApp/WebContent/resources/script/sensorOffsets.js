var UPDATING_UI = false;
var SELECT_STATE = -1;

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
  interactionModel: intModel,
  clickCallback: timeSeriesClick
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
  $('#offsetsTable').hide();
  $('#offsetFormContainer').show();
}

function updateOffsetTimeText() {
  if ($('#offsetForm\\:firstTime').val() == '') {
    $('#offsetForm\\:firstTimeText')[0].innerHTML = '&lt;Not selected&gt;';
  } else {
	$('#offsetForm\\:firstTimeText')[0].innerHTML = new Date(parseInt($('#offsetForm\\:firstTime').val())).toISOString();
  }

  if ($('#offsetForm\\:secondTime').val() == '') {
    $('#offsetForm\\:secondTimeText')[0].innerHTML = '&lt;Not selected&gt;';
  } else {
	$('#offsetForm\\:secondTimeText')[0].innerHTML = new Date(parseInt($('#offsetForm\\:secondTime').val())).toISOString();
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
    UPDATING_UI = false;

    updateOffsetTimeText();
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
  } else {
    SELECT_STATE = -1;	
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
  } else {
    SELECT_STATE = -1;	
  }
}