// After a sample file is uploaded, show the Processing message while
// the file is processed
function showProcessingMessage() {
  $('#uploadFile').hide();
  $('#processingFileMessage').show();
  guessFileLayout(); // PF Remote Command
}

// Once the file has been processed, display the file contents and
// format controls
// (upload_file.xhtml)
function updateFileContentDisplay() {
  $('#processingFileMessage').hide();
  $('#fileFormatSpec').show();
  updateHeaderFields();
  renderSampleFile();
}

// Go back to showing the file upload selector
function discardUploadedFile() {
  $('#fileFormatSpec').hide();
  $('#uploadFile').show();
}

// Render the contents of the uploaded sample file
// (upload.xhtml)
function renderSampleFile() {

  let fileData = JSON.parse($('#newInstrumentForm\\:sampleFileContent').val());
  let fileHtml = '';
  let messageTriggered = false;

  let currentRow = 0;

  if (getHeaderMode() == 0) {
    while (currentRow < PF('headerLines').value && currentRow < fileData.length) {
      fileHtml += getLineHtml(currentRow, fileData[currentRow], 'header');
      currentRow++;
      if (currentRow >= fileData.length) {
        sampleFileError("Header is too long");
        messageTriggered = true;
      }
    }
  } else {
    let headerEndFound = false;
    while (!headerEndFound && currentRow < fileData.length) {
      fileHtml += getLineHtml(currentRow, fileData[currentRow], 'header');
      if (fileData[currentRow] == PF('headerEndString').getJQ().val()) {
        headerEndFound = true;
      }

      currentRow++;
      if (currentRow >= fileData.length) {
        sampleFileError("Header end string not found");
        messageTriggered = true;
      }
    }
  }

  let lastColHeadRow = currentRow + PF('colHeadRows').value;
  let firstColHeadRow = true;
  while (currentRow < lastColHeadRow && currentRow < fileData.length) {
    fileHtml += getLineHtml(currentRow, fileData[currentRow], firstColHeadRow ? 'firstColumnHeading' : 'columnHeading');
    currentRow++;
	firstColHeadRow = false;
    if (currentRow >= fileData.length) {
      sampleFileError("Too many column headers");
      messageTriggered = true;
    }
  }

  while (currentRow < fileData.length) {
    fileHtml += getLineHtml(currentRow, fileData[currentRow], null);
    currentRow++;
  }

  let columnCount = parseInt($('#newInstrumentForm\\:columnCount').val());
  $('#newInstrumentForm\\:columnCountDisplay').html($('#newInstrumentForm\\:columnCount').val());
  if (columnCount <= 1) {
    sampleFileError('Cannot extract any columns with the selected separator');
    messageTriggered = true;
  }

  $('#fileContent').html(fileHtml);

  if (!messageTriggered) {
    hideSampleFileErrors();
  }

  updateUseFileButton();
}

function hideSampleFileErrors() {
  $('#sampleFileMessage').hide();
}

function sampleFileError(messages) {
  $('#sampleFileMessage').text(messages);
  $('#sampleFileMessage').show();
}

function getLineHtml(lineNumber, data, styleClass) {
  let line = '<pre id="line' + (lineNumber + 1) + '"';
  if (null != styleClass) {
    line += ' class="' + styleClass + '"';
  }
  line += '>' + data.replace(/\\t/gi, '\t') + '</pre>';

  return line;
}

function updateHeaderFields() {

  if (getHeaderMode() == 0) {
    PF('headerEndString').disable();
    enableSpinner(PF('headerLines'));
  } else {
    disableSpinner(PF('headerLines'));
    PF('headerEndString').enable();
  }
}

function getHeaderMode() {
  return parseInt($('[id^=newInstrumentForm\\:headerType]:checked').val());
}

function disableSpinner(spinnerObject) {
  spinnerObject.input.prop("disabled", true);
  spinnerObject.jq.addClass("ui-state-disabled");
  $('#newInstrumentForm\\:headerLineCount').css('pointer-events','none');
}

function enableSpinner(spinnerObject) {
  spinnerObject.input.prop("disabled", false);
  spinnerObject.jq.removeClass("ui-state-disabled");
  $('#newInstrumentForm\\:headerLineCount').css('pointer-events','all');
}

function numberOnly(event) {
  let charOK = true;
  let charCode = (event.which) ? event.which : event.keyCode
  if (charCode > 31 && (charCode < 48 || charCode > 57)) {
    charOK = false;
  }

  return charOK;
}

function updateColumnCount() {
  $('#newInstrumentForm\\:columnCountDisplay').html($('#newInstrumentForm\\:columnCount').val());
  renderSampleFile();
}

function updateUseFileButton() {
  let canUseFile = true;

  if ($('#newInstrumentForm\\:msgFileDescription').text().trim()) {
    canUseFile = false;
  }

  if ($('#sampleFileMessage').is(':visible')) {
    canUseFile = false;
  }

  if (canUseFile) {
    PF('useFileButton').enable();
  } else {
    PF('useFileButton').disable();
  }
}
