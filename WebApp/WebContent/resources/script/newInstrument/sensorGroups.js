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