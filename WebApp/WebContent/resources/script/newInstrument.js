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
    spinnerObject.upButton.unbind('mousedown.spinner')
    spinnerObject.downButton.unbind('mousedown.spinner')
}

function enableSpinner(spinnerObject) {
   spinnerObject.input.prop("disabled", false);
   spinnerObject.jq.removeClass("ui-state-disabled");
   spinnerObject.bindEvents()
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
	
	if ($('#newInstrumentForm\\:msgFileDescription').is(':visible')) {
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
function renderAssignments() {
	var sensorsOK = renderSensorAssignments();
	var positionOK = renderPositionAssignments();
	var timeOK = renderDateTimeAssignments();
	
	if (sensorsOK && positionOK && timeOK) {
		PF('next').enable();
	} else {
		PF('next').disable();
	}
}

function renderSensorAssignments() {
	var sensorsOK = true;
	
	var html = '';
	
	var assignments = JSON.parse($('#newInstrumentForm\\:sensorAssignments').val());
	
	for (var i = 0; i < assignments.length; i++) {
		var assignment = assignments[i];
		
		html += '<div class="assignmentListEntry';
		if (assignment['required']) {
			html += ' assignmentRequired';
			sensorsOK = false;
		}
		html += '"><div class="assignmentLabel">';
		html += assignment['name'];
		html += '</div><div class="assignmentCount">';
		html += assignment['assignments'].length;
		if (!assignment['many']) {
			html += '/1';
		}
		html += '</div>';
		html += '</div>';
	}
	
	$('#assignmentsList').html(html);
	
	return sensorsOK;
}

function renderDateTimeAssignments() {
	var allTimesOK = true;
	
	var assignments = JSON.parse($('#newInstrumentForm\\:timePositionAssignments').val());
	
	// Loop through the files
	for (var i = 0; i < assignments.length; i++) {
		var assignment = assignments[i];

		var timeOK = true;
		var fieldsAssigned = false;
		var timeHtml = '';
	
		var dateTimeAssignments = assignment['dateTime'];
		
		
		timeHtml += '<table>';
		
		for (var j = 0; j < dateTimeAssignments.length; j++) {
			var dtAssignment = dateTimeAssignments[j];
			
			timeHtml += '<tr><td><label class="ui-outputlabel ui-widget">';
			timeHtml += dtAssignment['name'];
			timeHtml += ':</label></td>';
			
			if (dtAssignment['column'] == -1) {
				timeHtml += '<td class="error">Not assigned</td>';
				timeOK = false;
			} else {
				timeHtml += '<td>';
				timeHtml += filesAndColumns[i]['columns'][dtAssignment['column']];
				timeHtml += '</td>';
			}
			
			timeHtml += '</tr>';
			
			if (dtAssignment['column'] != -1) {
				fieldsAssigned = true;
			}
		}
		
		timeHtml += '</table>';
		
	
		if (!fieldsAssigned) {
			timeHtml = 'No columns assigned';
			timeOK = false;
		}

		$('#dateTimeColumns-'+ i).html(timeHtml);

		if (timeOK) {
			$('#dateTimeColumns-' + i).closest("fieldset").removeClass("invalidFileAssignment");
		} else {
			$('#dateTimeColumns-' + i).closest("fieldset").addClass("invalidFileAssignment");
			allTimesOK = false;
		}
	}
		
	return timeOK;
}

function renderPositionAssignments() {
	var positionOK = true;
	var positionsAssigned = 0;
	
	var assignments = JSON.parse($('#newInstrumentForm\\:timePositionAssignments').val());
	
	for (var i = 0; i < assignments.length; i++) {
		var assignment = assignments[i];
		
		positionHtml = '';
		if (assignment['longitude']['valueColumn'] == -1 && assignment['latitude']['valueColumn'] == -1) {
			positionHtml += 'No columns assigned';
		} else {
			positionsAssigned++;
			
			positionHtml += '<h4>Longitude</h4>';
			positionHtml += '<table><tr><td><label class="ui-outputlabel ui-widget">Column:</label></td>';
			if (assignment['longitude']['valueColumn'] == -1) {
				positionHtml += '<td class="error">Not assigned</td>';
				positionOK = false;
			} else {
				positionHtml += '<td>';
				positionHtml += filesAndColumns[i]['columns'][assignment['longitude']['valueColumn']];
				positionHtml += '</td></tr><tr><td><label class="ui-outputlabel ui-widget">Format:</label></td><td>';
				
				switch(assignment['longitude']['format']) {
				case 0: {
					positionHtml += '0° to 360°';
					break;
				}
				case 1: {
					positionHtml += '-180° to 180°';
					break;
				}
				case 2: {
					positionHtml += '0° to 180°';
					break;
				}
				}
				
				positionHtml += '</td></tr>';
				
				if (assignment['longitude']['hemisphereRequired']) {
					positionHtml += '<tr><td><label class="ui-outputlabel ui-widget">Hemisphere:</label></td>';
					if (assignment['longitude']['hemisphereColumn'] == -1) {
						positionHtml += '<td class="error">Not assigned</td>';
						positionOK = false;
					} else {
						positionHtml += '<td>';
						positionHtml += filesAndColumns[i]['columns'][assignment['longitude']['hemisphereColumn']];
						positionHtml += '</td>';
					}
					positionHtml += '</tr>';
				}
			}
			positionHtml += '</table>';
			
			positionHtml += '<h4>Latitude</h4>';
			positionHtml += '<table><tr><td><label class="ui-outputlabel ui-widget">Column:</label></td>';
			if (assignment['latitude']['valueColumn'] == -1) {
				positionHtml += '<td class="error">Not assigned</td>';
				positionOK = false;
			} else {
				positionHtml += '<td>';
				positionHtml += filesAndColumns[i]['columns'][assignment['latitude']['valueColumn']];
				positionHtml += '</td></tr><tr><td><label class="ui-outputlabel ui-widget">Format:</label></td><td>';
				
				switch(assignment['latitude']['format']) {
				case 0: {
					positionHtml += '-90° to 90°';
					break;
				}
				case 1: {
					positionHtml += '0° to 90°';
					break;
				}
				}
				
				positionHtml += '</td></tr>';
				
				if (assignment['latitude']['hemisphereRequired']) {
					positionHtml += '<tr><td><label class="ui-outputlabel ui-widget">Hemisphere:</label></td>';
					if (assignment['latitude']['hemisphereColumn'] == -1) {
						positionHtml += '<td class="error">Not assigned</td>';
						positionOK = false;
					} else {
						positionHtml += '<td>';
						positionHtml += filesAndColumns[i]['columns'][assignment['latitude']['hemisphereColumn']];
						positionHtml += '</td>';
					}
					positionHtml += '</tr>';
				}
			}
			positionHtml += '</table>';
			
		}
		
		$('#positionColumns-' + i).html(positionHtml);
		if (positionOK) {
			$('#positionColumns-' + i).closest('fieldset').removeClass('invalidFileAssignment');
		} else {
			timePositionOK = false;
			$('#positionColumns-' + i).closest('fieldset').addClass('invalidFileAssignment');
		}
	}
	
	if (positionsAssigned == 0) {
		for (var i = 0; i < filesAndColumns.length; i++) {
			$('#positionColumns-' + i).closest('fieldset').addClass('invalidFileAssignment');
		}
	}
	
	return positionOK;
}

function buildMainAssignmentMenu(file, column) {
	var columnAssignment = getColumnAssignment(file, column);
	var timePositionAssignments = JSON.parse($('#newInstrumentForm\\:timePositionAssignments').val());

	var menuHtml = '';
	
	if (null != columnAssignment) {
		// Add the current assignment here
	}
	
	menuHtml += '<ul class="ui-menu-list ui-helper-reset">';
	menuHtml += makeParentMenuItem('DATETIMESUBMENU', 'Date/Time', file, column, 'dateTimeMenu');
	
	if (timePositionAssignments[file]['longitude']['valueColumn'] != -1) {
		menuHtml += makeDisabledMenuItem('Longitude');
	} else {
		menuHtml += makeMenuItem('POS_longitude', 'Longitude', file, column);
	}
	

	if (hemisphereRequired(file, 'longitude')) {
		if (timePositionAssignments[file]['longitude']['hemisphereColumn'] != -1) {
			menuHtml += makeDisabledMenuItem('Longitude - Hemisphere');
		} else {
			menuHtml += makeMenuItem('POS_longitude_hemisphere', 'Longitude - Hemisphere', file, column);
		}
	}
	
	if (timePositionAssignments[file]['latitude']['valueColumn'] != -1) {
		menuHtml += makeDisabledMenuItem('Latitude');
	} else {
		menuHtml += makeMenuItem('POS_latitude', 'Latitude', file, column);
	}
		
	if (hemisphereRequired(file, 'latitude')) {
		if (timePositionAssignments[file]['latitude']['hemisphereColumn'] != -1) {
			menuHtml += makeDisabledMenuItem('Latitude - Hemisphere');
		} else {
			menuHtml += makeMenuItem('POS_latitude_hemisphere', 'Latitude - Hemisphere', file, column);
		}
	}
	
	var sensorAssignments = JSON.parse($('#newInstrumentForm\\:sensorAssignments').val());
	for (var i = 0; i < sensorAssignments.length; i++) {
		if (!sensorAssignments[i]['many'] && sensorAssignments[i]['assignments'].length > 0) {
			menuHtml += makeDisabledMenuItem(sensorAssignments[i]['name']);
		} else {
			menuHtml += makeMenuItem(sensorAssignments[i]['name'], sensorAssignments[i]['name'], file, column);
		}
	}
	
	menuHtml += '</ul>';
	
	$('#mainAssignmentMenu').html(menuHtml);
	
	// item hover styles
	$('#mainAssignmentMenu').find('.ui-menuitem-link').hover(
			function() {$(this).addClass('ui-state-hover')},
			function() {$(this).removeClass('ui-state-hover')}
	);
}

function buildDateTimeAssignmentMenu(file, column) {

	var timeAssignments = JSON.parse($('#newInstrumentForm\\:timePositionAssignments').val())[file]['dateTime'];

	var menuHtml = '<ul class="ui-menu-list ui-helper-reset">';

	for (var i = 0; i < timeAssignments.length; i++) {
		var assignment = timeAssignments[i];
		
		if (assignment['column'] == -1) {
			menuHtml += makeMenuItem('DATETIME_' + assignment['name'], assignment['name'], file, column);
		} else {
			menuHtml += makeDisabledMenuItem(assignment['name']);
		}
	}
	
	menuHtml += '</ul>';

	$('#dateTimeMenu').html(menuHtml);
	
	// item hover styles
	$('#dateTimeMenu').find('.ui-menuitem-link').hover(
			function() {$(this).addClass('ui-state-hover')},
			function() {$(this).removeClass('ui-state-hover')}
	);
}

function makeDisabledMenuItem(label) {
	return '<li class="ui-menuitem ui-widget ui-corner-all assignmentMenuItem assignmentMenuItemDisabled">' + label + '</li>';
}

function makeMenuItem(item, label, file, column) {
	return '<li class="ui-menuitem ui-widget ui-corner-all"><a href="#" class="ui-menuitem-link ui-corner-all ui-menuitem-text assignmentMenuItem" onclick="startAssign(\'' + item + '\',' + file + ',' + column + ')">' + label + '</a></li>';
}

function makeParentMenuItem(item, label, file, column, subMenu) {
	return '<li id="' + item + '_menuItem" class="ui-menuitem ui-menuitem-link ui-widget ui-corner-all"><div style="float: right">▸</div><a href="#" class="ui-menuitem-link ui-corner-all ui-menuitem-text assignmentMenuItem" onclick="startAssign(\'' + item + '\',' + file + ',' + column + ')">' + label + '</a></li></li>';
}

function showMainAssignmentMenu(event, file, column) {
	event.stopPropagation();
	buildMainAssignmentMenu(file, column);
	positionMainAssignmentMenu(event.target);
	$('#mainAssignmentMenu').removeClass('ui-overlay-hidden').addClass('ui-overlay-visible');
}

function hideMainAssignmentMenu() {
	$('#mainAssignmentMenu').removeClass('ui-overlay-visible').addClass('ui-overlay-hidden');
}

function hideDateTimeMenu() {
	$('#dateTimeMenu').removeClass('ui-overlay-visible').addClass('ui-overlay-hidden');
}

function positionMainAssignmentMenu(source) {
	
	var tableContainer = $(source).closest('.sampleDataTableContainer');
	var rightLimit = tableContainer.offset().left + tableContainer.width(); 
	
	var leftPos = $(source).offset().left;
	if (leftPos + $('#mainAssignmentMenu').width() > rightLimit) {
		leftPos = rightLimit - $('#mainAssignmentMenu').width();
	}
	
	var topPos = $(source).offset().top + $(source).height();
	
	$('#mainAssignmentMenu').css({'left': leftPos + 'px', 'top': topPos + 'px'});
}

function positionDateTimeAssignmentMenu() {
	var parentMenuItem = $('#DATETIMESUBMENU_menuItem')[0];
	var parentMenu = $('#mainAssignmentMenu');
	
	var leftPos = parseFloat(parentMenu.css('left')) + parentMenu.width() + 10;
	if (leftPos + $('#dateTimeMenu').width() > $(window).width()) {
		leftPos = parseFloat(parentMenu.css('left')) - $('#dateTimeMenu').width() - 10;
	}
	
	var topPos = parseFloat(parentMenu.css('top'));
	
	$('#dateTimeMenu').css({'left': leftPos + 'px', 'top': topPos + 'px'});
}

function getColumnAssignment(file, column) {
	return null;
}

function startAssign(item, file, column) {
	if (item == 'DATETIMESUBMENU') {
		showDateTimeSubmenu(file, column);
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
	} else {
		openAssignSensorDialog(item, file, column);
	}
}

function openAssignSensorDialog(sensor, file, column) {
	
	$('#newInstrumentForm\\:sensorAssignmentFile').val(filesAndColumns[file]['description']);
	$('#newInstrumentForm\\:sensorAssignmentColumn').val(column);
	$('#newInstrumentForm\\:sensorAssignmentSensorType').val(sensor);
	
	$('#sensorAssignmentFileName').text(filesAndColumns[file]['description']);
	$('#sensorAssignmentColumnName').text(filesAndColumns[file]['columns'][column]);
	$('#sensorAssignmentSensorTypeText').text(sensor);
	
	if (canBeNamed(sensor)) {
		$('#sensorAssignmentNameContainer').show();
	} else {
		$('#sensorAssignmentNameContainer').hide();
	}
	
	var dependsQuestion = getDependsQuestion(sensor);
	if (null == dependsQuestion) {
		$('#sensorAssignmentDependsQuestionContainer').hide();
	} else {
		$('#sensorAssignmentDependsQuestion').text(dependsQuestion);
		$('#sensorAssignmentDependsQuestionContainer').show();
	}
	
	if (usePrimary(sensor)) {
		$('#sensorAssignmentPrimaryContainer').show();
	} else {
		$('#sensorAssignmentPrimaryContainer').hide();
	}
	
	if (canBePostCalibrated(sensor)) {
		$('#sensorAssignmentPostCalibration').show();
	} else {
		$('#sensorAssignmentPostCalibration').hide();
	}
	
	PF('sensorAssignmentDialog').show();
}

function getDependsQuestion(sensor) {
	var question = null;
	
	var assignments = JSON.parse($('#newInstrumentForm\\:sensorAssignments').val());

	for (var i = 0; i < assignments.length; i++) {
		var assignment = assignments[i];
		if (assignment['name'] == sensor) {
			question = assignment['dependsQuestion'];
			break;
		}
	}
	
	return question;
}

//Primary/Fallback must be specified if (a) many sensors can
//be assigned, and (b) they will be averaged
function usePrimary(sensor) {
	var result = true;
	
	var assignments = JSON.parse($('#newInstrumentForm\\:sensorAssignments').val());

	for (var i = 0; i < assignments.length; i++) {
		var assignment = assignments[i];
		if (assignment['name'] == sensor) {
			result = assignment['many'] && assignment['averaged'];
			break;
		}
	}
	
	return result;
}

function canBePostCalibrated(sensor) {
	var result = true;
	
	var assignments = JSON.parse($('#newInstrumentForm\\:sensorAssignments').val());

	for (var i = 0; i < assignments.length; i++) {
		var assignment = assignments[i];
		if (assignment['name'] == sensor) {
			result = assignment['postCalibrated'];
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
	renderSensorAssignments();
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
	
	var assignments = JSON.parse($('#newInstrumentForm\\:timePositionAssignments').val());
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

function showDateTimeSubmenu(file, column) {
	event.stopPropagation();
	buildDateTimeAssignmentMenu(file, column);
	positionDateTimeAssignmentMenu();
	$('#dateTimeMenu').removeClass('ui-overlay-hidden').addClass('ui-overlay-visible');
}

function openDateTimeDialog(item, file, column) {
	$('#newInstrumentForm\\:dateTimeFile').val(file);
	$('#newInstrumentForm\\:dateTimeColumn').val(column);

	var variableName = item.substring(9, item.length);
	$('#newInstrumentForm\\:dateTimeVariable').val(variableName);
	
	$('#dateTimeAssignmentFile').text(filesAndColumns[file]['description']);
	$('#dateTimeAssignmentColumn').text(filesAndColumns[file]['columns'][column]);
	$('#dateTimeAssignmentVariable').text(variableName);
	
	$('#dateTimeFormatContainer').hide();
	$('#dateFormatContainer').hide();
	$('#timeFormatContainer').hide();
	$('#jdayFromStartContainer').hide();
	PF('jdayFromStartAssign').disable();
	
	switch (variableName) {
	case "Combined Date and Time": {
		$('#dateTimeFormatContainer').show();
		break;
	}
	case "Julian Day/Time from start of file": {
		$('#jdayFromStartContainer').show();
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
		PF('jdayFromStartAssign').disable();
	} else {
		$('#startTimeExtractedDate').text(extractedDate);
		$('#startTimeExtractedDate').removeClass("error");
		$('#startTimeExtractedDate').addClass("highlight");
		PF('jdayFromStartAssign').enable();
	}
}
