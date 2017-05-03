/*
 * Javascript for the Data Screen
 */

/*
 * MAIN VARIABLES
 */

var PLOT_ROW_INDEX = 1;
var PLOT_QCFLAG_INDEX = 2;
var PLOT_WOCEFLAG_INDEX = 3;
var PLOT_FIRST_Y_INDEX = 4;

var PLOT_POINT_SIZE = 2;
var PLOT_HIGHLIGHT_SIZE = 5;
var PLOT_FLAG_SIZE = 8;

var FLAG_GOOD = 2;
var FLAG_ASSUMED_GOOD = -2;
var FLAG_QUESTIONABLE = 3;
var FLAG_BAD = 4;
var FLAG_FATAL = 44;
var FLAG_NEEDS_FLAG = -10;
var FLAG_IGNORED = -1002;

var SELECT_ACTION = 1;
var DESELECT_ACTION = 0;

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

var AXIS_LABELS = {
		'dateTime': 'Date/Time',
		'longitude': 'Longitude (°E)',
		'latitude': 'Latitude (°N)',
		'intakeTemp1': 'Temperature (°C)',
		'intakeTemp2': 'Temperature (°C)',
		'intakeTemp3': 'Temperature (°C)',
		'intakeTempMean': 'Temperature (°C)',
		'salinity1': 'Salinity (PSU)',
		'salinity2': 'Salinity (PSU)',
		'salinity3': 'Salinity (PSU)',
		'salinityMean': 'Salinity (PSU)',
		'eqt1': 'Temperature (°C)',
		'eqt2': 'Temperature (°C)',
		'eqt3': 'Temperature (°C)',
		'eqtMean': 'Temperature (°C)',
		'deltaT': 'Δ Temperature (°C)',
		'eqp1': 'Pressure (hPa)',
		'eqp2': 'Pressure (hPa)',
		'eqp3': 'Pressure (hPa)',
		'eqpMean': 'Pressure (hPa)',
		'airFlow1': 'Flow (ml/min)',
		'airFlow2': 'Flow (ml/min)',
		'airFlow3': 'Flow (ml/min)',
		'waterFlow1': 'Flow (ml/min)',
		'waterFlow2': 'Flow (ml/min)',
		'waterFlow3': 'Flow (ml/min)',
		'eqpMean': 'Pressure (hPa)',
		'xh2oMeasured': 'xH₂O (μmol/mol)',
		'xh2oTrue': 'xH₂O (μmol/mol)',
		'pH2O': 'pH₂O (UNITS)',
		'co2Measured': 'CO₂ (ppm/μatm)',
		'co2Dried': 'CO₂ (ppm/μatm)',
		'co2Calibrated': 'CO₂ (ppm/μatm)',
		'pCO2TEDry': 'CO₂ (ppm/μatm)',
		'pCO2TEWet': 'CO₂ (ppm/μatm)',
		'fCO2TE': 'CO₂ (ppm/μatm)',
		'fCO2Final': 'CO₂ (ppm/μatm)'
}

// Keeps track of the split positions as a percentage of the
// full data area
var tableSplitPercent = 0;
var plotSplitPercent = 0;

// The X Axis popup can only have one item selected;
// The Y axis can have more than one. This controls
// the selection limits
var plotPopupSingleSelection = true;

// Specifies the target plot (L/R) and axis (X/Y) for the popup
var plotPopupTarget = 'LX';

// The selected parameters for the plots and maps
var leftPlotXAxis = ['plot_datetime_dateTime'];
var leftPlotYAxis = ['plot_intaketemp_intakeTempMean'];

var rightPlotXAxis = ['plot_datetime_dateTime'];
var rightPlotYAxis = ['plot_co2_fCO2Final'];

// Variables for the plots
var leftGraph = null;
var rightGraph = null;

// Map variables
var mapPopupTarget = 'L';
var leftMapVar = 'map_co2_fCO2Final';
var rightMapVar = 'map_intaketemp_intakeTempMean';

var leftMap = null;
var rightMap = null;

var colorScale = new ColorScale([[0,'#2C7BB6'],[0.25,'#ABD9E9'],[0.5,'#FFFFBF'],[0.75,'#FDAE61'],[1,'#D7191C']]);
var leftMapDataLayer = null;
var rightMapDataLayer = null;

var mapSource = new ol.source.Stamen({
		layer: "terrain",
		url: "https://stamen-tiles-{a-d}.a.ssl.fastly.net/terrain/{z}/{x}/{y}.png"
});

// The data table
var jsDataTable = null;

// The callback function for the DataTables drawing call
var dataTableDrawCallback = null;

// The list of visible table columns for each table mode
var compulsoryColumns = ['Date/Time', 'Longitude', 'Latitude', 'QC Flag', 'WOCE Flag'];

// These are regular expression patterns
var visibleColumns = {
	'basic': [/Intake Temp/, /Intake Temp: Mean/, /Salinity/, /Salinity: Mean/, /Equil\. Temp/, /Equil\. Temp: Mean/, /Equil\. Pressure/, /Equil\. Pressure: Mean/, /xH₂O \(True\)/, /fCO₂ Final/],
	'water': [/Intake Temp.*/, /Salinity.*/, /Air Flow.*/, /Water Flow.*/],
	'equilibrator': [/Equil.*/, /Δ.*/, /xH₂O.*/],
	'co2': [/pH₂O/, /.*CO₂.*/]
};

// The list of dates in the current view. Used for searching.
var dateList = null;

// Variables for highlighting selected row in table
var tableScrollRow = null;
var scrollEventTimer = null;
var scrollEventTimeLimit = 300;


