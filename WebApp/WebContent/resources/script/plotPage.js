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
