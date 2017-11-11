var standardsPlot = null;

function start() {
	drawPage();
}

function drawPage() {
	drawPlot();
	drawTable();
}

function drawPlot() {
	// Remove the existing plot
	if (null != standardsPlot) {
		standardsPlot.destroy();
	}
	
	var graph_options = BASE_GRAPH_OPTIONS;
	graph_options.labels = JSON.parse($('#plotPageForm\\:plotLabels').val());
	
	standardsPlot = new Dygraph (
			document.getElementById('standardsPlotContainer'),
			makeJSDates(JSON.parse($('#plotPageForm\\:plotData').val())),
			BASE_GRAPH_OPTIONS
		);
}

/*
 * Show or hide columns as required.
 */
function renderTableColumns() {
	jsDataTable.columns(0).visible(false, false);
	jsDataTable.columns(5).visible(false, false);
}