// Table selections
var selectedRows = [];

// The row number (in the data file) of the last selected/deselected row, and
// which action was performed.
var lastClickedRow = -1;
var lastClickedAction = DESELECT_ACTION;

// Indicates whether data has changed
var dirty = false;

////////////////////////////////////////////////////////////////////////////////

/*
 * Document Load function.
 * Initialises the splits, adds handler for window resizing,
 * and kicks off drawing the data
 */
$(function() {
	// Make the panel splits
	$('#dataScreenContent').split({orientation: 'horizontal', onDragEnd: function(){resizeContent()} });
	$('#plots').split({orientation: 'vertical', onDragEnd: function(){resizeContent()} });
	tableSplitPercent = '50%';
	plotSplitPercent = '50%';

	
	// Initial loading screens for each panel
	drawLoading($('#plotLeft'));
	drawLoading($('#plotRight'));
	drawLoading($('#tableContent'));
	
	// When the window is resized, scale the panels
	$(window).resize(function() {
		scaleSplits();
	});
	
	// Hide the popup menus when clicking outside them
    $('#dataScreenContent').click(function() {
    	hidePlotPopup();
    });
    
    // Set up change handlers for checkboxes on the plot popup
    $('#plotFieldList')
    .find('input')
    .each(function(index, item) {
    	$(item).change(function(event) {
    		processPlotFieldChange(item);
    	});
    });
    
    // Set up change handlers for checkboxes on the map popup
    $('#mapFieldList')
    .find('input')
    .each(function(index, item) {
    	$(item).change(function(event) {
    		processMapFieldChange(item);
    	});
    });
    
    // Set up the data table options
    
    
    // Draw all the page contents
    drawAllData();
});

/*
 * Refreshes all data objects (both plots and the table)
 */
function drawAllData() {
	drawLoading($('#plotLeft'));
	drawLoading($('#plotRight'));
	drawLoading($('#tableContent'));
    updatePlot('left');
    updatePlot('right');
    drawTable();
}

/*
 * Handles the resizing of individual panels
 */
function resizeContent() {
	tableSplitPercent = '' + $('#dataScreenContent').split().position() / $('#dataScreenContent').height() * 100 + '%';
	plotSplitPercent = '' + $('#plots').split().position() / $('#dataScreenContent').width() * 100 + '%';

	$('#plotLeft').width('100%');
	$('#plotLeft').height($('#plotContainerLeft').height() - 30);
	if (leftGraph != null) {
		leftGraph.resize($('#plotLeft').width(), $('#plotLeft').height() - 15);
	}
	
	$('#plotRight').width('100%');
	$('#plotRight').height('' + $('#plotContainerRight').height() - 30);
	if (rightGraph != null) {
		rightGraph.resize($('#plotRight').width(), $('#plotRight').height() - 15);
	}
	
	// Set the height of the DataTables scrollbody
	// We have to select it by class, but there's only one so we can get away with it
	$('.dataTables_scrollBody').height(calcTableScrollY());
	
	// Resize the table columns
	if (null != jsDataTable) {
		jsDataTable.columns.adjust().draw(false);
	}
}

/*
 * Proportionally resizes the splits when the window is resized
 */
function scaleSplits() {
	$('#dataScreenContent').split().position(tableSplitPercent);
	$('#plots').split().position(plotSplitPercent);
	resizeContent();
}

/*
 * Draw the loading message in the specified div
*/
function drawLoading(el) {
	el.html('<div class="loading">&nbsp;</div>');
}

/*
 * Show the popup for plots
 */
function showPlotPopup(event, target, singleSelection) {
	
	// Set the target information
	plotPopupSingleSelection = singleSelection;
	plotPopupTarget = target;
	
	// Update the inputs with the existing selections
	setPlotPopupInputs();
	
	// Position and show the popup
	leftOffset = ($(window).width() / 2) - ($('#plotFieldList').width() / 2);
	topOffset = ($(window).height() / 2) - ($('#plotFieldList').height() / 2);

	$('#plotFieldList')
	.css({"left": 0, "top": 0})
	.offset({
		"left": leftOffset,
		"top": topOffset,
		"width": $('#plotFieldList').width(),
		"height": $('#plotFieldList').height()
	})
	.zIndex(1000);

	
	$('#plotFieldList').show(0);

    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();

	return false;
}

/*
 * Hide the popup for plots
 */
function hidePlotPopup() {
	$('#plotFieldList').hide(100);
}

/*
 * Process a change of selected field in the plot popup
 */
function processPlotFieldChange(input) {

	var group = getGroupName(input.id);
	
	if (input.checked) {
	
		// If we're in single selection mode,
		// uncheck all others
		if (plotPopupSingleSelection) {
			$('#plotFieldList')
	        .find('input')
	        .each(function(index, item) {
	        	if (item.id != input.id) {
	        		item.checked = false;
	        	}
	        });
			
		} else {
			// We can leave entries in the same group checked.
			$('#plotFieldList')
	        .find('input')
	        .each(function(index, item) {
	        	var itemGroup = getGroupName(item.id);
	        	if (itemGroup != group) {
	        		item.checked = false;
	        	}
	        });
		}
	} else {
		
		var foundCheckedFriends = false;
		
		// See if others are checked in the same group
		$('#plotFieldList')
		.find('input')
		.each(function(index, item) {
			if (item.id != input.id && getGroupName(item.id) == group) {
				if (item.checked) {
					foundCheckedFriends = true;
				}
			}
		});
		
		if (!foundCheckedFriends) {
			input.checked = true;
		}
	}
}

