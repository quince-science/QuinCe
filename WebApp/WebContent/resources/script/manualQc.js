var sensorColums = [];
var calculationColumns = [];

function start() {
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
	
	/*
	return [
        {"className": "noWrap", "targets": [0]},
        {"className": "centreCol", "targets": [2, 4]},
        {"className": "numericCol", "targets": [3]},
        {"render":
        	function (data, type, row) {
        		return $.format.date(data, 'yyyy-MM-dd HH:mm:ss');
        	},
        	"targets": 1
        },
        {"render":
        	function (data, type, row) {
        		return data.toFixed(3);
        	},
        	"targets": 3
        },
        {"render":
        	function (data, type, row) {
        		var output = '<div onmouseover="showInfoPopup(' + 4 + ', \'' + row[5] + '\', this)" onmouseout="hideInfoPopup()" class="';
	            output += data ? 'good' : 'bad';
	            output += '">';
	            output += data ? 'Yes' : 'No';
	            output += '</div>';
	            return output;
        	},
        	"targets": 4
        },
	];
	*/
}

function resizePlots() {
	// TODO See if we can make this work stuff out automatically
	// when the plots are stored in plotPage.js
	// See issue #564
	
	$('#plot1Container').width(window.innerWidth - 300).height('100%');
	plot1.resize($('#plot1Container').width(), $('#plot1Container').height());
	
}

function showFlagDialog() {
	// Select the first radio button, which is "Yes"
	//PF('useCalibrationsWidget').jq.find('input:radio[value=true]').parent().next().trigger('click.selectOneRadio');
	//$(PF('useCalibrationsMessageWidget').jqId).val("");
	//updateUseDialogControls();
	PF('flagDialog').show();
}

function storeCalibrationSelection() {
	$('#plotPageForm\\:selectedRows').val(selectedRows);
	$('#plotPageForm\\:setUseCalibrations').click();

	// Update the table data
	var rows = jsDataTable.rows()[0];
	for (var i = 0; i < rows.length; i++) {
		var row = jsDataTable.row(i);
		if ($.inArray(row.data()[0], selectedRows) > -1) {
			jsDataTable.cell(i, 4).data(PF('useCalibrationsWidget').getJQ().find(':checked').val() == 'true');
			jsDataTable.cell(i, 5).data($(PF('useCalibrationsMessageWidget').jqId).val());
		}
	}
	
	clearSelection();
	PF('useDialog').hide();
}

function postSelectionUpdated() {
	if (selectedRows.length == 0) {
		PF('useCalibrationsButton').disable();
	} else {
		PF('useCalibrationsButton').enable();
	}
}

function updateUseDialogControls() {
	if (PF('useCalibrationsWidget').getJQ().find(':checked').val() == 'true') {
		$('#reasonSection').css('visibility', 'hidden');
		PF('okButtonWidget').enable();
	} else {
		$('#reasonSection').css('visibility', 'visible');
		if ($(PF('useCalibrationsMessageWidget').jqId).val().trim() == '') {
			PF('okButtonWidget').disable();
		} else {
			PF('okButtonWidget').enable();
		}
	}
}