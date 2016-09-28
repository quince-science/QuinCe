/*
 * Javascript for the Data Screen
 */

/*
 * MAIN VARIABLES
 */

var PLOT_ROW_INDEX = 1;
var PLOT_QCFLAG_INDEX = 2;
var PLOT_WOCEFLAG_INDEX = 3;

var FLAG_GOOD = 2;
var FLAG_ASSUMED_GOOD = -2;
var FLAG_QUESTIONABLE = 3;
var FLAG_BAD = 4;
var FLAG_NEEDS_FLAG = -1001;
var FLAG_IGNORED = -1002;

var BASE_GRAPH_OPTIONS = {
    drawPoints: true,
    strokeWidth: 0.0,
    labelsUTC: true,
    labelsSeparateLine: true,
    digitsAfterDecimal: 2,
    animatedZooms: true,
    pointSize: 2,
    highlightCircleSize: 5,
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
		'eqp1': 'Pressure (hPa)',
		'eqp2': 'Pressure (hPa)',
		'eqp3': 'Pressure (hPa)',
		'airFlow1': 'Flow (ml/min)',
		'airFlow2': 'Flow (ml/min)',
		'airFlow3': 'Flow (ml/min)',
		'waterFlow1': 'Flow (ml/min)',
		'waterFlow2': 'Flow (ml/min)',
		'waterFlow3': 'Flow (ml/min)',
		'eqpMean': 'Pressure (hPa)',
		'moistureMeasured': 'Moisture (%)',
		'moistureTrue': 'Moisture (%)',
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

// Specifies the target plot (1/2) and axis (X/Y) for the popup
var plotPopupTarget = 'LX';

// The selected parameters for the plots and maps
var leftPlotXAxis = ['plot_datetime_dateTime'];
var leftPlotYAxis = ['plot_intaketemp_intakeTempMean', 'plot_intaketemp_intakeTemp1', 'plot_intaketemp_intakeTemp2', 'plot_intaketemp_intakeTemp3'];
var leftMap = 'plot_co2_fCO2Final';

var rightPlotXAxis = ['plot_datetime_dateTime'];
var rightPlotYAxis = ['plot_co2_fCO2Final'];
var rightMap = 'plot_intaketemp_intakeTempMean';

// Variables for the plots
var leftGraph = null;
var rightGraph = null;

// The data table
var jsDataTable = null;

// The callback function for the DataTables drawing call
var dataTableDrawCallback = null;

// The list of visible table columns for each table mode
var compulsoryColumns = ['Date/Time', 'Longitude', 'Latitude', 'QC Flag', 'WOCE Flag'];

// These are regular expression patterns
var visibleColumns = {
	'basic': [/Intake Temp/, /Intake Temp: Mean/, /Salinity/, /Salinity: Mean/, /Equil\. Temp/, /Equil\. Temp: Mean/, /Equil\. Pressure/, /Equil\. Pressure: Mean/, /Moisture \(True\)/, /fCO₂ Final/],
	'water': [/Intake Temp.*/, /Salinity.*/],
	'equilibrator': [/Equil.*/, /Moisture.*/],
	'co2': [/pH₂O/, /.*CO₂.*/]
};

// The viewing mode for the table
var tableMode = 'basic';

// The list of dates in the current view. Used for searching.
var dateList = null;

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
	drawLoading($('#plotLeftContent'));
	drawLoading($('#plotRightContent'));
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
    
    // Set up the data table options
    
    
    // Draw all the page contents
    drawAllData();
});

/*
 * Refreshes all data objects (both plots and the table)
 */
