const IGNORED_RUN_TYPE = '-1';
const ALIAS_RUN_TYPE = '-2';

drawingPage = false;

function setRunTypeCategory(fileIndex, runType) {

  if (!drawingPage) {
    let escapedRunType = runType.replace(/(~)/g, "\\$1");
    let runTypeCategory = PF(fileIndex + '-' + runType + '-menu').getSelectedValue();
	let flushingTime = PF(fileIndex + '-' + runType + '-flushingTime').getValue();
    let aliasTo = null;

    if (runTypeCategory == ALIAS_RUN_TYPE) {
      PF(fileIndex + "-" + escapedRunType + "-alias").getJQ().show();
      aliasTo = PF(fileIndex + '-' + runType + '-alias').getSelectedValue();
    } else {
      PF(fileIndex + "-" + escapedRunType + "-alias").getJQ().hide();
    }
	
	if (runTypeCategory == IGNORED_RUN_TYPE || runTypeCategory == ALIAS_RUN_TYPE) {
      PF(fileIndex + "-" + escapedRunType + "-flushingTime").getJQ().hide();
	} else {
      PF(fileIndex + "-" + escapedRunType + "-flushingTime").getJQ().show();
    }

    $('#newInstrumentForm\\:assignCategoryFile').val(fileIndex);
    $('#newInstrumentForm\\:assignCategoryRunType').val(runType);
    $('#newInstrumentForm\\:assignCategoryCode').val(runTypeCategory);
    $('#newInstrumentForm\\:assignAliasTo').val(aliasTo);
    $('#newInstrumentForm\\:assignFlushingTime').val(flushingTime);
    $('#newInstrumentForm\\:assignCategoryLink').click();
  }
}

function populateRunTypeMenus() {
  drawingPage = true;
  let runTypeAssignments = JSON.parse($('#newInstrumentForm\\:assignedRunTypes').val());
  for (let i = 0; i < runTypeAssignments.length; i++) {
    let file = runTypeAssignments[i];

    for (let j = 0; j < file['assignments'].length; j++) {
      let category = file["assignments"][j]["category"];
	  
	  PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-flushingTime").setValue(file["assignments"][j]["flushingTime"]);
	  
      if (category == parseInt(ALIAS_RUN_TYPE)) {
        // Alias
        PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-menu").selectValue('ALIAS');        
		PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-alias").selectValue(file["assignments"][j]["aliasTo"]);
		PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-alias").getJQ().show();
      } else {
        PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-menu").selectValue(category);
		PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-alias").getJQ().hide();
      }
	  
	  // Flushing time hidden if alias or ignored
      if (category == parseInt(IGNORED_RUN_TYPE) || category == parseInt(ALIAS_RUN_TYPE)) {
        PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-flushingTime").getJQ().hide();
      } else {
        PF(file["index"] + "-" + file["assignments"][j]["runType"] + "-flushingTime").getJQ().show();
      }
    }
  }
  drawingPage = false;
}
