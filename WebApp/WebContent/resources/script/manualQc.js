var sensorColums = [];
var calculationColumns = [];

function start() {
	/*
	 * This is a hack to get round a bug in PrimeFaces.
	 * The selection buttons should be disabled up front, but
	 * PrimeFaces then drops their onclick handlers. So they're
	 * enabled when the page loads, and this will disable them.
	 */
	postSelectionUpdated();
	drawPage();
}

function drawPage() {
	//drawPlot(1);
	drawTable();
}

/*
 * Show or hide columns as required.
 */
function renderTableColumns() {
	// ID
	jsDataTable.columns(0).visible(false, false);

	// Message columns are the last and third from last columns
	jsDataTable.columns([jsDataTable.columns()[0].length - 1, jsDataTable.columns()[0].length - 3]).visible(false, false);
	
	var tableMode = PF('tableModeSelector').getJQ().find(':checked').val();
	
	if (tableMode == "sensors") {
		jsDataTable.columns(sensorColumns).visible(true, false);
		jsDataTable.columns(calculationColumns).visible(false, false);
	} else if (tableMode == "calculations") {
		jsDataTable.columns(sensorColumns).visible(false, false);
		jsDataTable.columns(calculationColumns).visible(true, false);
	}
	
	// TODO This is an ugly hack to ensure the final fCO2 is always displayed.
	var additionalData = JSON.parse($('#plotPageForm\\:additionalTableData').val());
	jsDataTable.columns(additionalData.flagColumns[0] - 1).visible(true, true);
}

/*
 * Formats etc for table columns
 */
function getColumnDefs() {
	var additionalData = JSON.parse($('#plotPageForm\\:additionalTableData').val());
	
	sensorColumns = [];
	var colIndex = 3;
	for (i = 0; i < additionalData.sensorColumnCount; i++) {
		colIndex++;
		sensorColumns.push(colIndex);
	}

	calculationColumns = [];
	for (i = 0; i < additionalData.calculationColumnCount; i++) {
		colIndex++;
		calculationColumns.push(colIndex);
	}
	
	var numericCols = $.merge($.merge([2, 3], sensorColumns), calculationColumns);
	
	return [
        {"className": "centreCol", "targets": additionalData.flagColumns},
        {"className": "numericCol", "targets": numericCols},
        {"render":
        	function (data, type, row) {
        		return $.format.date(data, 'yyyy-MM-dd HH:mm:ss');
        	},
        	"targets": 1
        },
        {"render":
        	function (data, type, row) {
        		return (null == data ? null : data.toFixed(3));
        	},
        	"targets": numericCols
        },
        {"render":
        	function (data, type, row) {
                var output = '<div onmouseover="showInfoPopup(' + row[additionalData.flagColumns[0]] + ', \'' + row[additionalData.flagColumns[0] + 1] + '\', this)" onmouseout="hideInfoPopup()" class="';
                output += getFlagClass(data);
                output += '">';
                output += getFlagText(data);
                output += '</div>';
                return output;
            },
            "targets": additionalData.flagColumns[0]
        },
        {"render":
        	function (data, type, row) {
        		var output = '<div class="';
        		output += getFlagClass(data);
        		output += '">';
				output += getFlagText(data);
				output += '</div>';
				return output;
            },
            "targets": additionalData.flagColumns[1]
        }
	];
}

function resizePlots() {
	// TODO See if we can make this work stuff out automatically
	// when the plots are stored in plotPage.js
	// See issue #564
	
//	$('#plot1Container').width(window.innerWidth - 300).height('100%');
//	plot1.resize($('#plot1Container').width(), $('#plot1Container').height());
	
}

function postSelectionUpdated() {
	if (selectedRows.length == 0) {
		PF('acceptQcButton').disable();
		PF('overrideQcButton').disable();
	} else {
		PF('acceptQcButton').enable();
		PF('overrideQcButton').enable();
	}
}


function updateFlagDialogControls() {
	var canSubmit = true;
	
	if (PF('flagMenu').input.val() != 2) {
		if ($('#plotPageForm\\:manualComment').val().trim().length == 0) {
			canSubmit = false;
		}
	}
	
	if (canSubmit) {
		PF('okButtonWidget').enable();
	} else {
		PF('okButtonWidget').disable();
	}
}

function acceptAutoQc() {
	$('#plotPageForm\\:selectedRows').val(selectedRows);
	$('#plotPageForm\\:acceptAutoQc').click();
}

function qcFlagsAccepted() {
	var additionalData = JSON.parse($('#plotPageForm\\:additionalTableData').val());
	
	var autoFlagColumn = additionalData.flagColumns[0];
	var autoMessageColumn = autoFlagColumn + 1;
	var userFlagColumn= additionalData.flagColumns[1];
	var userMessageColumn = userFlagColumn + 1;
	
	var rows = jsDataTable.rows()[0];
	for (var i = 0; i < rows.length; i++) {
		var row = jsDataTable.row(i);
		if ($.inArray(row.data()[0], selectedRows) > -1) {
			jsDataTable.cell(i, userMessageColumn).data(jsDataTable.cell(row, autoMessageColumn).data());
			jsDataTable.cell(i, userFlagColumn).data(jsDataTable.cell(row, autoFlagColumn).data());
		}
	}
	
	clearSelection();
}

function startUserQcFlags() {
	$('#plotPageForm\\:selectedRows').val(selectedRows);
	$('#plotPageForm\\:generateUserQcComments').click();
}

function showFlagDialog() {
	var woceRowHtml = selectedRows.length.toString() + ' row';
	if (selectedRows.length > 1) {
		woceRowHtml += 's';
	} 
	$('#manualRowCount').html(woceRowHtml);

    var commentsString = '';
    var comments = JSON.parse($('#plotPageForm\\:userCommentList').val());
    for (var i = 0; i < comments.length; i++) {
    	var comment = comments[i];
    	commentsString += comment[0];
    	commentsString += ' (' + comment[2] + ')';
    	if (i < comments.length - 1) {
    		commentsString += '\n';
    	}
    }
    $('#plotPageForm\\:manualComment').val(commentsString);
    
    PF('flagMenu').selectValue($('#plotPageForm\\:worstSelectedFlag').val());
    updateFlagDialogControls();
    PF('flagDialog').show();
}

function saveManualComment() {
	$('#plotPageForm\\:selectedRows').val(selectedRows);
	$('#plotPageForm\\:applyManualFlag').click();
	PF('flagDialog').hide();
}

function manualFlagsUpdated() {
	
	var additionalData = JSON.parse($('#plotPageForm\\:additionalTableData').val());
	
	var userFlagColumn= additionalData.flagColumns[1];
	var userMessageColumn = userFlagColumn + 1;
	
	var rows = jsDataTable.rows()[0];
	for (var i = 0; i < rows.length; i++) {
		var row = jsDataTable.row(i);
		if ($.inArray(row.data()[0], selectedRows) > -1) {
			jsDataTable.cell(i, userMessageColumn).data($('#plotPageForm\\:manualComment').val());
			jsDataTable.cell(i, userFlagColumn).data(PF('flagMenu').input.val());
		}
	}

	clearSelection();
}

