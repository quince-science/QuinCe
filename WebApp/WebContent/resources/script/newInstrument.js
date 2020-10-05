//************************************************
// Global variables:
//************************************************

allTimesOK = false;
drawingPage = false;

ALIAS_RUN_TYPE = '-2';

//************************************************
//
// UPLOAD FILE PAGE
//
//************************************************

// After a sample file is uploaded, show the Processing message while
// the file is processed
// (upload_file.xhtml)
function showProcessingMessage() {
  $('#uploadFile').hide();
  $('#processingFileMessage').show();
  $('#newInstrumentForm\\:guessFileLayout').click();
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

  var fileData = JSON.parse($('#newInstrumentForm\\:sampleFileContent').val());
  var fileHtml = '';
  var messageTriggered = false;

  var currentRow = 0;

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
    var headerEndFound = false;
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

  var lastColHeadRow = currentRow + PF('colHeadRows').value;
  while (currentRow < lastColHeadRow && currentRow < fileData.length) {
    fileHtml += getLineHtml(currentRow, fileData[currentRow], 'columnHeading');
    currentRow++;
    if (currentRow >= fileData.length) {
      sampleFileError("Too many column headers");
      messageTriggered = true;
    }
  }

  while (currentRow < fileData.length) {
    fileHtml += getLineHtml(currentRow, fileData[currentRow], null);
    currentRow++;
  }

  var columnCount = parseInt($('#newInstrumentForm\\:columnCount').val());
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
  var line = '<pre id="line' + (lineNumber + 1) + '"';
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
  var charOK = true;
  var charCode = (event.which) ? event.which : event.keyCode
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
  var canUseFile = true;

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

function useFile() {
  $('#useFileForm\\:useFileLink').click()
}

//************************************************
//
// SENSOR ASSIGNMENTS PAGE
//
//************************************************
function getFileIndex(fileName) {
  var index = -1;

  for (var i = 0; i < filesAndColumns.length; i++) {
    if (filesAndColumns[i]['description'] == fileName) {
      index = i;
      break;
    }
  }

  return index;
}

function unAssign(file, column) {
  $('#newInstrumentForm\\:unassignFile').val(file);
  $('#newInstrumentForm\\:unassignColumn').val(column);
  $('#newInstrumentForm\\:unassignVariableLink').click();
}

function getColumnName(fileName, columnIndex) {
  var result = null;

  for (var i = 0; i < filesAndColumns.length; i++) {
    if (filesAndColumns[i]['description'] == fileName) {
      result = filesAndColumns[i]['columns'][columnIndex];
      break;
    }
  }

  return result;
}

function getColumnAssignment(fileIndex, column) {
  var assigned = null;

  var fileName = filesAndColumns[fileIndex]['description'];

  var sensorAssignments = JSON.parse($('#newInstrumentForm\\:sensorAssignments').val());
  for (var i = 0; null == assigned && i < sensorAssignments.length; i++) {
    var assignments = sensorAssignments[i]['assignments'];

    for (var j = 0; j < assignments.length; j++) {
      var assignment = assignments[j];
      if (assignment['file'] == fileName && assignment['column'] == column) {
        assigned = sensorAssignments[i]['name'];
        break;
      }
    }
  }

  if (null == assigned) {
    var fileSpecificAssignments = JSON.parse($('#newInstrumentForm\\:fileSpecificAssignments').val());

    assignments = fileSpecificAssignments[fileIndex];

    var dateTimes = assignments['dateTime'];
    for (var i = 0; i < dateTimes.length; i++) {
      var dateTime = dateTimes[i];
      if (dateTime['column'] == column) {
        assigned = dateTime['name'];
        break;
      }
    }

    for (var i = 0; i < fileSpecificAssignments.length; i++) {

      if (assignments['latitude']['valueColumn'] == column) {
        assigned = 'Latitude';
      } else if (assignments['latitude']['hemisphereColumn'] == column) {
        assigned = 'Latitue Hemisphere';
      } else if (assignments['longitude']['valueColumn'] == column) {
        assigned = 'Longitude';
      } else if (assignments['longitude']['hemisphereColumn'] == column) {
        assigned = 'Longitude Hemisphere';
      }
    }

    if (assignments['runTypeCol'] == column) {
      assigned = 'Run Type';
    }
  }

  return assigned;
}

function startAssign(event, item, file, column) {
  if (item == 'DATETIMESUBMENU') {
    showDateTimeSubmenu(event, file, column);
  } else if (item == 'DIAGNOSTICSUBMENU') {
    showDiagnosticSubmenu(event, file, column);
  } else if (item.startsWith('DATETIME_')) {
    openDateTimeDialog(item, file, column);
  } else if (item == 'POS_longitude') {
    openLongitudeDialog(file, column);
  } else if (item == 'POS_longitude_hemisphere') {
    openHemisphereDialog('longitude', file, column);
  } else if (item == 'POS_latitude') {
    openLatitudeDialog(file, column);
  } else if (item == 'POS_latitude_hemisphere') {
    openHemisphereDialog('latitude', file, column);
  } else if (item == 'OTHER_runType') {
    assignRunType(file, column);
  } else {
    openAssignSensorDialog(item, file, column);
  }
}

function assignRunType(file, column) {
  $('#newInstrumentForm\\:runTypeFile').val(filesAndColumns[file]['description']);
  $('#newInstrumentForm\\:runTypeColumn').val(column);
  $('#newInstrumentForm\\:runTypeLink').click();
}

function openAssignSensorDialog(sensorType, file, columnIndex, columnName) {

  $('#newInstrumentForm\\:sensorAssignmentFile').val(file);
  $('#newInstrumentForm\\:sensorAssignmentColumn').val(columnIndex);
  $('#newInstrumentForm\\:sensorAssignmentSensorType').val(sensorType.id);

  $('#sensorAssignmentFileName').text(file);
  $('#sensorAssignmentColumnName').text(columnName);
  $('#sensorAssignmentSensorTypeText').text(sensorType.name);

  $('#newInstrumentForm\\:sensorAssignmentName').val(columnName);

  if (null == sensorType.dependsQuestion) {
    $('#sensorAssignmentDependsQuestionContainer').hide();
  } else {
    $('#sensorAssignmentDependsQuestion').text(sensorType.dependsQuestion);
    $('#sensorAssignmentDependsQuestionContainer').show();
  }

  $('#sensorAssignmentPrimaryContainer').hide();

  PF('sensorAssignmentAssignButton').enable();
  PF('sensorAssignmentDialog').show();
}

function getSensorType(sensorId) {
  var result = null;

  for (var i = 0; i < window.sensorTypes.length; i++) {
    var sensorType = window.sensorTypes[i];
    if (sensorType['id'] == sensorId) {
      result = sensorType;
      break;
    }
  }

  return result;
}

function canBeNamed(sensor) {
  var result = true;

  var assignments = JSON.parse($('#newInstrumentForm\\:sensorAssignments').val());

  for (var i = 0; i < assignments.length; i++) {
    var assignment = assignments[i];
    if (assignment['name'] == sensor) {
      result = assignment['named'];
      break;
    }
  }

  return result;
}

function sensorAssigned() {
  PF('sensorAssignmentDialog').hide();
  setupDragDropEvents();
}

function timePositionAssigned(dialogName) {
  PF(dialogName).hide();
  renderAssignments();
}

function openLongitudeDialog(file, column) {
  $('#newInstrumentForm\\:longitudeFile').val(filesAndColumns[file]['description']);
  $('#newInstrumentForm\\:longitudeColumn').val(column);

  $('#longitudeAssignmentFile').text(filesAndColumns[file]['description']);
  $('#longitudeAssignmentColumn').text(filesAndColumns[file]['columns'][column]);

  PF('longitudeAssignmentDialog').show();
}

function openLatitudeDialog(file, column) {
  $('#newInstrumentForm\\:latitudeFile').val(filesAndColumns[file]['description']);
  $('#newInstrumentForm\\:latitudeColumn').val(column);

  $('#latitudeAssignmentFile').text(filesAndColumns[file]['description']);
  $('#latitudeAssignmentColumn').text(filesAndColumns[file]['columns'][column]);

  PF('latitudeAssignmentDialog').show();
}

function hemisphereRequired(fileIndex, direction) {
  var result = false;

  var assignments = JSON.parse($('#newInstrumentForm\\:fileSpecificAssignments').val());
  var assignment = assignments[fileIndex][direction];

  if (null != assignment) {
    result = assignment['hemisphereRequired'];
  }

  return result;
}

function openHemisphereDialog(coordinate, file, column) {
  $('#newInstrumentForm\\:hemisphereFile').val(filesAndColumns[file]['description']);
  $('#hemisphereAssignmentFile').text(filesAndColumns[file]['description']);

  $('#newInstrumentForm\\:hemisphereColumn').val(column);
  $('#hemisphereAssignmentColumn').text(filesAndColumns[file]['columns'][column]);

  if (coordinate == "longitude") {
    $('#newInstrumentForm\\:hemisphereCoordinate').val(0);
    $('#hemisphereAssignmentCoordinate').text('Longitude Hemisphere');
  } else {
    $('#newInstrumentForm\\:hemisphereCoordinate').val(1);
    $('#hemisphereAssignmentCoordinate').text('Latitude Hemisphere');
  }

  PF('hemisphereAssignmentDialog').show();
}

function openDateTimeDialog(item, file, column) {
  $('#newInstrumentForm\\:dateTimeFile').val(filesAndColumns[file]['description']);
  $('#newInstrumentForm\\:dateTimeColumn').val(column);
  $('#newInstrumentForm\\:startTimePrefix').val('');
  $('#newInstrumentForm\\:startTimeSuffix').val('');


  var variableName = item.substring(9, item.length);
  $('#newInstrumentForm\\:dateTimeVariable').val(variableName);

  $('#dateTimeAssignmentFile').text(filesAndColumns[file]['description']);
  $('#dateTimeAssignmentColumn').text(filesAndColumns[file]['columns'][column]);
  $('#dateTimeAssignmentVariable').text(variableName);

  $('#dateTimeFormatContainer').hide();
  $('#dateFormatContainer').hide();
  $('#timeFormatContainer').hide();
  $('#hoursFromStartContainer').hide();

  switch (variableName) {
  case "Combined Date and Time": {
    $('#dateTimeFormatContainer').show();
    break;
  }
  case "Hours from start of file": {
    $('#hoursFromStartContainer').show();
    updateStartTime();
    break;
  }
  case "Date": {
    $('#dateFormatContainer').show();
    break;
  }
  case "Time": {
    $('#timeFormatContainer').show();
    break;
  }
  }

  if (variableName == 'Hours from start of file') {
    PF('dateTimeAssign').disable();
  } else {
    PF('dateTimeAssign').enable();
  }

  PF('dateTimeAssignmentDialog').show();
}

function updateStartTime() {
  var lineJson = $('#newInstrumentForm\\:startTimeLine').val();

  if (null == lineJson || lineJson == "") {
    $('#startTimeExtractedLine').text("No matching line found in header");
    $('#startTimeExtractedLine').addClass("error");
  } else {
    var line = JSON.parse(lineJson);
    if (line['string'] == "") {
      $('#startTimeExtractedLine').text("No matching line found in header");
      $('#startTimeExtractedLine').addClass("error");
    } else {
      var lineHtml = "";

      if (line['highlightStart'] > 0) {
        lineHtml += line['string'].substring(0, line['highlightStart']);
      }

      lineHtml += '<span class="highlight">';
      lineHtml += line['string'].substring(line['highlightStart'], line['highlightEnd']);
      lineHtml += '</span>';

      lineHtml += line['string'].substring(line['highlightEnd'], line['string'].length);

      $('#startTimeExtractedLine').html(lineHtml);
      $('#startTimeExtractedLine').removeClass("error");
    }
  }

  var extractedDate = $('#newInstrumentForm\\:startTimeDate').val();
  if (null == extractedDate || extractedDate == "") {
    $('#startTimeExtractedDate').text("Could not extract date from header line");
    $('#startTimeExtractedDate').addClass("error");
    $('#startTimeExtractedDate').removeClass("highlight");
    PF('dateTimeAssign').disable();
  } else {
    $('#startTimeExtractedDate').text(extractedDate);
    $('#startTimeExtractedDate').removeClass("error");
    $('#startTimeExtractedDate').addClass("highlight");
    PF('dateTimeAssign').enable();
  }
}

function checkSensorName() {
  var nameOK = true;

  var enteredName = $('#newInstrumentForm\\:sensorAssignmentName').val().trim();
  if (enteredName == "") {
    nameOK = false;
  }

  if (nameOK) {
    var sensorType = $('#newInstrumentForm\\:sensorAssignmentSensorType').val();
    var sensorAssignments = JSON.parse($('#newInstrumentForm\\:sensorAssignments').val());

    var currentSensorAssignments = null;

    for (var i = 0; i < sensorAssignments.length; i++) {
      if (sensorAssignments[i]['name'] == sensorType) {
        currentSensorAssignments = sensorAssignments[i]['assignments'];
        break;
      }
    }

    if (null != currentSensorAssignments && currentSensorAssignments.length > 0) {
      for (var i = 0; i < currentSensorAssignments.length; i++) {
        if (currentSensorAssignments[i]['sensorName'] == enteredName) {
          nameOK = false;
          break;
        }
      }
    }
  }

  if (!nameOK) {
    $('#sensorNameMessage').show();
    PF('sensorAssignmentAssignButton').disable();
  } else {
    $('#sensorNameMessage').hide();
    PF('sensorAssignmentAssignButton').enable();
  }
}

function hideMenusAndDialogs(event) {
  event.stopPropagation();
  PF('sensorAssignmentDialog').hide();
  PF('longitudeAssignmentDialog').hide();
  PF('latitudeAssignmentDialog').hide();
  PF('hemisphereAssignmentDialog').hide();
  PF('dateTimeAssignmentDialog').hide();
  hideMainAssignmentMenu();
}

function removeFile(fileName) {
  $('#newInstrumentForm\\:removeFileName').val(fileName);
  PF('removeFileConfirm').show();
  return false;
}

function renameFile(fileDescription) {
  $('#newInstrumentForm\\:renameOldFile').val(fileDescription);
  PF('renameNewFile').jq.val(fileDescription);
  PF('renameFileDialog').show();
  return false;
}

function renameFileInputMonitor() {
  if (PF('renameNewFile').jq.val().length == 0) {
    PF('renameFileButton').disable();
  } else {
    PF('renameFileButton').enable();
  }
}

function assignVariablesInit() {
  window.sensorTypes = JSON.parse($('#referenceDataForm\\:sensorTypes').val());
  setupDragDropEvents();
}

function setupDragDropEvents() {
  $('div[id^=col-]').on('dragstart', handleColumnDragStart);
  
  $('.columnDropTarget')
    .on('dragover', handleColumnDragOver)
    .on('dragenter', handleColumnDragEnter)
    .on('dragleave', handleColumnDragLeave)
    .on('drop', handleColumnDrop);
}

function handleColumnDragStart(e) {
  e.originalEvent.dataTransfer.setData("text/plain", e.target.id);
  e.originalEvent.dataTransfer.dropEffect = "link";
}

function handleColumnDragEnter(e) {
  $(this).addClass('columnDropTargetHover');
}

function handleColumnDragLeave(e) {
  $(this).removeClass('columnDropTargetHover');
}

function handleColumnDragOver(e) {
  e.preventDefault();
  $(this).addClass('columnDropTargetHover');
  e.originalEvent.dataTransfer.dropEffect = "link";
}

function handleColumnDrop(e) {
  e.preventDefault();
  $(this).removeClass('columnDropTargetHover');
  
  // Get sensor type
  let sensorTypeName = $(this).find('span[class~="ui-treenode-content"] > span[class~="ui-treenode-label"]').html();
  let sensorTypeId = getSensorTypeID(sensorTypeName);

  // Get column details
  let colElementId = e.originalEvent.dataTransfer.getData("text/plain");
  let colExtractor = /.*---(.*)---(.*)---(.*)/;
  let match = colExtractor.exec(colElementId);

  openAssignSensorDialog(getSensorType(sensorTypeId), match[1], match[2], match[3]);
}

function getSensorTypeID(typeName) {
  let result = null;

  for (let i = 0; i < window.sensorTypes.length; i++) {
	if (window.sensorTypes[i].name == typeName) {
	  result = window.sensorTypes[i].id;
      break;
	}
  }

  return result;
}

/*******************************************************
*
* RUN TYPES PAGE
*
*******************************************************/
function setRunTypeCategory(fileIndex, runType) {

  if (!drawingPage) {
    var escapedRunType = runType.replace(/(~)/g, "\\$1");
    var runTypeCategory = PF(fileIndex + '-' + runType + '-menu').getSelectedValue();
    var aliasTo = null;

    if (runTypeCategory == ALIAS_RUN_TYPE) {
      $('#' + fileIndex + '-' + escapedRunType + '-aliasMenu').show();
      aliasTo = PF(fileIndex + '-' + runType + '-alias').getSelectedValue();
    } else {
      $('#' + fileIndex + '-' + escapedRunType + '-aliasMenu').hide();
    }

    $('#newInstrumentForm\\:assignCategoryFile').val(fileIndex);
    $('#newInstrumentForm\\:assignCategoryRunType').val(runType);
    $('#newInstrumentForm\\:assignCategoryCode').val(runTypeCategory);
    $('#newInstrumentForm\\:assignAliasTo').val(aliasTo);
    $('#newInstrumentForm\\:assignCategoryLink').click();
  }
}

function renderAssignedCategories() {
  var categoriesOK = true;

  var html = '';

  var assignments = JSON.parse($('#newInstrumentForm\\:assignedCategories').val());

  for (var i = 0; i < assignments.length; i++) {
    var assignment = assignments[i];

    html += '<div class="assignmentListEntry';
    if (assignment[1] < assignment[2]) {
      html += ' assignmentRequired';
      categoriesOK = false;
    }
    html += '"><div class="assignmentLabel">';
    html += assignment[0];
    html += '</div><div class="assignmentCount">';
    html += assignment[1];
    if (assignment[2] > 0) {
      html += '/';
      html += assignment[2];
    }
    html += '</div>';
    html += '</div>';
  }

  $('#categoriesList').html(html);

  if (categoriesOK) {
    PF('next').enable();
  } else {
    PF('next').disable();
  }
}

function populateRunTypeMenus() {
  drawingPage = true;
  var runTypeAssignments = JSON.parse($('#newInstrumentForm\\:assignedRunTypes').val());
  for (var i = 0; i < runTypeAssignments.length; i++) {
    var file = runTypeAssignments[i];

    for (var j = 0; j < file['assignments'].length; j++) {
      var category = file["assignments"][j]["category"];
      if (null == category) {
        // Alias
          PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-menu").selectValue('ALIAS');
        $('#' + file["index"] + '-' + file["assignments"][j]["runType"] + '-aliasMenu').show();
        PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-alias").selectValue(file["assignments"][j]["aliasTo"]);
      } else {
        PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-menu").selectValue(category);
        $('#' + file["index"] + '-' + file["assignments"][j]["runType"] + '-aliasMenu').hide();
      }
    }
  }
  drawingPage = false;
}
