var PLOT_POINT_SIZE = 2;
var PLOT_HIGHLIGHT_SIZE = 5;
var PLOT_FLAG_SIZE = 8;

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
    },
	clickCallback: function(e, x, points) {
		scrollToTableRow(getRowId(e, x, points));
	}
};

//The data table
var jsDataTable = null;

// The callback function for the DataTables drawing call
var dataTableDrawCallback = null;

// Variables for highlighting selected row in table
var tableScrollRow = null;
var scrollEventTimer = null;
var scrollEventTimeLimit = 300;

//Keeps track of the split positions as a percentage of the
//full data area

var resizeEventTimer = null;
var tableSplitProportion = 0.5;

// Page Load function - kicks everything off
$(function() {
	// Make the panel splits
	$('#plotPageContent').split({orientation: 'horizontal', onDragEnd: function(){scaleTableSplit()}});
	tableSplitProportion = 0.5;

	if (typeof start == 'function') {
		start();
	}
	
	// When the window is resized, scale the panels
	$(window).resize(function() {
		clearTimeout(resizeEventTimer);
		resizeEventTimer = setTimeout(resizeContent, 100);
	});

});

function scaleTableSplit() {
	tableSplitProportion = $('#plotPageContent').split().position() / $('#plotPageContent').height();
	resizeContent();
}

function resizeContent() {
	$('#plotPageContent').height(window.innerHeight - 65);

	$('#plotPageContent').split().position($('#plotPageContent').height() * tableSplitProportion);
	
	if (null != jsDataTable) {
		$('.dataTables_scrollBody').height(calcTableScrollY());
	}

	if (typeof resizePlots == 'function') {
		resizePlots();
	}
}

function makeJSDates(data) {
	
	for (var i = 0; i < data.length; i++) {
		// Replace the milliseconds value with a Javascript date
		point_data = data[i];
		point_data[0] = new Date(point_data[0]);
		data[i] = point_data;
	}

	return data;
}

/*
 * Begins the redraw of the data table.
 * The HTML table is initialised (with header only), and
 * the DataTables object is created and configured to load
 * its data from the server using the hidden form.
 */
function drawTable() {

	// PUT COLUMN HEADERS IN JS FROM DATASCREENBEAN
	html = '<table id="dataTable" class="display compact nowrap" cellspacing="0" width="100%"><thead>';

	columnHeadings.forEach(heading => {
		html += '<th>';
		html += heading;
		html += '</th>';
	});

	html += '</thead></table>';

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
    		$('#plotPageForm\\:tableDataDraw').val(data.draw);		
    		$('#plotPageForm\\:tableDataStart').val(data.start);		
    		$('#plotPageForm\\:tableDataLength').val(data.length);		
    		
    		// Submit the query to the server		
    		$('#plotPageForm\\:tableGetData').click();		
    	},
    	bInfo: false,
    	columnDefs: getColumnDefs(),
    	drawCallback: function (settings) {
    		if (null != tableScrollRow) {
    			highlightRow(tableScrollRow);
				tableScrollRow = null;
    		}
    	}
    	/*
    	rowCallback: function( row, data, index ) {
    		var rowNumber = parseInt(data[getColumnIndex('Row')], 10);
            if ( $.inArray(rowNumber, selectedRows) !== -1 ) {
                $(row).addClass('selected');
            }
            
            $(row).on('click', function(event) {
            	clickRowAction(rowNumber, event.shiftKey);
            });
        }
        */
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

/*
 * Calculate the value of the scrollY entry for the data table
 */
function calcTableScrollY() {
	return $('#tableContent').height() - $('#plotPageForm\\:footerToolbar').outerHeight();
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
            draw: $('#plotPageForm\\:tableDataDraw').val(),		
            data: JSON.parse($('#plotPageForm\\:tableJsonData').val()),		
            recordsTotal: $('#plotPageForm\\:recordCount').val(),		
            recordsFiltered: $('#plotPageForm\\:recordCount').val()		
		});
	}
}

/*
 * Get the Row ID from a given graph click event
 * 
 * For now, this just looks up the row using the X value. This will
 * work for dates, but will need to be more intelligent for non-date plots.
 */
function getRowId(event, xValue, points) {
	var plotData = JSON.parse($('#plotPageForm\\:plotData').val());
	var pointId = points[0]['idx'];
	return plotData[pointId][1];
}

/*
 * Scroll to the table row with the given ID
 */
function scrollToTableRow(rowId) {

	var tableRow = -1;
	
	if (null != rowId) {
		tableRow = JSON.parse($('#plotPageForm\\:tableRowIds').val()).indexOf(rowId);
	}
	
	if (tableRow >= 0) {
		jsDataTable.scroller().scrollToRow(tableRow - 2);
		
		// Because we scroll to the row - 2, we know that the
		// row we want to highlight is the third row
		tableScrollRow = tableRow;
		
		// The highlight is done as part of the table draw callback
	}
}

function highlightRow(tableRow) {
	if (null != tableRow) {
		setTimeout(function() {
			var rowNode = $('#row' + tableRow)[0];
			$(rowNode).css('animationName', 'rowFlash').css('animationDuration', '1s');
			setTimeout(function() {
				$(rowNode).css('animationName', '');
			}, 1000);
		}, 100);
	}
}
