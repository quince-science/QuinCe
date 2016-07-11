/*
 * Javascript for the Data Screen
 */

/*
 * MAIN VARIABLES
 */

var GRAPH_OPTIONS = {
        drawPoints: true,
        strokeWidth: 0.0,
        labelsUTC: true,
        labelsSeparateLine: true,
        digitsAfterDecimal: 2,
        animatedZooms: true
};

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

// The selected paramters for the plots and maps
var leftPlotXAxis = ['plot_datetime_dateTime'];
var leftPlotYAxis = ['plot_eqt_eqtMean', 'plot_eqt_eqt1'];
var leftMap = 'plot_co2_fCO2Final';

var rightPlotXAxis = ['plot_datetime_dateTime'];
var rightPlotYAxis = ['plot_co2_fCO2Final'];
var rightMap = 'plot_intaketemp_intakeTempMean';

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
    
    drawAllData();
});

function drawAllData() {
	drawLoading($('#plotLeftContent'));
	drawLoading($('#plotRightContent'));
	drawLoading($('#tableContent'));
    updatePlot('left');
    updatePlot('right');
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

function drawLeftPlot(data) {
	var status = data.status;
	
	if (status == "success") {
		leftGraph = new Dygraph (
			document.getElementById('plotLeftContent'),
	        $('#plotDataForm\\:leftData').text(),
	        GRAPH_OPTIONS
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
	        GRAPH_OPTIONS
		);
		
		resizeContent();
	}
}