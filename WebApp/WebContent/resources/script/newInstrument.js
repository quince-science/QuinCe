//************************************************
// Global variables:
//************************************************

allTimesOK = false;
drawingPage = false;

const RUN_TYPE_SENSOR_TYPE_ID = -1;
const ALIAS_RUN_TYPE = '-2';

// DATE-TIME types
const DATE_TIME = '0';
const HOURS_FROM_START = '1';
const DATE = '2';
const YEAR = '3';
const JDAY_TIME = '4';
const JDAY = '5';
const MONTH = '6';
const DAY = '7';
const TIME = '8';
const HOUR = '9';
const MINUTE = '10';
const SECOND = '11';

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

function assignRunType(file, column) {
  $('#newInstrumentForm\\:sensorAssignmentFile').val(file);
  $('#newInstrumentForm\\:sensorAssignmentColumn').val(column);
  $('#newInstrumentForm\\:sensorAssignmentSensorType').val(RUN_TYPE_SENSOR_TYPE_ID);
  $('#newInstrumentForm\\:sensorAssignmentName').val('Run Type');
  $('#newInstrumentForm\\:sensorAssignmentMissingValue').val('');
  PF('sensorAssignmentAssignButton').jq.click();
}

function openAssignSensorDialog(sensorType, column) {

  $('#newInstrumentForm\\:sensorAssignmentFile').val(column.dataFile);
  $('#newInstrumentForm\\:sensorAssignmentColumn').val(column.colIndex);
  $('#newInstrumentForm\\:sensorAssignmentSensorType').val(sensorType.id);

  $('#sensorAssignmentFileName').text(column.dataFile);
  $('#sensorAssignmentColumnName').text(column.colName);
  $('#sensorAssignmentSensorTypeText').text(sensorType.name);

  $('#newInstrumentForm\\:sensorAssignmentName').val(column.colName);

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

function openDateTimeAssignDialog(dateTimeType, column) {

  $('#newInstrumentForm\\:dateTimeFile').val(column.dataFile);
  $('#newInstrumentForm\\:dateTimeColumn').val(column.colIndex);
  $('#newInstrumentForm\\:dateTimeType').val(getDateTimeTypeIndex(dateTimeType));

  window.suspendEvents = true;
  let showDialog = false;

  $('#dateTimeFormatContainer').hide();
  $('#dateFormatContainer').hide();
  $('#timeFormatContainer').hide();
  $('#hoursFromStartContainer').hide();

  switch ($('#newInstrumentForm\\:dateTimeType').val()) {
  case DATE_TIME: {
    PF('dateTimeFormat').selectValue('yyyy-MM-dd HH:mm:ss');
    $('#dateTimeFormatContainer').show();
    PF('dateTimeAssignButton').enable();
    showDialog = true;
    break;
  }
  case DATE: {
    PF('dateFormat').selectValue('yyyy-MM-dd');
    $('#dateFormatContainer').show();
    PF('dateTimeAssignButton').enable();
    showDialog = true;
    break;
  }
  case TIME: {
    PF('timeFormat').selectValue('HH:mm:ss');
    $('#timeFormatContainer').show();
    PF('dateTimeAssignButton').enable();
    showDialog = true;
    break;
  }
  case HOURS_FROM_START: {
    PF('startTimePrefix').jq.val('');
    PF('startTimeSuffix').jq.val('');
    PF('startTimeFormat').selectValue('MMM dd yyyy HH:mm:ss');
    $('#newInstrumentForm\\:startTimeLine').val('');
    $('#newInstrumentForm\\:startTimeDate').val('');
    $('#startTimeExtractedLine').text('');
    $('#startTimeExtractedDate').text('');
    $('#hoursFromStartContainer').show();
    updateStartTime();
    showDialog = true;
    break;
  }
  default: {
    showDialog = false;
  }
  }

  window.suspendEvents = false;

  if (showDialog) {
    $('#dateTimeFileLabel').html(column.dataFile);
    $('#dateTimeColumnLabel').html(column.colName);
    $('#dateTimeTypeText').html(dateTimeType);
    PF('dateTimeAssignmentDialog').initPosition();
    PF('dateTimeAssignmentDialog').show();
  } else {
    PF('dateTimeAssignButton').jq.click();
  }
}

function getDateTimeTypeIndex(dateTimeType) {

  let result = -1;

  switch (dateTimeType) {
    case 'Combined Date and Time': {
    result = DATE_TIME;
      break;
    }
    case 'Hours from start of file': {
    result = HOURS_FROM_START;
      break;
    }
    case 'Date': {
    result = DATE;
      break;
    }
    case 'Year': {
    result = YEAR;
      break;
    }
    case 'Julian Day with Time': {
    result = JDAY_TIME;
      break;
    }
    case 'Julian Day': {
    result = JDAY;
      break;
    }
    case 'Month': {
    result = MONTH;
      break;
    }
    case 'Day': {
    result = DAY;
      break;
    }
    case 'Time': {
    result = TIME;
      break;
    }
    case 'Hour': {
    result = HOUR;
      break;
    }
    case 'Minute': {
    result = MINUTE;
      break;
    }
    case 'Second': {
    result = SECOND;
      break;
    }
    default: {
    result = -1;
    }
  }

  return result;
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

function dateTimeAssigned() {
  PF('dateTimeAssignmentDialog').hide();
  setupDragDropEvents();
}

function positionAssigned() {
  PF('longitudeAssignmentDialog').hide();
  PF('latitudeAssignmentDialog').hide();
  setupDragDropEvents();
}

function openLongitudeDialog(column) {
  $('#newInstrumentForm\\:longitudeFile').val(column.dataFile);
  $('#newInstrumentForm\\:longitudeColumn').val(column.colIndex);

  $('#longitudeAssignmentFile').text(column.dataFile);
  $('#longitudeAssignmentColumn').text(column.colName);

  PF('longitudeAssignmentDialog').initPosition();
  PF('longitudeAssignmentDialog').show();
}

function openLatitudeDialog(column) {
  $('#newInstrumentForm\\:latitudeFile').val(column.dataFile);
  $('#newInstrumentForm\\:latitudeColumn').val(column.colIndex);

  $('#latitudeAssignmentFile').text(column.dataFile);
  $('#latitudeAssignmentColumn').text(column.colName);

  PF('latitudeAssignmentDialog').initPosition();
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
    PF('dateTimeAssignButton').disable();
  } else {
    PF('dateTimeAssignButton').enable();
  }

  PF('dateTimeAssignmentDialog').show();
}

function updateStartTime() {

  if (!window.suspendEvents) {
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
      PF('dateTimeAssignButton').disable();
    } else {
      $('#startTimeExtractedDate').text(extractedDate);
      $('#startTimeExtractedDate').removeClass("error");
      $('#startTimeExtractedDate').addClass("highlight");
      PF('dateTimeAssignButton').enable();
    }
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
  window.suspendEvents = false;
  setupDragDropEvents();
}

function setupDragDropEvents() {
  $('div[id^=col-]').on('dragstart', handleColumnDragStart);
  $('div[id^=col-]').on('dragend', handleColumnDragEnd);

  $('.sensorTypeDropTarget')
    .on('dragover', handleColumnDragOver)
    .on('dragenter', handleColumnDragEnter)
    .on('dragleave', handleColumnDragLeave)
    .on('drop', handleSensorTypeColumnDrop);

  $('.dateTimeDropTarget')
    .on('dragover', handleColumnDragOver)
    .on('dragenter', handleColumnDragEnter)
    .on('dragleave', handleColumnDragLeave)
    .on('drop', handleDateTimeColumnDrop);

  $('.longitudeDropTarget')
    .on('dragover', handleColumnDragOver)
    .on('dragenter', handleColumnDragEnter)
    .on('dragleave', handleColumnDragLeave)
    .on('drop', handleLongitudeColumnDrop);

  $('.latitudeDropTarget')
    .on('dragover', handleColumnDragOver)
    .on('dragenter', handleColumnDragEnter)
    .on('dragleave', handleColumnDragLeave)
    .on('drop', handleLatitudeColumnDrop);

  $('.hemisphereDropTarget')
    .on('dragover', handleColumnDragOver)
    .on('dragenter', handleColumnDragEnter)
    .on('dragleave', handleColumnDragLeave)
    .on('drop', handleHemisphereColumnDrop);
}

function handleColumnDragStart(e) {
  e.originalEvent.dataTransfer.setData("text/plain", e.target.id);
  e.originalEvent.dataTransfer.dropEffect = "link";
  disableOtherDateTimeNodes(getDragColumn(e).dataFile);
}

function handleColumnDragEnd(e) {
  enableAllDateTimeNodes();
}

function handleColumnDragEnter(e) {
  if ($(this).hasClass('disabled') == false) {
    $(this).addClass('dropTargetHover');
  }
}

function handleColumnDragLeave(e) {
  $(this).removeClass('dropTargetHover');
}

function handleColumnDragOver(e) {
  e.preventDefault();
  if ($(this).hasClass('disabled') == false) {
    $(this).addClass('dropTargetHover');
    e.originalEvent.dataTransfer.dropEffect = "link";
  }
}

function handleSensorTypeColumnDrop(e) {
  e.preventDefault();
  $(this).removeClass('dropTargetHover');

  // Get sensor type
  let sensorTypeName = $(this)[0].innerText;
  let sensorTypeId = getSensorTypeID(sensorTypeName);

  // Get column details
  let column = getDragColumn(e);

  switch (sensorTypeId) {
  case RUN_TYPE_SENSOR_TYPE_ID: {
    assignRunType(column.dataFile, column.colName);
    break;
  }
  default: {
    openAssignSensorDialog(getSensorType(sensorTypeId), column);
  }
  }
}

function handleDateTimeColumnDrop(e) {
  e.preventDefault();
  $(this).removeClass('dropTargetHover');

  if ($(this).hasClass('disabled') == false) {
    let column = getDragColumn(e);
    openDateTimeAssignDialog($(this)[0].innerText, column);
  }
}

function handleLongitudeColumnDrop(e) {
  e.preventDefault();
  openLongitudeDialog(getDragColumn(e));
}

function handleLatitudeColumnDrop(e) {
  e.preventDefault();
  openLatitudeDialog(getDragColumn(e));
}

function handleHemisphereColumnDrop(e) {
  e.preventDefault();

  let column = getDragColumn(e);
  let hemisphereCoordinate = $(this)[0].innerText.split(' ')[0];

  $('#newInstrumentForm\\:hemisphereFile').val(column.dataFile);
  $('#newInstrumentForm\\:hemisphereColumn').val(column.colIndex);
  $('#newInstrumentForm\\:hemisphereCoordinate').val(hemisphereCoordinate);
  assignHemisphereAction(); // PF remotecommand
}

function getDragColumn(e) {
  let colElementId = e.originalEvent.dataTransfer.getData("text/plain");
  let colExtractor = /.*---(.*)---(.*)---(.*)/;
  let match = colExtractor.exec(colElementId);

  return {'dataFile': match[1], 'colIndex': match[2], 'colName': match[3]};
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

function removeSensorAssignment(sensorType, file, column) {
  $('#newInstrumentForm\\:removeAssignmentSensorType').val(sensorType);
  $('#newInstrumentForm\\:removeAssignmentDataFile').val(file);
  $('#newInstrumentForm\\:removeAssignmentColumn').val(column);
  removeSensorAssignmentCommand(); // PrimeFaces remoteCommand
}

function removeDateTimeAssignment(file, column) {
  $('#newInstrumentForm\\:dateTimeFile').val(file);
  $('#newInstrumentForm\\:dateTimeColumn').val(column);
  removeDateTimeAssignmentAction(); // PF RemoteCommand
}

function disableOtherDateTimeNodes(dataFile) {
  $('[data-nodetype$="FINISHED_DATETIME"]').each(function() {
  nodeDataFile = $(this)[0].innerText.split('\n')[0];

    if (nodeDataFile != dataFile) {
      $(this).find('.unassignedDateTimeType').each(function() {
      $(this).addClass('disabled');
      });
    }
  });
}

function enableAllDateTimeNodes() {
  $('[data-nodetype$="FINISHED_DATETIME"]')
    .find('.unassignedDateTimeType').each(function() {

    $(this).removeClass('disabled');
  });
}

function removePositionAssignment(file, column) {
  $('#newInstrumentForm\\:removeAssignmentDataFile').val(file);
  $('#newInstrumentForm\\:removeAssignmentColumn').val(column);
  removePositionAssignmentAction(); // PF remote command
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