/*
 * Extract the group name from an input's name
 */
function getGroupName(inputName) {
	return inputName.match(/_(.*)_/)[1];
}

/*
 * Extract the column name from an input's name
 */
function getColumnName(inputName) {
	return inputName.match(/_([^_]*)$/)[1];
}

/*
 * Set up the plot popup's inputs according
 * to which plot/axis has been selected
 */
function setPlotPopupInputs() {
	
	// Get the list of inputs to be checked
	var selectedInputs = new Array();
	
	switch (plotPopupTarget) {
	case 'LX': {
		selectedInputs = leftPlotXAxis;
		break;
	}
	case 'LY': {
		selectedInputs = leftPlotYAxis;
		break;
	}
	case 'RX': {
		selectedInputs = rightPlotXAxis;
		break;
	}
	case 'RY': {
		selectedInputs = rightPlotYAxis;
		break;
	}
	}
	
	// Now update the inputs
	$('#plotFieldList')
	.find('input')
	.each(function(index, item) {
		if (selectedInputs.indexOf(item.id) > -1) {
			item.checked = true;
		} else {
			item.checked = false;
		}
	});
}

/*
 * Store the selected plot option in the relevant variable
 */
function savePlotSelection() {
	
	// Get the list of checked inputs
	selectedInputs = new Array();
	
	$('#plotFieldList')
	.find('input')
	.each(function(index, item) {
		if (item.checked) {
			selectedInputs[selectedInputs.length] = item.id;
		}
	});

	switch (plotPopupTarget) {
	case 'LX': {
		leftPlotXAxis = selectedInputs;
		$('#leftUpdate').addClass('highlightButton');
		break;
	}
	case 'LY': {
		leftPlotYAxis = selectedInputs;
		$('#leftUpdate').addClass('highlightButton');
		break;
	}
	case 'RX': {
		rightPlotXAxis = selectedInputs;
		$('#rightUpdate').addClass('highlightButton');
		break;
	}
	case 'RY': {
		$('#rightUpdate').addClass('highlightButton');
		rightPlotYAxis = selectedInputs;
		break;
	}
	}
	
	hidePlotPopup();
	return false;
}

/*
 * Triggers an update of a plot's data.
 * Gets the necessary details and submits them to the server by
 * submitting the hidden form as an ajax request. The event handler
 * redraws the plot when the request completes.
 */
function updatePlot(plot) {
	
	if (plot == 'left') {
		// Destroy the existing graph data
		if (leftGraph != null) {
			leftGraph.destroy();
			leftGraph = null;
		}
		
		drawLoading($('#plotLeft'));
		initLeftPlot();
	} else {
		
		// Destroy the existing graph data
		if (rightGraph != null) {
			rightGraph.destroy();
			rightGraph = null;
		}
		
		drawLoading($('#plotRight'));
		initRightPlot();
	}
	
	return false;
}

function initLeftPlot() {
	// Build the list of columns to be sent to the server
	var columnList = '';
	for (var i = 0; i < leftPlotXAxis.length; i++) {
		columnList += getColumnName(leftPlotXAxis[i]);
		columnList += ';';
	}
	for (var i = 0; i < leftPlotYAxis.length; i++) {
		columnList += getColumnName(leftPlotYAxis[i]);
		if (i < leftPlotYAxis.length - 1) {
			columnList += ';';
		}
	}
	
	// Fill in the hidden form and submit it
	$('#plotDataForm\\:leftColumns').val(columnList);
	$('#plotDataForm\\:leftGetData').click();
	$('#leftUpdate').removeClass('highlightButton');
}

function initRightPlot() {
	// Build the list of columns to be sent to the server
	var columnList = '';
	for (var i = 0; i < rightPlotXAxis.length; i++) {
		columnList += getColumnName(rightPlotXAxis[i]);
		columnList += ';';
	}
	for (var i = 0; i < rightPlotYAxis.length; i++) {
		columnList += getColumnName(rightPlotYAxis[i]);
		if (i < rightPlotYAxis.length - 1) {
			columnList += ';';
		}
	}
	
	// Fill in the hidden form and submit it
	$('#plotDataForm\\:rightColumns').val(columnList);
	$('#plotDataForm\\:rightGetData').click();
	$('#rightUpdate').removeClass('highlightButton');
}

/*
 * Render the left plot
 */
function drawLeftPlot(data) {
	var status = data.status;
	
	if (status == "success") {
		var interaction_model = Dygraph.defaultInteractionModel;
		interaction_model.dblclick = null;

		var graph_options = BASE_GRAPH_OPTIONS;
		graph_options.interactionModel = interaction_model;

		graph_options.xlabel = AXIS_LABELS[leftPlotXAxis[0].match(/[^_]*$/)];
		graph_options.ylabel = AXIS_LABELS[leftPlotYAxis[0].match(/[^_]*$/)];
		graph_options.labels = $('#plotDataForm\\:leftNames').html().split(';');
		graph_options.labelsDiv = 'plotLeftLabels';
	
		// Row, QC Flag and WOCE flag are always invisible
		var columnVisibility = [false, false, false];
		for (var i = 0; i < leftPlotYAxis.length; i++) {
			columnVisibility.push(true);
		}
	
		graph_options.visibility = columnVisibility;
		
		var graph_data = JSON.parse($('#plotDataForm\\:leftData').text());

		if (leftPlotXAxis[0] == 'plot_datetime_dateTime') {
			graph_data = makeJSDates(graph_data);
			graph_options.clickCallback = function(e, x, points) {
				scrollToTableRow(x);
			};

			var plotHighlights = makeHighlights(graph_data);
			if (plotHighlights.length > 0) {
				graph_options.underlayCallback = function(canvas, area, g) {
					for (var i = 0; i < plotHighlights.length; i++) {
						var xPoint = g.toDomXCoord(plotHighlights[i][0]);
						var yPoint = g.toDomYCoord(plotHighlights[i][1]);
						canvas.fillStyle = plotHighlights[i][2];
						canvas.beginPath();
						canvas.arc(xPoint, yPoint, PLOT_FLAG_SIZE, 0, 2 * Math.PI, false);
						canvas.fill();
					}
				}
			} else {
				graph_options.underlayCallback = null;
			}
		
		} else {
			graph_options.clickCallback = null;
			graph_options.underlayCallback = null
		}
	
		leftGraph = new Dygraph (
			document.getElementById('plotLeft'),
			graph_data,
	        	graph_options
		);
		
		resizeContent();
	}
}

