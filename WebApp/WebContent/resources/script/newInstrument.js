// After a sample file is uploaded, show the Processing message while
// the file is processed
// (upload_file.xhtml)
function showProcessingMessage() {
	$('#uploadFile').hide();
	$('#processingFileMessage').show();
	$('#newInstrumentForm\\:guessFileLayout').click();
}

function updateFileContentDisplay(data) {
	var status = data.status;
	
	if (status == "success") {
		$('#processingFileMessage').hide();
		renderSampleFile();
		$('#fileFormatSpec').show();
	}
}

function renderSampleFile() {
	
	var fileData = $('#newInstrumentForm\\:sampleFileContent').val().split('\n');
	
	
	var fileHtml = '';
	for (var i = 0; i < fileData.length; i++) {
		fileHtml += '<pre>' + fileData[i] + '</pre>';
	}
	
	$('#fileContent').html(fileHtml);
	
}