function showProcessingMessage() {
	$('#uploadFile').hide();
	$('#uploadForm\\:extractFileLink').click();
}

function showFileDetails() {
	
	$('#processingFileMessage').hide();

	var matchedFile = $('#uploadForm\\:fileDefinition').html();
	if (matchedFile == '') {
		$('#uploadFile').show();
	} else {
		$('#fileDetails').show();
	}
}