function drawAllData() {
	drawLoading($('#plotLeftContent'));
	drawLoading($('#plotRightContent'));
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

	$('#plotLeftContent').width('100%');
	$('#plotLeftContent').height($('#plotContainerLeft').height() - 30);
	if (leftGraph != null) {
		leftGraph.resize($('#plotLeftContent').width(), $('#plotLeftContent').height() - 15);
	}
	
	$('#plotRightContent').width('100%');
	$('#plotRightContent').height('' + $('#plotContainerRight').height() - 30);
	if (rightGraph != null) {
		rightGraph.resize($('#plotRightContent').width(), $('#plotRightContent').height() - 15);
	}
	
	// Set the height of the DataTables scrollbody
	// We have to select it by class, but there's only one so we can get away with it
	$('.dataTables_scrollBody').height(calcTableScrollY());
	
	// Resize the table columns
	jsDataTable.columns.adjust().draw(false);
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
		// Replace the existing plot with the loading animation
		drawLoading($('#plotLeftContent'));
		
		// Destroy the existing graph data
		if (leftGraph != null) {
			leftGraph.destroy();
			leftGraph = null;
		}
		
		// Build the list of columns to be sent to the server
		var columnList = '';
		for (i = 0; i < leftPlotXAxis.length; i++) {
			columnList += getColumnName(leftPlotXAxis[i]);
			columnList += ';';
		}
		for (i = 0; i < leftPlotYAxis.length; i++) {
			columnList += getColumnName(leftPlotYAxis[i]);
			if (i < leftPlotYAxis.length - 1) {
				columnList += ';';
			}
		}
		
		// Fill in the hidden form and submit it
		$('#plotDataForm\\:leftColumns').val(columnList);
		$('#plotDataForm\\:leftGetData').click();
		$('#leftUpdate').removeClass('highlightButton');
	} else {
		// Replace the existing plot with the loading animation
		drawLoading($('#plotRightContent'));
		
		// Destroy the existing graph data
		if (rightGraph != null) {
			rightGraph.destroy();
			rightGraph = null;
		}
		
		// Build the list of columns to be sent to the server
		var columnList = '';
		for (i = 0; i < rightPlotXAxis.length; i++) {
			columnList += getColumnName(rightPlotXAxis[i]);
			columnList += ';';
		}
		for (i = 0; i < rightPlotYAxis.length; i++) {
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
	
	
	return false;
}

/*
 * Render the left plot
 */
function drawLeftPlot(data) {
	var status = data.status;
	
	if (status == "success") {
		var graph_options = BASE_GRAPH_OPTIONS;
		graph_options.xlabel = AXIS_LABELS[leftPlotXAxis[0].match(/[^_]*$/)];
		graph_options.ylabel = AXIS_LABELS[leftPlotYAxis[0].match(/[^_]*$/)];
	
		// Row, QC Flag and WOCE flag are always invisible
		var columnVisibility = [false, false, false];
		for (var i = 0; i < leftPlotYAxis.length; i++) {
			columnVisibility.push(true);
		}
	
		graph_options.visibility = columnVisibility;
		
		graph_data = JSON.parse($('#plotDataForm\\:leftData').text());
		leftPlotHighlights = makeHighlights(graph_data);
		
		if (leftPlotXAxis[0] == 'plot_datetime_dateTime') {
			graph_data = makeJSDates(graph_data);
			graph_options.clickCallback = function(e, x, points) {
				scrollToTableRow(x);
			};

			plotHighlights = makeHighlights(graph_data);
			if (plotHighlights.length > 0) {
				graph_options.underlayCallback = function(canvas, area, g) {
					for (var i = 0; i < plotHighlights.length; i++) {
						var canvas_left_x = g.toDomXCoord(plotHighlights[i][0]);
			            var canvas_right_x = g.toDomXCoord(plotHighlights[i][1]);
			            var canvas_width = canvas_right_x - canvas_left_x;
			            canvas.fillStyle = plotHighlights[i][2];
			            canvas.fillRect(canvas_left_x, area.y, canvas_width, area.h);
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
			document.getElementById('plotLeftContent'),
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
		var graph_options = BASE_GRAPH_OPTIONS;
		graph_options.xlabel = AXIS_LABELS[rightPlotXAxis[0].match(/[^_]*$/)];
		graph_options.ylabel = AXIS_LABELS[rightPlotYAxis[0].match(/[^_]*$/)];
	
		// Row, QC Flag and WOCE flag are always invisible
		var columnVisibility = [false, false, false];
		for (var i = 0; i < rightPlotYAxis.length; i++) {
			columnVisibility.push(true);
		}
		
		graph_options.visibility = columnVisibility;
	
		graph_data = JSON.parse($('#plotDataForm\\:rightData').text());
		if (rightPlotXAxis[0] == 'plot_datetime_dateTime') {
			graph_data = makeJSDates(graph_data);
			rightPlotHighlights = makeHighlights(graph_data);
			graph_options.clickCallback = function(e, x, points) {
				scrollToTableRow(x);
			};

			plotHighlights = makeHighlights(graph_data);
			if (plotHighlights.length > 0) {
				graph_options.underlayCallback = function(canvas, area, g) {
					for (var i = 0; i < plotHighlights.length; i++) {
						var canvas_left_x = g.toDomXCoord(plotHighlights[i][0]);
			            var canvas_right_x = g.toDomXCoord(plotHighlights[i][1]);
			            var canvas_width = canvas_right_x - canvas_left_x;
			            canvas.fillStyle = plotHighlights[i][2];
			            canvas.fillRect(canvas_left_x, area.y, canvas_width, area.h);
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
			document.getElementById('plotRightContent'),
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
    	bInfo: false,
    	ajax: function ( data, callback, settings ) {
            
    		// Store the callback
    		dataTableDrawCallback = callback;

    		// Fill in the form inputs
    		$('#plotDataForm\\:tableDataDraw').val(data.draw);
    		$('#plotDataForm\\:tableDataStart').val(data.start);
    		$('#plotDataForm\\:tableDataLength').val(data.length);
    		
    		// Submit the query to the server
    		$('#plotDataForm\\:tableGetData').click();
        },
        columnDefs:[
            // DateTime doesn't wrap
            {"className": "noWrap", "targets": [0]},
            {"className": "centreCol", "targets": getQCColumns()},
            {"className": "numericCol", "targets": getNumericColumns()},
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
            }
        ]
    });
    
    renderTableColumns();
}

function getNumericColumns() {
	numericCols = new Array();
	for (i = 0; i < columnHeadings.length; i++) {
		if (columnHeadings[i] != 'Date/Time' && columnHeadings[i] != 'QC Flag' && columnHeadings[i] != 'QC Message' && columnHeadings[i] != 'WOCE Flag' && columnHeadings[i] != 'WOCE Message') {
			numericCols.push(i);
		}
	}
	return numericCols;
}

function getQCColumns() {
	qcColumns = new Array();
	for (i = 0; i < columnHeadings.length; i++) {
		if (columnHeadings[i] == 'QC Flag' || columnHeadings[i] == 'QC Message' || columnHeadings[i] == 'WOCE Flag' || columnHeadings[i] == 'WOCE Message') {
			qcColumns.push(i);
		}
	}
	return qcColumns;
}

function getColumnIndex(columnName) {
	var index = -1;
	for (i = 0; i < columnHeadings.length; i++) {
		if (columnHeadings[i] == columnName) {
			index = i;
			break;
		}
	}
	return index;
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

/*
 * Calculate the value of the scrollY entry for the data table
 */
function calcTableScrollY() {
	// 41 is the post-rendered height of the header in FF (as measured on screen). Can we detect it somewhere?
	return $('#data').height() - $('#tableControls').outerHeight() - 41;
}

function renderTableColumns() {
		
	var visibleTableColumns = new Array();
	var hiddenTableColumns = new Array();
	
	// Do the stuff
	for (i = 0; i < columnHeadings.length; i++) {
		columnVisible = false;
		
		if ($.inArray(columnHeadings[i], compulsoryColumns) != -1) {
			columnVisible = true;
		} else {
			searchColumns = visibleColumns[tableMode];
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

function changeTableMode(event) {
	tableMode = event.target.value;
	renderTableColumns();
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
    } else if (flag == '4') {
        flagClass = 'bad';
    } else {
        flagClass = 'needsFlagging';
    }

    return flagClass;
}

function makeJSDates(data) {
	
	dateList = new Array();
	
	for (i = 0; i < data.length; i++) {
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
		jsDataTable.scroller().scrollToRow(tableRow);
	}
}


function makeHighlights(plotData) {
	
	var highlights = [];
	
	var currentFlag = FLAG_GOOD;
	var highlightStart = -1;
	var highlightEnd = -1;
	var highlightColor = null;
	
	for (var i = 0; i < plotData.length; i++) {
		var woceFlag = plotData[i][PLOT_WOCEFLAG_INDEX];
		
		if (woceFlag != currentFlag) {
			if (highlightStart > -1) {
				highlightEnd = plotData[i][0];
				highlights.push([highlightStart, highlightEnd, highlightColor]);
			}
			
			if (Math.abs(woceFlag) == FLAG_GOOD) {
				highlightStart = -1;
			} else {
				highlightStartIndex = i - 1;
				if (highlightStartIndex < 0) {
					highlightStartIndex = 0;
				}
				
				highlightStart = plotData[highlightStartIndex][0];
				switch (woceFlag) {
				case FLAG_BAD: {
					highlightColor = 'rgba(255, 0, 0, 1)';
					break;
				}
				case FLAG_QUESTIONABLE: {
					highlightColor = 'rgba(216, 177, 0, 1)';
					break;
				}
				case FLAG_NEEDS_FLAG: {
					highlightColor = 'rgba(69, 66, 255, 1)';
					break;
				}
				case FLAG_IGNORED: {
					highlightColor = 'rgba(225, 225, 225, 1)';
					break;
				}
				}
			}

			currentFlag = woceFlag;
		}
	}
	
	if (highlightStart != -1) {
		highllightEnd = plotData[plotData.length - 1][0];
		highlights.push([highlightStart, highlightEnd, highlightColor]);
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
	    case 4: {
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
          .show('slide', {direction: 'right'}, 100);;
   }
 }

 function hideQCInfoPopup() {
     $('#qcInfoPopup').stop(true, true);
     $('#qcInfoPopup').hide('slide', {direction: 'right'}, 100);
 }

