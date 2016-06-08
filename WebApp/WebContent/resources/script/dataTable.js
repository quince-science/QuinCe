var DATA_TABLE_MIN_HEIGHT = 250;

// Initialise panel resizing
// Vertical
$(function() {
	$('#plots').resizable({
		handles: { 's' : $('#dividerHorizontal')},
		resize: function(event, ui) { 
		    ui.size.width = ui.originalSize.width;
		},
		stop: function(event, ui) {
			resizeDataTable();
		}
	});
	
	resizeDataTable();
});

function resizeDataTable() {
	dataTableTop = $('#dividerHorizontal').position().top + 7;
	windowHeight = window.innerHeight;
	
	dataTableHeight = window.innerHeight - dataTableTop - $('#plots').position().top * 2;
	if (dataTableHeight < DATA_TABLE_MIN_HEIGHT) {
		$('#plots').height(windowHeight - DATA_TABLE_MIN_HEIGHT - 7 - $('#plots').position().top);
		dataTableHeight = DATA_TABLE_MIN_HEIGHT;
	}
	
	$('#dataTable').height(dataTableHeight);
	console.log(windowHeight);
	console.log(dataTableTop + dataTableHeight);
}
