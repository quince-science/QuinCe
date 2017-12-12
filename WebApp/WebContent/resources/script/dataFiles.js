function processFiles() {
	extractNext();
	if (PF('fileUploadWidget').files.length === 0) {
		refreshFileList();
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
