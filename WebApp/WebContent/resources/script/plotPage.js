var PLOT_POINT_SIZE = 2;
var PLOT_HIGHLIGHT_SIZE = 5;
var PLOT_FLAG_SIZE = 8;

var BASE_GRAPH_OPTIONS = {
    drawPoints: true,
    strokeWidth: 0.0,
    labelsUTC: true,
    labelsSeparateLine: true,
    digitsAfterDecimal: 2,
    animatedZooms: true,
    pointSize: PLOT_POINT_SIZE,
    highlightCircleSize: PLOT_HIGHLIGHT_SIZE,
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
};

//The list of dates in the current view. Used for searching.
var dateList = null;


$(function() {
	// Make the panel splits
	$('#plotPageContent').split({orientation: 'horizontal', onDragEnd: function(){resizeContent()}});
	
	if (typeof start == 'function') {
		start();
	}
});

function resizeContent() {
	// Don't do anything just now
}

function makeJSDates(data) {
	
	dateList = new Array();
	
	for (var i = 0; i < data.length; i++) {
		point_data = data[i];
		
		// Store the milliseconds value in the global dates list
		dateList.push(point_data[0]);
		
		// Replace the milliseconds value with a Javascript date
		point_data[0] = new Date(point_data[0]);
		
		data[i] = point_data;
	}

	return data;
}
