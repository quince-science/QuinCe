const ALIAS_RUN_TYPE = '-2';

drawingPage = false;

function setRunTypeCategory(fileIndex, runType) {

  if (!drawingPage) {
    let escapedRunType = runType.replace(/(~)/g, "\\$1");
    let runTypeCategory = PF(fileIndex + '-' + runType + '-menu').getSelectedValue();
    let aliasTo = null;

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
  let categoriesOK = true;

  let html = '';

  let assignments = JSON.parse($('#newInstrumentForm\\:assignedCategories').val());

  for (let i = 0; i < assignments.length; i++) {
    let assignment = assignments[i];

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
  let runTypeAssignments = JSON.parse($('#newInstrumentForm\\:assignedRunTypes').val());
  for (let i = 0; i < runTypeAssignments.length; i++) {
    let file = runTypeAssignments[i];

    for (let j = 0; j < file['assignments'].length; j++) {
      let category = file["assignments"][j]["category"];
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
