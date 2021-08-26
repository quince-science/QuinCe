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
	$('#groupNameMessage').hide();
    PF('renameGroupButton').disable();
  } else if ($.inArray(to.toUpperCase(), groupNames) !== -1) {
	$('#groupNameMessage').show();
    PF('renameGroupButton').disable();
  } else {
	$('#groupNameMessage').hide();
	PF('renameGroupButton').enable();
  }
}