/*
 * Render the right plot
 */
function drawRightPlot(data) {
	var status = data.status;
	
	if (status == "success") {
		var interaction_model = Dygraph.defaultInteractionModel;
		interaction_model.dblclick = null;

		var graph_options = BASE_GRAPH_OPTIONS;
		graph_options.interactionModel = interaction_model;

		graph_options.xlabel = AXIS_LABELS[rightPlotXAxis[0].match(/[^_]*$/)];
		graph_options.ylabel = AXIS_LABELS[rightPlotYAxis[0].match(/[^_]*$/)];
		graph_options.labels = $('#plotDataForm\\:rightNames').html().split(';');
		graph_options.labelsDiv = 'plotRightLabels';
	
		// Row, QC Flag and WOCE flag are always invisible
		var columnVisibility = [false, false, false];
		for (var i = 0; i < rightPlotYAxis.length; i++) {
			columnVisibility.push(true);
		}
		
		graph_options.visibility = columnVisibility;
	
		graph_data = JSON.parse($('#plotDataForm\\:rightData').text());
		if (rightPlotXAxis[0] == 'plot_datetime_dateTime') {
			graph_data = makeJSDates(graph_data);
			graph_options.clickCallback = function(e, x, points) {
				scrollToTableRow(x);
			};

			plotHighlights = makeHighlights(graph_data);
			if (plotHighlights.length > 0) {
				graph_options.underlayCallback = function(canvas, area, g) {
					for (var i = 0; i < plotHighlights.length; i++) {
						var xPoint = g.toDomXCoord(plotHighlights[i][0]);
						var yPoint = g.toDomYCoord(plotHighlights[i][1]);
						canvas.fillStyle = plotHighlights[i][2];
						canvas.beginPath();
						canvas.arc(xPoint, yPoint, PLOT_FLAG_SIZE, 0, 2 * Math.PI, false);
						canvas.fill();
					}
				}
			} else {
				graph_options.underlayCallback = null;
			}
		} else {
			graph_options.underlayCallback = null;
			graph_options.clickCallback = null;
		}

		rightGraph = new Dygraph (
			document.getElementById('plotRight'),
			graph_data,
	        	graph_options
		);
		
		resizeContent();
	}
}

/*
 * Begins the redraw of the data table.
 * The HTML table is initialised (with header only), and
 * the DataTables object is created and configured to load
 * its data from the server using the hidden form.
 */
function drawTable() {

	// PUT COLUMN HEADERS IN JS FROM DATASCREENBEAN
	html = '<table id="dataTable" class="display compact nowrap" cellspacing="0" width="100%">';
	html += '<thead>';

	columnHeadings.forEach(heading => {
		html += '<th>';
		html += heading;
		html += '</th>';
	});

	$('#tableContent').html(html);
    
    jsDataTable = $('#dataTable').DataTable( {
    	ordering: false,
    	searching: false,
    	serverSide: true,
    	scroller: {
    		loadingIndicator: true		
    	},
    	scrollY: calcTableScrollY(),
    	ajax: function ( data, callback, settings ) {
    		// Since we've done a major scroll, disable the short
    		// scroll timeout
    		clearTimeout(scrollEventTimer);
    		scrollEventTimer = null;
    		
    		// Store the callback		
    		dataTableDrawCallback = callback;		
    				
    		// Fill in the form inputs		
    		$('#plotDataForm\\:tableDataDraw').val(data.draw);		
    		$('#plotDataForm\\:tableDataStart').val(data.start);		
    		$('#plotDataForm\\:tableDataLength').val(data.length);		
    		
    		// Submit the query to the server		
    		$('#plotDataForm\\:tableGetData').click();		
    	},
    	drawCallback: function (settings) {
    		if (null != tableScrollRow) {
    			highlightRow(tableScrollRow);
    			tableScrollRow = null;
    		}
    	},
    	bInfo: false,
    	rowCallback: function( row, data, index ) {
    		var rowNumber = parseInt(data[getColumnIndex('Row')], 10);
            if ( $.inArray(rowNumber, selectedRows) !== -1 ) {
                $(row).addClass('selected');
            }
            
            $(row).on('click', function(event) {
            	clickRowAction(rowNumber, event.shiftKey);
            });
        },
        columnDefs:[
            // DateTime doesn't wrap
            {"className": "noWrap", "targets": [0]},
            {"className": "centreCol", "targets": getQCColumns()},
            {"className": "numericCol", "targets": getNumericColumns()},
            {"render":
            	function (data, type, row) {
            		return $.format.date(data, 'yyyy-MM-dd HH:mm:ss');
            	},
            	"targets": 0
            },
            {"render":
            	function (data, type, row) {
	                var output = '<div onmouseover="showQCInfoPopup(' + row[getColumnIndex('QC Flag')] + ', \'' + row[getColumnIndex('QC Message')] + '\', this)" onmouseout="hideQCInfoPopup()" class="';
	                output += getFlagClass(data);
	                output += '">';
	                output += getFlagText(data);
	                output += '</div>';
	                return output;
	            },
                "targets": getColumnIndex('QC Flag')
            },
            {"render":
            	function (data, type, row) {
            		var output = '<div class="';
            		output += getFlagClass(data);
            		output += '">';
    				output += getFlagText(data);
    				output += '</div>';
    				return output;
	            },
                "targets": getColumnIndex('WOCE Flag')
            }
        ]
    });
    
    renderTableColumns();
    resizeContent();
    
    // Large table scrolls trigger highlights when the table is redrawn.
    // This handles small scrolls that don't trigger a redraw.
    $('.dataTables_scrollBody').scroll(function() {
		if (null != tableScrollRow) {
			if (scrollEventTimer) {
				clearTimeout(scrollEventTimer);
			}
			
			scrollEventTimer = setTimeout(function() {
				highlightRow(tableScrollRow);
				tableScrollRow = null;
			}, scrollEventTimeLimit);
		}
    });
}

