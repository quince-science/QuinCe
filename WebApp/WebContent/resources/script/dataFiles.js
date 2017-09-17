function showProcessingMessage() {
	$('#uploadFile').hide();
	$('#processingFileMessage').show();
	$('#uploadForm\\:extractFileLink').click();
}

function showFileDetails() {
	$('#processingFileMessage').hide();
	$('#fileDetails').show();
}