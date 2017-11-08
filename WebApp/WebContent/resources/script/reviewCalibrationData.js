var standardsPlot = null;

function start() {
	drawPage();
}

function drawPage() {
	drawPlot();
}

function drawPlot() {
	// Remove the existing plot
	if (null != standardsPlot) {
		standardsPlot.destroy();
	}
	
	standardsPlot = new Dygraph (
			document.getElementById('standardsPlotContainer'),
			$('#plotPageForm\\:plotData').val()
		);
}