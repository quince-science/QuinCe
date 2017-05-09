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
	
	var fileData = $('#newInstrumentForm\\:sampleFileContent').val().split('\n');
	var fileHtml = '';
	for (var i = 0; i < fileData.length; i++) {
		fileHtml += '<pre id="line' + (i + 1) + '">' + fileData[i] + '</pre>';
	}
	$('#fileContent').html(fileHtml);
}

function updateHeaderFields() {
	
	switch (parseInt($('[id^=newInstrumentForm\\:headerType]:checked').val())) {
	case 0: {
		PF('headerEndString').disable();
		enableSpinner(PF('headerLines'));
		break;
	}
	case 1: {
		disableSpinner(PF('headerLines'));
		PF('headerEndString').enable();
		break;
	}
	}
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