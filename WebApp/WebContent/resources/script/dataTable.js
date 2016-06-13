// Keeps track of the split positions as a percentage of the
// full data area
var tableSplitPercent = 0;
var plotSplitPercent = 0;

/*
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

});

/*
 * Handles the resizing of individual panels
 */
function resizeContent() {
	tableSplitPercent = '' + $('#dataScreenContent').split().position() / $('#dataScreenContent').height() * 100 + '%';
	plotSplitPercent = '' + $('#plots').split().position() / $('#dataScreenContent').width() * 100 + '%';
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
function showPlotPopup(event) {
	
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
		$('#plotFieldList')
        .find('input')
        .each(function(index, item) {
        	var itemGroup = getGroupName(item.id);
        	if (itemGroup != group) {
        		item.checked = false;
        	}
        });
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