function showProcessingMessage() {
	$('#uploadFile').hide();
	$('#uploadForm\\:extractFileLink').click();
}

function showFileDetails() {
	
	$('#processingFileMessage').hide();

	var matchedFile = $('#uploadForm\\:fileDefinition').html();
	var messages = JSON.parse($('#uploadForm\\:fileMessages').val());
	
	if (matchedFile == '') {
		$('#uploadFile').show();
	} else if (messages.length > 0) {
		$('#uploadFile').show();
		renderMessages(messages);
		$('#messages').show();
	} else {
		$('#fileDetails').show();
	}
}


function renderMessages(messages) {
	var html = '<ul>';
	for (var i = 0; i < messages.length; i++) {
		html += '<li>' + messages[i] + '</li>';
	}
	html += '</ul>';

	$('#messages').html(html);
}
