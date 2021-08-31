function showRenameGroupDialog(group) {
  $('#newInstrumentForm\\:renameGroupFrom').val(group);
  $('#newInstrumentForm\\:renameGroupTo').val(group);
  $('#newInstrumentForm\\:renameGroupTo').select();

  PF('renameGroupDialog').show();
  checkRenameGroup();
}

function groupRenamed() {
  let from = $('#newInstrumentForm\\:renameGroupFrom').val().trim();
  let to = $('#newInstrumentForm\\:renameGroupTo').val().trim();

  let groupDiv = $('#groupSection_' + from);

  groupDiv.attr('id', 'groupSection_' + to);
  groupDiv.children('.groupName').children('span').text(to);
  PF('renameGroupDialog').hide();
}

function checkRenameGroup() {
  let from = $('#newInstrumentForm\\:renameGroupFrom').val().trim();
  let to = $('#newInstrumentForm\\:renameGroupTo').val().trim();

  let groupNames = JSON.parse($('#newInstrumentForm\\:groupNames').val())
    .map(function(elem) { return elem.toUpperCase(); }); 

  if (to === '' || from.toUpperCase() === to.toUpperCase()) {
	$('#renameGroupNameMessage').hide();
    PF('renameGroupButton').disable();
  } else if ($.inArray(to.toUpperCase(), groupNames) !== -1) {
	$('#renameGroupNameMessage').show();
    PF('renameGroupButton').disable();
  } else {
	$('#renameGroupNameMessage').hide();
	PF('renameGroupButton').enable();
  }
}

function showAddDialog(afterGroup) {
  $('#newInstrumentForm\\:afterGroup').val(afterGroup);
  $('#newInstrumentForm\\:addGroupName').val('');
  PF('addGroupDialog').show();
  checkAddGroup();
}

function checkAddGroup() {
  let name = $('#newInstrumentForm\\:addGroupName').val().trim();

  let groupNames = JSON.parse($('#newInstrumentForm\\:groupNames').val())
    .map(function(elem) { return elem.toUpperCase(); }); 

  if (name === '') {
	$('#addGroupNameMessage').hide();
    PF('addGroupButton').disable();
  } else if ($.inArray(name.toUpperCase(), groupNames) !== -1) {
	$('#addGroupNameMessage').show();
    PF('addGroupButton').disable();
  } else {
	$('#addGroupNameMessage').hide();
	PF('addGroupButton').enable();
  }
}

function deleteGroup(group) {
  $('#newInstrumentForm\\:deleteGroupName').val(group);
  $('#groupNameText').text(group);
  PF('deleteGroupDialog').show();
}

/////////////////////////////////////
// Drag/Drop stuff

function setupDragDropEvents() {
  window.suspendEvents = false;

  $('.draggable').on('dragstart', handleDragStart);
  $('.draggable').on('dragend', handleDragEnd);

  $('.dropTarget')
    .on('dragover', handleDragOver)
    .on('dragenter', handleDragEnter)
    .on('dragleave', handleDragLeave)
    .on('drop', handleDrop);
}

function handleDragStart(e) {
  e.originalEvent.dataTransfer.setData("text/plain", e.target.innerText);
  e.originalEvent.dataTransfer.dropEffect = "link";
}

function handleDragEnd(e) {
}

function handleDragOver(e) {
  e.preventDefault();
  $(this).addClass('sensorGroupHover');
  e.originalEvent.dataTransfer.dropEffect = "link";
}

function handleDragEnter(e) {
  $(this).addClass('sensorGroupHover');
}

function handleDragLeave(e) {
  $(this).removeClass('sensorGroupHover');
}

function handleDrop(e) {
  e.preventDefault();
  $('#newInstrumentForm\\:moveSensorName')
    .val(e.originalEvent.dataTransfer.getData("text/plain"));

  let targetGroup = $(e.target)
    .parents('div[id^=groupSection_]')
    .attr('id').substr(13);
  
  $('#newInstrumentForm\\:moveSensorGroup').val(targetGroup);
  
  $('#newInstrumentForm\\:moveSensorLink').click();
}