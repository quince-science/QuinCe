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
		$('#fileFormatSpec').show();
		updateHeaderFields();
		renderSampleFile();
	}
}

// Go back to showing the file upload selector
function discardUploadedFile() {
	$('#fileFormatSpec').hide();
	$('#uploadFile').show();
}

// Render the contents of the uploaded sample file
// (upload.xhtml)
function renderSampleFile() {
	
	hideSampleFileErrors();
	
	var fileData = JSON.parse($('#newInstrumentForm\\:sampleFileContent').val());
	var fileHtml = '';
	
	var currentRow = 0;
	
	if (getHeaderMode() == 0) {
		while (currentRow < PF('headerLines').value && currentRow < fileData.length) {
			fileHtml += getLineHtml(currentRow, fileData[currentRow], 'header');
			currentRow++;
			if (currentRow >= fileData.length) {
				sampleFileError("Header is too long");
			}
		}
	} else {
		var headerEndFound = false;
		while (!headerEndFound && currentRow < fileData.length) {
			fileHtml += getLineHtml(currentRow, fileData[currentRow], 'header');
			if (fileData[currentRow] == PF('headerEndString').getJQ().val()) {
				headerEndFound = true;
			}
			
			currentRow++;
			if (currentRow >= fileData.length) {
				sampleFileError("Header end string not found");
			}
		}
	}
		
	var lastColHeadRow = currentRow + PF('colHeadRows').value;
	while (currentRow < lastColHeadRow && currentRow < fileData.length) {
		fileHtml += getLineHtml(currentRow, fileData[currentRow], 'columnHeading');
		currentRow++;
		if (currentRow >= fileData.length) {
			sampleFileError("Too many column headers");
		}
	}
	
	while (currentRow < fileData.length) {
		fileHtml += getLineHtml(currentRow, fileData[currentRow], null);
		currentRow++;
	}

	$('#fileContent').html(fileHtml);
}

function hideSampleFileErrors() {
	$('#sampleFileMessage').hide();
}

function sampleFileError(message) {
	$('#sampleFileMessage').text(message);
	$('#sampleFileMessage').show();
}

function getLineHtml(lineNumber, data, styleClass) {
	var line = '<pre id="line' + (lineNumber + 1) + '"';
	if (null != styleClass) {
		line += ' class="' + styleClass + '"';
	}
	line += '>' + data.replace(/\\t/gi, '\t') + '</pre>';
	
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

function numberOnly(event) {
	var charOK = true;
	var charCode = (event.which) ? event.which : event.keyCode
	if (charCode > 31 && (charCode < 48 || charCode > 57)) {
		charOK = false;
	}

	return charOK;
}