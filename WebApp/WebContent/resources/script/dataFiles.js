function showProcessingMessage() {
	$('#uploadFile').hide();
	$('#messages').hide();
	$('#uploadForm\\:extractFileLink').click();
}

function showFileDetails() {
	
	$('#processingFileMessage').hide();

	var matchedFile = $('#uploadForm\\:fileType').html();
	
	if (matchedFile == '') {
		$('#uploadFile').show();
	} else {
		var messages = JSON.parse($('#uploadForm\\:fileMessages').val());
		if (messages.length > 0) {
			$('#uploadFile').show();
			renderMessages(messages);
			$('#messages').show();
		} else {
			$('#fileDetails').show();
			$('#uploadForm\\:fileButtons').show();
		}
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
