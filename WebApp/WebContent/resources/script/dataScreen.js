/*
 * Javascript for the Data Screen
 */

/*
 * MAIN VARIABLES
 */

// Keeps track of the split positions as a percentage of the
// full data area
var tableSplitPercent = 0;
var plotSplitPercent = 0;

// The X Axis popup can only have one item selected;
// The Y axis can have more than one. This controls
// the selection limits
var plotPopupSingleSelection = true;

// Specifies the target plot (1/2) and axis (X/Y) for the popup
var plotPopupTarget = '1X';

// The selected paramters for the plots and maps
var plot1XAxis = ['plot_datetime_dateTime'];
var plot1YAxis = ['plot_eqt_eqtMean', 'plot_eqt_eqt1'];
var plot1Map = 'plot_co2_fCO2Final';

var plot2XAxis = ['plot_datetime_dateTime'];
var plot2YAxis = ['plot_co2_fCO2Final'];
var plot2Map = 'plot_intaketemp_intakeTempMean';

// Variables for the plots
var leftGraph = null;
var rightGraph = null;

////////////////////////////////////////////////////////////////////////////////

/*
 * Document Load function.
 * Initialises the splits, and adds handler for window resizing
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
    
    updatePlot('left');
    updatePlot('right');
});


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
	$('#plotRightContent').height('' + $('#plotContainerRight').height() - 30 + 'px');
	if (rightGraph != null) {
		rightGraph.resize($('#plotRightContent').width(), $('#plotRightContent').height() - 15);
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
    var button = '#' + event.target.id;
	
	container = $(button).parents('[id^=controls]')[0];

	leftOffset = $(button).position().left + $(container).position().left;
	topOffset = $(button).position().top + $(container).position().top;
	
	$('#plotFieldList')
	.css({"left": 0, "top": 0})
	.offset({
		"left": leftOffset,
		"top": topOffset,
		"width": $('#plotFieldList').width(),
		"height": $('#plotFieldList').height()
	})
	.zIndex(1000);

	
	$('#plotFieldList').show(100);

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
	case '1X': {
		selectedInputs = plot1XAxis;
		break;
	}
	case '1Y': {
		selectedInputs = plot1YAxis;
		break;
	}
	case '2X': {
		selectedInputs = plot2XAxis;
		break;
	}
	case '2Y': {
		selectedInputs = plot2YAxis;
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
	case '1X': {
		plot1XAxis = selectedInputs;
		break;
	}
	case '1Y': {
		plot1YAxis = selectedInputs;
		break;
	}
	case '2X': {
		plot2XAxis = selectedInputs;
		break;
	}
	case '2Y': {
		plot2YAxis = selectedInputs;
		break;
	}
	}
	
	hidePlotPopup();
	return false;
}

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
		for (i = 0; i < plot1XAxis.length; i++) {
			columnList += getColumnName(plot1XAxis[i]);
			columnList += ';';
		}
		for (i = 0; i < plot1YAxis.length; i++) {
			columnList += getColumnName(plot1YAxis[i]);
			if (i < plot1YAxis.length - 1) {
				columnList += ';';
			}
		}
		
		// Fill in the hidden form and submit it
		$('#plotDataForm\\:leftColumns').val(columnList);
		$('#plotDataForm\\:leftGetData').click();
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
		for (i = 0; i < plot2XAxis.length; i++) {
			columnList += getColumnName(plot2XAxis[i]);
			columnList += ';';
		}
		for (i = 0; i < plot2YAxis.length; i++) {
			columnList += getColumnName(plot2YAxis[i]);
			if (i < plot2YAxis.length - 1) {
				columnList += ';';
			}
		}
		
		// Fill in the hidden form and submit it
		$('#plotDataForm\\:rightColumns').val(columnList);
		$('#plotDataForm\\:rightGetData').click();
	}
	
	
	return false;
}

function drawLeftPlot(data) {
	var status = data.status;
	
	if (status == "success") {
		leftGraph = new Dygraph (
			document.getElementById('plotLeftContent'),
	        $('#plotDataForm\\:leftData').text(),
	        {
	          drawPoints: true,
	          strokeWidth: 0.0,
	          labelsUTC: true,
	          labelsSeparateLine: true
	        }
		);
		
		resizeContent();
	}
}

function drawRightPlot(data) {
	var status = data.status;
	
	if (status == "success") {
		rightGraph = new Dygraph (
			document.getElementById('plotRightContent'),
	        $('#plotDataForm\\:rightData').text(),
	        {
	          drawPoints: true,
	          strokeWidth: 0.0,
	          labelsUTC: true,
	          labelsSeparateLine: true
	        }
		);
		
		resizeContent();
	}
}