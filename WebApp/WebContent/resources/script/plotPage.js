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
