// Hide dialog with escape key
$(document).on('keydown', function(e) {
    if (e.keyCode === 27) {
        PF('msgDialog').hide();
    }
});

function renderMessages(messages) {
	var html = $('<ul/>');
	for (var i = 0; i < messages.length; i++) {
		var row = $('<li/>');
		row.addClass(messages[i].severity);
		var summary = $('<span>');
		if (messages[i].type == "file") {
			summary = $('<h3/>');
		}
		summary.text(messages[i].summary);
		row.html(summary)
		html.append(row);
	}
	$("#messageText").html(html);
	PF('msgDialog').show();
}
