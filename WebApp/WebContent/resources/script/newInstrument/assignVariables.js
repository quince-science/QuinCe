
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
const UNIX = '12';
const SECONDS_FROM_START = '13';

function assignVariablesInit() {
  window.sensorTypes = JSON.parse($('#referenceDataForm\\:sensorTypes').val());
  window.sensorTypesWithFixedDependsQuestionAnswer = JSON.parse($('#referenceDataForm\\:fixedDependsQuestionAnswers').val());
  window.suspendEvents = false;
  selectUpdatedFile();
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

  updateAssignmentsNextButton();
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
  let sensorTypeName = $(this).find('.ui-treenode-label')[0].innerText;
  let sensorTypeId = getSensorTypeID(sensorTypeName);

  // Get column details
  let column = getDragColumn(e);

  switch (sensorTypeId) {
  case RUN_TYPE_SENSOR_TYPE_ID: {
    assignRunType(column.dataFile, column.colIndex);
    break;
  }
  default: {
    let allowAssignment = true;

    // Check non-diagnostic sensors
    if (!sensorTypeName.startsWith('Diagnostic')) {
      let existingEntries = $(this).find('ul[role="group"]').find('li').length;
      // Only allow one column to be assigned
      if (existingEntries > 0) {
        allowAssignment = false;
        PF('tooManyAssignments').show();
      }
    }

    if (allowAssignment) {
      openAssignSensorDialog(getSensorType(sensorTypeId), column);
    }
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
  let colExtractor = /.*---(.*)---(.*)---(.*)---(.*)/;
  let match = colExtractor.exec(colElementId);

  return {'dataFile': match[1], 'colIndex': match[2], 'colName': match[3], 'exampleValue': match[4]};
}

function assignRunType(file, column) {

  let existingRunTypeFile = $('#newInstrumentForm\\:runTypeFile').val();
  if (existingRunTypeFile != '' && existingRunTypeFile != file) {
    PF('multiFileRunType').show();
  } else {
    $('#newInstrumentForm\\:runTypeFile').val(file);
    $('#newInstrumentForm\\:runTypeColumn').val(column);
    assignRunTypeAction(); // PF remote command
  }
}

function openAssignSensorDialog(sensorType, column) {

  $('#newInstrumentForm\\:sensorAssignmentFile').val(column.dataFile);
  $('#newInstrumentForm\\:sensorAssignmentColumn').val(column.colIndex);
  $('#newInstrumentForm\\:sensorAssignmentSensorType').val(sensorType.id);

  $('#sensorAssignmentFileName').text(column.dataFile);
  $('#sensorAssignmentColumnName').text(column.colName);
  $('#sensorAssignmentSensorTypeText').text(sensorType.name);

  $('#newInstrumentForm\\:sensorAssignmentName').val(column.colName);

  if (null == sensorType.dependsQuestion ||
    window.sensorTypesWithFixedDependsQuestionAnswer.includes(sensorType.id)) {
    $('#sensorAssignmentDependsQuestionContainer').hide();
  } else {
    $('#sensorAssignmentDependsQuestion').text(sensorType.dependsQuestion);
    $('#sensorAssignmentDependsQuestionContainer').show();
  }

  $('#sensorAssignmentPrimaryContainer').hide();

  PF('sensorAssignmentAssignButton').enable();
  checkSensorName(column.colName);
  PF('sensorAssignmentDialog').show();
}

function openDateTimeAssignDialog(dateTimeType, column) {

  $('#newInstrumentForm\\:dateTimeFile').val(column.dataFile);
  $('#newInstrumentForm\\:dateTimeColumn').val(column.colIndex);
  $('#newInstrumentForm\\:dateTimeType').val(getDateTimeTypeIndex(dateTimeType));

  window.suspendEvents = true;
  window.DATE_TIME_COLUMN = column;
  window.DATE_TIME_TYPE = dateTimeType;

  let showDialog = false;

  $('#dateTimeFormatContainer').hide();
  $('#dateFormatContainer').hide();
  $('#timeFormatContainer').hide();
  $('#hoursFromStartContainer').hide();

  switch ($('#newInstrumentForm\\:dateTimeType').val()) {
  case DATE_TIME: {
	$('#newInstrumentForm\\:dateTimeValue').val(column.exampleValue);
	prepareDateTimeFormatDialog(); // PF RemoteCommand
    break;
  }
  case DATE: {
    $('#newInstrumentForm\\:dateValue').val(column.exampleValue);
	prepareDateFormatDialog(); // PF RemoteCommand
    break;
  }
  case TIME: {
	$('#newInstrumentForm\\:timeValue').val(column.exampleValue);
	prepareTimeFormatDialog(); // PF RemoteCommand
    break;
  }
  case HOURS_FROM_START:
  case SECONDS_FROM_START: {
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

function showDateTimeFormatDialog() {
  let options = $('#newInstrumentForm\\:dateTimeFormat_input').children();
  if (options.length == 1) {
	PF('dateTimeAssignButton').jq.click();
  } else {
    let column = window.DATE_TIME_COLUMN;
    $('#dateTimeFormatContainer').show();
    PF('dateTimeAssignButton').enable();
    $('#dateTimeFileLabel').html(column.dataFile);
    $('#dateTimeColumnLabel').html(column.colName);
    $('#dateTimeTypeText').html(window.DATE_TIME_TYPE);
    PF('dateTimeAssignmentDialog').initPosition();
    PF('dateTimeAssignmentDialog').show();
  }

  return false;
}

function showDateFormatDialog() {
  let options = $('#newInstrumentForm\\:dateFormat_input').children();
  if (options.length == 1) {
    PF('dateTimeAssignButton').jq.click();
  } else {
    let column = window.DATE_TIME_COLUMN;
	$('#dateFormatContainer').show();
	PF('dateTimeAssignButton').enable();
	$('#dateTimeFileLabel').html(column.dataFile);
	$('#dateTimeColumnLabel').html(column.colName);
	$('#dateTimeTypeText').html(window.DATE_TIME_TYPE);
	PF('dateTimeAssignmentDialog').initPosition();
	PF('dateTimeAssignmentDialog').show();
  }
}

function showTimeFormatDialog() {
  let options = $('#newInstrumentForm\\:timeFormat_input').children();
  if (options.length == 1) {
    PF('dateTimeAssignButton').jq.click();
  } else {
    let column = window.DATE_TIME_COLUMN;
	$('#timeFormatContainer').show();
	PF('dateTimeAssignButton').enable();
	$('#dateTimeFileLabel').html(column.dataFile);
	$('#dateTimeColumnLabel').html(column.colName);
	$('#dateTimeTypeText').html(window.DATE_TIME_TYPE);
	PF('dateTimeAssignmentDialog').initPosition();
	PF('dateTimeAssignmentDialog').show();
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
    case 'Seconds from start of file': {
    result = SECONDS_FROM_START;
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
    case 'UNIX Time': {
    result = UNIX;
      break;
    }
    default: {
    result = -1;
    }
  }

  return result;
}

function getSensorType(sensorId) {
  let result = null;

  for (let i = 0; i < window.sensorTypes.length; i++) {
    let sensorType = window.sensorTypes[i];
    if (sensorType['id'] == sensorId) {
      result = sensorType;
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

function updateStartTime() {

  if (!window.suspendEvents) {
    let lineJson = $('#newInstrumentForm\\:startTimeLine').val();

    if (null == lineJson || lineJson == "") {
      $('#startTimeExtractedLine').text("No matching line found in header");
      $('#startTimeExtractedLine').addClass("error");
    } else {
      let line = JSON.parse(lineJson);
      if (line['string'] == "") {
        $('#startTimeExtractedLine').text("No matching line found in header");
        $('#startTimeExtractedLine').addClass("error");
      } else {
        let lineHtml = "";

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

    let extractedDate = $('#newInstrumentForm\\:startTimeDate').val();
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

function checkSensorNameEvent(event) {
  checkSensorName(event.target.value.trim());
}

function checkSensorName(enteredName) {
  let nameOK = true;

  if (enteredName == "") {
    nameOK = false;
  }

  if (nameOK) {
    if ($.inArray(enteredName.toLowerCase(),
      JSON.parse($('#newInstrumentForm\\:assignedSensorNames').val())) >= 0) {
      
      nameOK = false;
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

function getSensorTypeID(typeName) {
  let result = null;

  for (let i = 0; i < window.sensorTypes.length; i++) {
  if (window.sensorTypes[i].shortName == typeName) {
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

function updateAssignmentsNextButton() {

  highlightFileTabs();

  if (!allFilesAssigned() ||
        $('[data-nodetype$="UNFINISHED_VARIABLE"]').length > 0) {
    PF('next').disable();
  } else {
    PF('next').enable();
  }
}

function highlightFileTabs() {
  let filesAssigned = JSON.parse($('#referenceDataForm\\:filesAssigned').val());
  for (let [file, assigned] of Object.entries(filesAssigned)) {
    let fileTab = getFileTab(file);
    if (null != fileTab) {
      if (assigned) {
	    $(fileTab).removeClass('unassignedFile');
	  } else {
        $(fileTab).addClass('unassignedFile');
	  }
    }
  }
}

function getFileTab(file) {
  let foundTab = null;
  $('span[id$="tabTitle"]').each(function () {
    if ($(this).text() == file) {
      foundTab = $(this).parents('.ui-tabs-header')[0];
	}
  });
  
  return foundTab;
}

function getFileIndex(file) {
  let foundTab = null;
  $('span[id$="tabTitle"]').each(function (index, tabFile) {
    if ($(tabFile).text() == file) {
      foundTab = index;
	}
  });
  
  return foundTab;
}

function allFilesAssigned() {
  let result = true;
  let filesAssigned = JSON.parse($('#referenceDataForm\\:filesAssigned').val());
  for (let [file, assigned] of Object.entries(filesAssigned)) {
    if (!assigned) {
      result = false;
      break;
    }
  }
  
  return result;
}

function selectUpdatedFile() {
  let updatedFile = $('#referenceDataForm\\:updatedFile').val();
  if (null != updatedFile) {
    PF('fileTabs').select(getFileIndex(updatedFile));
  }
}