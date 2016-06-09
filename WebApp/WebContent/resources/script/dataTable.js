// Keeps track of the split positions as a percentage of the
// full data area
var tableSplitPercent = 0;
var plotSplitPercent = 0;

/*
 * Initialises the splits, and adds handler for window resizing
 */
$(function() {
	$('#dataScreenContent').split({orientation: 'horizontal', onDragEnd: function(){resizeContent()} });
	$('#plots').split({orientation: 'vertical', onDragEnd: function(){resizeContent()} });
	tableSplitPercent = '50%';
	plotSplitPercent = '50%';
	
	$(window).resize(function() {
		scaleSplits();
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