function getNumericColumns() {
	numericCols = new Array();
	for (var i = 0; i < columnHeadings.length; i++) {
		if (columnHeadings[i] != 'Date/Time' && columnHeadings[i] != 'QC Flag' && columnHeadings[i] != 'QC Message' && columnHeadings[i] != 'WOCE Flag' && columnHeadings[i] != 'WOCE Message') {
			numericCols.push(i);
		}
	}
	return numericCols;
}

function getQCColumns() {
	qcColumns = new Array();
	for (var i = 0; i < columnHeadings.length; i++) {
		if (columnHeadings[i] == 'QC Flag' || columnHeadings[i] == 'QC Message' || columnHeadings[i] == 'WOCE Flag' || columnHeadings[i] == 'WOCE Message') {
			qcColumns.push(i);
		}
	}
	return qcColumns;
}

function getColumnIndex(columnName) {
	var index = -1;
	for (var i = 0; i < columnHeadings.length; i++) {
		if (columnHeadings[i] == columnName) {
			index = i;
			break;
		}
	}
	return index;
}

/*
 * Calculate the value of the scrollY entry for the data table
 */
function calcTableScrollY() {
	// 41 is the post-rendered height of the header in FF (as measured on screen). Can we detect it somewhere?
	return $('#data').height() - $('#tableControls').outerHeight() - 50;
}

function renderTableColumns() {
		
	var visibleTableColumns = new Array();
	var hiddenTableColumns = new Array();
	
	// Do the stuff
	for (var i = 0; i < columnHeadings.length; i++) {
		columnVisible = false;
		
		if ($.inArray(columnHeadings[i], compulsoryColumns) != -1) {
			columnVisible = true;
		} else {
			searchColumns = visibleColumns[$('#dataScreenForm\\:tableModeSelector').find(':checked').val()];
			for (j = 0; j < searchColumns.length && !columnVisible; j++) {
				columnVisible = new RegExp(searchColumns[j]).test(columnHeadings[i]);
			}
		}
		
		columnVisible ? visibleTableColumns.push(i) : hiddenTableColumns.push(i);
	}
	
	// Update the table
	jsDataTable.columns(visibleTableColumns).visible(true, false);
	jsDataTable.columns(hiddenTableColumns).visible(false, false);
	jsDataTable.columns.adjust().draw( false );
}

function getFlagText(flag) {
    var flagText = "";

    if (flag == '-1001') {
        flagText = 'Needs Flag';
    } else if (flag == '-1002') {
        flagText = 'Ignore';
    } else if (flag == '-2') {
        flagText = 'Assumed Good';
    } else if (flag == '2') {
        flagText = 'Good';
    } else if (flag == '3') {
        flagText = 'Questionable';
    } else if (flag == '4') {
        flagText = 'Bad';
    } else if (flag == '44') {
    	flagText = 'Fatal';
    } else {
        flagText = 'Needs Flag';
    }

    return flagText;
}

