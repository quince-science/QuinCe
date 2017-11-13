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
    }
};

// The list of dates in the current view. Used for searching.
var dateList = null;

//The data table
var jsDataTable = null;

// The callback function for the DataTables drawing call
var dataTableDrawCallback = null;

// Variables for highlighting selected row in table
var tableScrollRow = null;
var scrollEventTimer = null;
var scrollEventTimeLimit = 300;

// Page Load function - kicks everything off
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
    		$('#plotPageForm\\:tableDataDraw').val(data.draw);		
    		$('#plotPageForm\\:tableDataStart').val(data.start);		
    		$('#plotPageForm\\:tableDataLength').val(data.length);		
    		
    		// Submit the query to the server		
    		$('#plotPageForm\\:tableGetData').click();		
    	},
    	bInfo: false,
    	columnDefs: getColumnDefs()
    	
    	/*
    	drawCallback: function (settings) {
    		if (null != tableScrollRow) {
    			highlightRow(tableScrollRow);
    			tableScrollRow = null;
    		}
    	},
    	*/
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
	// 41 is the post-rendered height of the header in FF (as measured on screen). Can we detect it somewhere?
	return $('#tableContent').height() - $('#plotPageForm\\:footerToolbar').outerHeight() - 50;
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
