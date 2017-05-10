// After a sample file is uploaded, show the Processing message while
// the file is processed
// (upload_file.xhtml)
function showProcessingMessage() {
	$('#uploadFile').hide();
	$('#processingFileMessage').show();
	$('#newInstrumentForm\\:guessFileLayout').click();
}

// Once the file has been processed, display the file contents and
// format controls
// (upload_file.xhtml)
function updateFileContentDisplay(data) {
	var status = data.status;
	
	if (status == "success") {
		$('#processingFileMessage').hide();
		renderSampleFile();
		$('#fileFormatSpec').show();
	}
	
	updateHeaderFields();
}

// Render the contents of the uploaded sample file
// (upload.xhtml)
function renderSampleFile() {
	
	var fileData = JSON.parse($('#newInstrumentForm\\:sampleFileContent').val());
	var fileHtml = '';
	
	var currentRow = 0;
	
	if (getHeaderMode() == 0) {
		while (currentRow < PF('headerLines').value) {
			fileHtml += getLineHtml(currentRow, fileData[currentRow], 'header');
			currentRow++;
		}
	} else {
		var headerEndFound = false;
		while (!headerEndFound) {
			fileHtml += getLineHtml(currentRow, fileData[currentRow], 'header');
			if (fileData[currentRow] == PF('headerEndString').getJQ().val()) {
				headerEndFound = true;
			}
			
			currentRow++;
		}
	}
		
	var lastColHeadRow = currentRow + PF('colHeadRows').value;
	while (currentRow < lastColHeadRow) {
		fileHtml += getLineHtml(currentRow, fileData[currentRow], 'columnHeading');
		currentRow++;
	}
	
	while (currentRow < fileData.length) {
		fileHtml += getLineHtml(currentRow, fileData[currentRow], null);
		currentRow++;
	}

	$('#fileContent').html(fileHtml);
}

function getLineHtml(lineNumber, data, styleClass) {
	var line = '<pre id="line' + (lineNumber + 1) + '"';
	if (null != styleClass) {
		line += ' class="' + styleClass + '"';
	}
	line += '>' + data + '</pre>';
	
	return line;
}

function updateHeaderFields() {
	
	if (getHeaderMode() == 0) {
		PF('headerEndString').disable();
		enableSpinner(PF('headerLines'));
	} else {
		disableSpinner(PF('headerLines'));
		PF('headerEndString').enable();
	}
}

function getHeaderMode() {
	return parseInt($('[id^=newInstrumentForm\\:headerType]:checked').val());
}

function disableSpinner(spinnerObject) {
    spinnerObject.input.prop("disabled", true);
    spinnerObject.jq.addClass("ui-state-disabled");
    spinnerObject.upButton.unbind('mousedown.spinner')
    spinnerObject.downButton.unbind('mousedown.spinner')
}

function enableSpinner(spinnerObject) {
   spinnerObject.input.prop("disabled", false);
   spinnerObject.jq.removeClass("ui-state-disabled");
   spinnerObject.bindEvents()
}