function getFlagClass(flag) {
    var flagClass = "";

    if (flag == '-1001') {
        flagClass = 'needsFlagging';
    } else if (flag == '-1002') {
        flagClass = 'ignore';
    } else if (flag == '-2') {
        flagClass = 'assumedGood';
    } else if (flag == '2') {
        flagClass = 'good';
    } else if (flag == '3') {
        flagClass = 'questionable';
    } else if (flag == '4' || flag == '44') {
        flagClass = 'bad';
    } else {
        flagClass = 'needsFlagging';
    }

    return flagClass;
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

function scrollToTableRow(milliseconds) {
	var tableRow = dateList.indexOf(milliseconds);
	if (tableRow >= 0) {
		jsDataTable.scroller().scrollToRow(tableRow - 2);
		
		// Because we scroll to the row - 2, we know that the
		// row we want to highlight is the third row
		tableScrollRow = tableRow;
		
		// The highlight is done as part of the table draw callback
	}
}


function makeHighlights(plotData) {
	
	var highlights = [];
	
	var currentFlag = FLAG_GOOD;
	var highlightColor = null;
	
	for (var i = 0; i < plotData.length; i++) {
		
		if (Math.abs(plotData[i][PLOT_WOCEFLAG_INDEX]) != FLAG_GOOD) {
		
			switch (plotData[i][PLOT_WOCEFLAG_INDEX]) {
			case FLAG_BAD:
			case FLAG_FATAL: {
				highlightColor = 'rgba(255, 0, 0, 1)';
				break;
			}
			case FLAG_QUESTIONABLE: {
				highlightColor = 'rgba(216, 177, 0, 1)';
				break;
			}
			case FLAG_NEEDS_FLAG: {
				highlightColor = 'rgba(129, 127, 255, 1)';
				break;
			}
			case FLAG_IGNORED: {
				highlightColor = 'rgba(225, 225, 225, 1)';
				break;
			}
			}
			
			for (j = PLOT_FIRST_Y_INDEX; j < plotData[i].length; j++) {
				if (plotData[i][j] != null) {
					highlights.push([plotData[i][0], plotData[i][j], highlightColor]);
				}
			}
		}
	}
		
	return highlights;
}

function showQCInfoPopup(qcFlag, qcMessage, target) {

    $('#qcInfoPopup').stop(true, true);

    if (qcMessage != "") {

        var content = '';
	    content += '<div class="qcInfoMessage ';

	    switch (qcFlag) {
	    case 3: {
	    	content += 'questionable';
	    	break;
	    }
	    case 4:
	    case 44: {
	    	content += 'bad';
	    	break;
    	}
	    }

	    content += '">';
	    content += qcMessage;
	    content += '</div>';

    	$('#qcInfoPopup')
          .html(content)
          .css({"left": 0, "top": 0})
          .offset({"left": $(target).position().left - $('#qcInfoPopup').width() - 10, "top": $(target).offset().top - 3})
          .show('slide', {direction: 'right'}, 100);
   }
 }

function hideQCInfoPopup() {
    $('#qcInfoPopup').stop(true, true);
    $('#qcInfoPopup').hide('slide', {direction: 'right'}, 100);
}

/*
 * Process row clicks as selections/deselections
 */
function clickRowAction(rowNumber, shiftClick) {
	
	// We only do something if the row is selectable
	if ($.inArray(rowNumber, selectableRows) != -1) {
		
		var action = lastClickedAction;
		var actionRows = [rowNumber];
		
		if (!shiftClick) {
			if ($.inArray(rowNumber, selectedRows) != -1) {
				action = DESELECT_ACTION;
			} else {
				action = SELECT_ACTION;
			}
		} else {
			actionRows = getRowsInRange(lastClickedRow, rowNumber);
		}
		
		if (action == SELECT_ACTION) {
			addRowsToSelection(actionRows);
		} else {
			removeRowsFromSelection(actionRows);
		}
		
		selectionUpdated();
		lastClickedRow = rowNumber;
		lastClickedAction = action;
	}
}

function addRowsToSelection(rows) {
	var rowsIndex = 0;
	var selectionIndex = 0;
	
	while (selectionIndex < selectedRows.length && rowsIndex < rows.length) {
		while (selectedRows[selectionIndex] > rows[rowsIndex]) {
			selectedRows.splice(selectionIndex, 0, rows[rowsIndex]);
			selectionIndex++;
			rowsIndex++;
		}
		
		if (selectedRows[selectionIndex] == rows[rowsIndex]) {
			rowsIndex++;
		}
		selectionIndex++;
	}
	
	if (rowsIndex < rows.length) {
		selectedRows = selectedRows.concat(rows.slice(rowsIndex));
	}
}

function removeRowsFromSelection(rows) {
	
	var rowsIndex = 0;
	var selectionIndex = 0;
	
	while (selectionIndex < selectedRows.length && rowsIndex < rows.length) {
		while (selectedRows[selectionIndex] == rows[rowsIndex]) {
			selectedRows.splice(selectionIndex, 1);
			rowsIndex++;
			if (rowsIndex == rows.length || selectionIndex == selectedRows.length) {
				break;
			}
		}
		selectionIndex++;
	}
}

function getRowsInRange(startRow, endRow) {
	
	var rows = [];
	
	var step = 1;
	if (endRow < startRow) {
		step = -1;
	}
	
	var startIndex = $.inArray(startRow, selectableRows);
	var currentIndex = startIndex;
	
	while (selectableRows[currentIndex] != endRow) {
		currentIndex = currentIndex + step;
		rows.push(selectableRows[currentIndex]);
	}
	
	if (step == -1) {
		rows = rows.reverse();
	}
	
	return rows;
}

function selectionUpdated() {

	// Update the displayed rows
	var rows = jsDataTable.rows()[0];
	for (var i = 0; i < rows.length; i++) {
		var row = jsDataTable.row(i);
		if ($.inArray(row.data()[getColumnIndex('Row')], selectedRows) > -1) {
			$(row.node()).addClass('selected');
		} else {
			$(row.node()).removeClass('selected');
		}
	}
	
	// Update the selected rows counter
	$('#selectedRowsCount').html(selectedRows.length);
	
	// Update the buttons
	if (selectedRows.length == 0) {
		$('#acceptFlagsButton').prop('disabled', true).addClass('disabledButton');
		$('#overrideFlagsButton').prop('disabled', true).addClass('disabledButton');
	} else {
		$('#acceptFlagsButton').prop('disabled', false).removeClass('disabledButton');
		$('#overrideFlagsButton').prop('disabled', false).removeClass('disabledButton');
	}
}

function clearSelection() {
	jsDataTable.rows(selectedRows).deselect();
	selectedRows = [];
	selectionUpdated();
}

function acceptQCFlags() {
	$('#dataScreenForm\\:selectedRows').val(selectedRows);
	$('#dataScreenForm\\:acceptQCFlags').click();
}

function qcFlagsAccepted(data) {
	if (data.status == 'success') {
		
		var qcFlagColumn = getColumnIndex('QC Flag');
		var qcMessageColumn = getColumnIndex('QC Message');
		var woceFlagColumn= getColumnIndex('WOCE Flag');
		var woceMessageColumn = getColumnIndex('WOCE Message');
		var rowColumn = getColumnIndex('Row');
		
		var rows = jsDataTable.rows()[0];
		for (var i = 0; i < rows.length; i++) {
			var row = jsDataTable.row(i);
			if ($.inArray(row.data()[rowColumn], selectedRows) > -1) {
				jsDataTable.cell(i, woceMessageColumn).data(jsDataTable.cell(row, qcMessageColumn).data());
				jsDataTable.cell(i, woceFlagColumn).data(jsDataTable.cell(row, qcFlagColumn).data());
			}
		}
		
		if (!dirty) {
			dirty = true;
			$('#dataScreenForm\\:finishButton').val('Finish*');
		}
		clearSelection();
	}
}

function woceFlagsUpdated(data) {
	if (data.status == 'success') {
		
		var woceFlagColumn = getColumnIndex('WOCE Flag');
		var woceMessageColumn = getColumnIndex('WOCE Message');
		var rowColumn = getColumnIndex('Row');
		
		var rows = jsDataTable.rows()[0];
		for (var i = 0; i < rows.length; i++) {
			var row = jsDataTable.row(i);
			if ($.inArray(row.data()[rowColumn], selectedRows) > -1) {
				jsDataTable.cell(i, woceMessageColumn).data($('#dataScreenForm\\:woceComment').val());
				jsDataTable.cell(i, woceFlagColumn).data($('#dataScreenForm\\:woceFlag').val());
			}
		}
		
		if (!dirty) {
			dirty = true;
			$('#dataScreenForm\\:finishButton').val('Finish*');
		}
		clearSelection();
	} 
}

function woceFlagClick() {
    if ($('#woceSelectMenu').is(':hidden')) {
    	showWoceMenu();
    } else {
    	hideWoceMenu();
    }
}

function showWoceMenu() {
    $('#woceSelectMenu').show('slide', {direction: 'up'}, 100);
}

function hideWoceMenu() {
    $('#woceSelectMenu').hide('slide', {direction: 'up'}, 100);
}

function startWoceComment() {
	$('#dataScreenForm\\:selectedRows').val(selectedRows);
	$('#dataScreenForm\\:generateWoceComments').click();
}

function showWoceCommentDialog(data) {

	var status = data.status;		
	if (status == "success") {
		var woceRowHtml = selectedRows.length.toString() + ' row';
		if (selectedRows.length > 1) {
			woceRowHtml += 's';
		} 
		
		$('#woceRowCount').html(woceRowHtml);
		
		var worstSelectedFlag = parseInt($('#dataScreenForm\\:worstSelectedFlag').val());
		
	    woceSelection(worstSelectedFlag);
	
	    var commentsString = '';
	    var comments = JSON.parse($('#dataScreenForm\\:woceCommentList').val());
	    for (var i = 0; i < comments.length; i++) {
	    	var comment = comments[i];
	    	commentsString += comment[0];
	    	commentsString += ' (' + comment[2] + ')';
	    	if (i < comments.length - 1) {
	    		commentsString += '\n';
	    	}
	    }
	    
	    $('#dataScreenForm\\:woceComment').attr('disabled', (worstSelectedFlag == FLAG_IGNORED));
	    $('#dataScreenForm\\:woceComment').val(commentsString);
	    $('#woceCommentDialog').fadeIn(100);
	}
}

function saveWoceComment() {
	$('#dataScreenForm\\:selectedRows').val(selectedRows);
	$('#dataScreenForm\\:applyWoceFlag').click();
	hideWoceDialog();
}

function cancelWoceComment() {
	hideWoceDialog();
}

function hideWoceDialog() {
    $('#woceCommentDialog').fadeOut(100);
    hideWoceMenu();
}

function woceSelection(flagValue) {
	$('#woceCommentDialogFlag').html(getFlagText(flagValue))
	  .removeClass().addClass(getFlagClass(flagValue));
	
	$('#dataScreenForm\\:woceFlag').val(flagValue);
	
	hideWoceMenu();
}

function setWoceSelectedFlag(flagValue) {
	$('#woceCommentDialogFlag').html(getFlagText(flagValue));
}

/*
 * Called when table data has been downloaded from the server.		
 * The previously stored callback function is triggered with		
 * the data from the server.		
 */		
function tableDataDownload(data) {		
			
	var status = data.status;		
	if (status == "success") {
		dataTableDrawCallback( {		
            draw: $('#plotdDataForm\\:tableDataDraw').val(),		
            data: JSON.parse($('#plotDataForm\\:tableJsonData').val()),		
            recordsTotal: $('#plotDataForm\\:recordCount').val(),		
            recordsFiltered: $('#plotDataForm\\:recordCount').val()		
		});
	}
}		

function highlightRow(tableRow) {
	setTimeout(function() {
		var rowNode = $('#row' + tableRow)[0];
		$(rowNode).css('animationName', 'rowFlash').css('animationDuration', '1s');
		setTimeout(function() {
			$(rowNode).css('animationName', '');
		}, 1000);
	}, 100);
}

function zoomOut(g) {
	g.updateOptions({
	    dateWindow: null,
	    valueRange: null
	});
	
	return false;
}

function showLeftMap() {
	$('#plotLeft').hide();
	$('#plotControlsLeft').hide();
	$('#mapLeft').show();
	$('#mapControlsLeft').show();
	initLeftMap();
	resizeContent();
	return false;
}

function showLeftPlot() {
	$('#mapLeft').hide();
	$('#mapControlsLeft').hide();
	$('#plotLeft').show();
	$('#plotControlsLeft').show();
	resizeContent();
	return false;
}

function showRightMap() {
	$('#plotRight').hide();
	$('#plotControlsRight').hide();
	$('#mapRight').show();
	$('#mapControlsRight').show();
	initRightMap();
	resizeContent();
	return false;
}

function showRightPlot() {
	$('#mapRight').hide();
	$('#mapControlsRight').hide();
	$('#plotRight').show();
	$('#plotControlsRight').show();
	resizeContent();
	return false;
}

function setMapPopupInputs() {

	var selectedInput = null;
	
	switch (mapPopupTarget) {
	case 'L': {
		selectedInput = leftMapVar;
		break;
	}
	case 'R': {
		selectedInput = rightMapVar;
		break;
	}
	}
	
	// Now update the inputs
	$('#mapFieldList')
	.find('input')
	.each(function(index, item) {
		if (item.id == selectedInput) {
			item.checked = true;
		} else {
			item.checked = false;
		}
	});
}

function showMapPopup(event, target) {
	// Set the target information
	mapPopupTarget = target;
	
	// Update the inputs with the existing selections
	setMapPopupInputs();
	
	// Position and show the popup
	leftOffset = ($(window).width() / 2) - ($('#mapFieldList').width() / 2);
	topOffset = ($(window).height() / 2) - ($('#mapFieldList').height() / 2);

	$('#mapFieldList')
	.css({"left": 0, "top": 0})
	.offset({
		"left": leftOffset,
		"top": topOffset,
		"width": $('#mapFieldList').width(),
		"height": $('#mapFieldList').height()
	})
	.zIndex(1000);

	
	$('#mapFieldList').show(0);

    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();

	return false;
}

function hideMapPopup() {
	$('#mapFieldList').hide(100);
}

function processMapFieldChange(input) {
	$('#mapFieldList')
	.find('input')
	.each(function(index, item) {
		if (item.id != input.id) {
			item.checked = false;
		}
	});
}

function saveMapSelection() {
	
	// Get the list of checked inputs
	selectedInput = null;
	
	$('#mapFieldList')
	.find('input')
	.each(function(index, item) {
		if (item.checked) {
			selectedInput = item.id;
		}
	});

	switch (mapPopupTarget) {
	case 'L': {
		leftMapVar = selectedInput;
		updateMap('left');
		break;
	}
	case 'R': {
		rightMapVar = selectedInput;
		updateMap('right');
		break;
	}
	}
	
	hideMapPopup();
	return false;
}

function updateMap(target) {
	if (target == 'left') {
		initLeftMap();
	} else {
		initRightMap();
	}
}

function initLeftMap() {
	
	if (leftMap == null) {
		leftMap = new ol.Map({
	 		target: 'mapLeft',
	 		layers: [
	    		new ol.layer.Tile({
	        		source: mapSource
	    		}),
	  		],
	  		view: new ol.View({
	    		center: ol.proj.fromLonLat([10, 45]),
	    		zoom: 4,
	    		minZoom: 2
	  		}),
		});
	}
	
	// Fill in the hidden form and submit it
	$('#plotDataForm\\:leftMapColumn').val(getColumnName(leftMapVar));
	$('#plotDataForm\\:leftGetMapData').click();
}

function drawLeftMap(data) {
	var status = data.status;
	
	if (status == "success") {

		if (null != leftMapDataLayer) {
			leftMap.removeLayer(leftMapDataLayer);
			leftMapDataLayer = null;
		}

		var mapData = JSON.parse($('#plotDataForm\\:leftMapData').html());

		var dataMin = 9000000;
		var dataMax = -9000000;
		
		for (var i = 1; i < mapData.length; i++) {
			if (mapData[i][5] < dataMin) {
				dataMin = mapData[i][5];
			}
			
			if (mapData[i][5] > dataMax) {
				dataMax = mapData[i][5];
			}
		} 
		
		colorScale.setValueRange(dataMin, dataMax);
		

		var layerFeatures = new Array();

		for (var i = 0; i < mapData.length; i++) {
			var featureData = mapData[i];
			
			var feature = new ol.Feature({
				geometry: new ol.geom.Point([featureData[0], featureData[1]]).transform(ol.proj.get("EPSG:4326"), mapSource.getProjection())
			});
			
			feature.setStyle(new ol.style.Style({
				image: new ol.style.Circle({
					radius: 5,
					fill: new ol.style.Fill({
						color: colorScale.getColor(featureData[5])
					})
				})
			}));
			
			layerFeatures.push(feature);
		}
		
		leftMapDataLayer = new ol.layer.Vector({
			source: new ol.source.Vector({
				features: layerFeatures
			})
		})
		
		leftMap.addLayer(leftMapDataLayer);
	}
}

function initRightMap() {
	if (rightMap == null) {
		rightMap = new ol.Map({
	 		target: 'mapRight',
	 		layers: [
	    		new ol.layer.Tile({
	        		source: mapSource
	    		}),
	  		],
	  		view: new ol.View({
	    		center: ol.proj.fromLonLat([10, 45]),
	    		zoom: 4,
	    		minZoom: 2
	  		}),
		});
	}
}