const ALIAS_RUN_TYPE = '-2';

function renderMessages(messages) {
  let html = $('<ul/>');
  for (let i = 0; i < messages.length; i++) {
    let row = $('<li/>');
    row.addClass(messages[i].severity);
    let summary = $('<span>');
    if (messages[i].type == "file") {
      summary = $('<h3/>');
    }
    summary.text(messages[i].summary);
    row.html(summary)
    html.append(row);
  }
  $("#messageText").html(html);
  PF('msgDialog').show();
  $('.ui-scrollpanel')[0].scrollTop = 0;
}

function runTypeChanged(rowIndex) {
  let runType = PF('runType_' + rowIndex).getSelectedValue();
  if (runType == ALIAS_RUN_TYPE) {
    PF('alias_' + rowIndex).jq.show()
  } else {
    PF('alias_' + rowIndex).jq.hide()
  }
}

function fileUploaded() {
  if (PF('fileUploadWidget').files.length == 0) {
    extractFiles();
  }
}

function extractFiles() {
  $('#uploadForm\\:uploadFiles').hide();
  processAllFiles(); // PF RemoteCommand
  PF('extractProgress').show();
  PF('processedProgress').start();
}

function allFilesProcessed() {
  PF('processedProgress').cancel();
  PF('extractProgress').hide();
  if ($('#uploadForm\\:unrecognisedRunTypeCount').val() > 0) {
    PF('runTypesDialog').show();
  } else {
    $('#uploadForm\\:fileDetails').show();
    $('#uploadForm\\:storeFileButton').show();
    updateFileList(); // PF remoteCommand
  }
}
