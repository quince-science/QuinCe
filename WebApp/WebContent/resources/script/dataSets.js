TIMELINE_OPTIONS = {
	stack: false,
	showCurrentTime: false,
	selectable: false,
	editable: false
};

var newDataSetItem = {
	id: -100,
	type: 'background',
	className: 'newDataSet',
	start: new Date(),
	end: new Date(),
	content: 'New Data Set',
	title: 'New Date Set'
}


function drawNewDataSet() {
	
	// Remove the existing entry
	timeline.itemsData.getDataSet().remove(newDataSetItem);
	
	newDataSetItem['start'] = PF('pStartDate').getDate();
	newDataSetItem['end'] = PF('pEndDate').getDate();
	
	// If the dates are valid, show the new item
	if (newDataSetItem['end'] > newDataSetItem['start']) {
		timeline.itemsData.getDataSet().add(newDataSetItem);
	}
	
}