ALIAS_RUN_TYPE = '-2';

// Hide dialog with escape key
$(document).on('keydown', function(e) {
  if (e.keyCode === 27) {
    $.each(PrimeFaces.widgets, function(index, val) {
      if (index.match(/dialog/i)) {
        PF(index).hide();
      }
    })
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

function reprocessUploadedFiles() {
  $('#uploadForm\\:fileList_data>tr').each(function() {
    extractNext();
  });
}

function runTypeChanged(rowIndex, runTypeIndex) {
  var runType = PF('missingRunType_' + rowIndex + '_' + runTypeIndex).getSelectedValue();
  if (runType == ALIAS_RUN_TYPE) {
    PF('alias_' + rowIndex + '_' + runTypeIndex).jq.show()
  } else {
    PF('alias_' + rowIndex + '_' + runTypeIndex).jq.hide()
  }
}
