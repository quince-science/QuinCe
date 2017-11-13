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

/*
 * Formats etc for table columns
 */
function getColumnDefs() {
	return [
        {"className": "noWrap", "targets": [0]},
        {"className": "centreCol", "targets": [2, 4]},
        {"className": "numericCol", "targets": [3]},
        {"render":
        	function (data, type, row) {
        		return $.format.date(data, 'yyyy-MM-dd HH:mm:ss');
        	},
        	"targets": 1
        },
        {"render":
        	function (data, type, row) {
	            var output = '<div class="';
	            output += data ? 'good' : 'bad';
	            output += '">';
	            output += data ? 'Yes' : 'No';
	            output += '</div>';
	            return output;
        	},
        	"targets": 4
        },
	];
}