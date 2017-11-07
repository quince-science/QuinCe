$(function() {
	// Make the panel splits
	$('#plotPageContent').split({orientation: 'horizontal', onDragEnd: function(){resizeContent()}});
});

function resizeContent() {
	// Don't do anything just now
}
