// jsColorScale - a Javascript library for making and using color scales.

function ColorScale(scaleArray) {

	this.scaleArray = scaleArray;
	this.scaleMinIndex = scaleArray[0][0];
	this.scaleMaxIndex = scaleArray[scaleArray.length - 1][0];
	this.minValue = this.scaleMinIndex;
	this.maxValue = this.scaleMaxIndex;

	this.font = "sans-serif";
	this.fontSize = "14px";

	this.setValueRange = function(min, max) {
		this.minValue = parseFloat(min);
		this.maxValue = parseFloat(max);
	}

	this.setFont = function(font, size) {
		this.font = font;
		this.fontSize = size;
	}

	this.drawScale = function(scaleContainer, options) {		
		var container = $(scaleContainer);

		var outlierSize = parseInt(options['outlierSize']);

		var svg = '<svg width="100%" height="100%">';

		var gradientId = 'scaleGradient' + window.performance.now();
		
		svg += '<linearGradient id="' + gradientId + '">';
		for (var i = 0; i < scaleArray.length; i++) {
			var percentage = (this.scaleArray[i][0] - this.scaleMinIndex) / (this.scaleMaxIndex - this.scaleMinIndex) * 100;
			svg += '<stop offset="' + percentage + '%" stop-color="' + this.scaleArray[i][1] + '"/>';
		}
		svg += '</linearGradient>';

		svg += '<svg width="100%" height="80%">';

		if (options['outliers'] == 'l' || options['outliers'] == 'b') {
			svg += '<svg viewBox="0 0 100 100" preserveAspectRatio="xMinYMin meet">';
			svg += '<polygon transform="scale(' + outlierSize + ' 1)" fill="' + this.scaleArray[0][1] + '" points="0,50 8.4,0 8.4,100"/>';
			svg += '</svg>';
		}

		if (options['outliers'] == 'r' || options['outliers'] == 'b') {
			svg += '<svg viewBox="0 0 100 100" preserveAspectRatio="xMaxYMax meet">';
			svg += '<polygon transform="rotate(180 50 50) scale(' + outlierSize + ' 1)" fill="' + this.scaleArray[this.scaleArray.length - 1][1] + '" points="0,50 8.4,0 8.4,100"/>';
			svg += '</svg>';
		}

		var barStart = 0;
		var barWidth = 100;

		if (options['outliers'] == 'l' || options['outliers'] == 'b') {
			barStart = outlierSize + 1;
		}

		switch (options['outliers']) {
		case 'l': {
			barStart = outlierSize + 1;
			barWidth = 100 - (outlierSize + 1);
			break;
		}
		case 'r': {
			barWidth = 100 - (outlierSize + 1);
			break;
		}
		case 'b': {
			barStart = outlierSize + 1;
			barWidth = 100 - (outlierSize * 2 + 2);
			break;
		}
		}

		svg += '<rect fill="url(#' + gradientId + ')" x="' + barStart + '%" y="0%" width="' + barWidth + '%" height="100%"/>';

		svg += '</svg>';

		var lowValue = (Math.round(this.percentToRangeValue(0) * (Math.pow(10, options['decimalPlaces']))) / Math.pow(10, options['decimalPlaces'])).toFixed(2);
		var midValue = (Math.round(this.percentToRangeValue(50) * (Math.pow(10, options['decimalPlaces']))) / Math.pow(10, options['decimalPlaces'])).toFixed(2);
		var highValue = (Math.round(this.percentToRangeValue(100) * (Math.pow(10, options['decimalPlaces']))) / Math.pow(10, options['decimalPlaces'])).toFixed(2);

		svg += '<rect fill="#000000" x="' + (outlierSize + 1) + '%" y="80%" width="0.25%" height="30%"/>';
		svg += '<text fill="#000000" x="' + (outlierSize + 1.5) + '%" y="100%" font-family="' + this.font + '" font-size="' + this.fontSize + '">' + lowValue + '</text>';

		svg += '<rect fill="#000000" x="49.825%" y="80%" width="0.25%" height="30%"/>';
		svg += '<text fill="#000000" x="50.25%" y="100%" font-family="' + this.font + '" font-size="' + this.fontSize + '">' + midValue + '</text>';

		svg += '<rect fill="#000000" x="' + (100 - (outlierSize + 1.25)) + '%" y="80%" width="0.25%" height="30%"/>';
		svg += '<text fill="#000000" x="' + (100 - (outlierSize + 1.5)) + '%" y="100%"  font-family="' + this.font + '" font-size="' + this.fontSize + '" text-anchor="end">' + highValue + '</text>';

		svg += '</svg>';


		container.html(svg);
	}


	this.getColor = function(value) {
		
		var result = null;

		if (value < this.minValue) {
			value = this.minValue;
		} else if (value > this.maxValue) {
			value = this.maxValue;
		}

		var valueProportion = (value - this.minValue) / (this.maxValue - this.minValue);
		var scaleValue = this.scaleMinIndex + (this.scaleMaxIndex - this.scaleMinIndex) * valueProportion;

		var preColor = this.getColorBefore(scaleValue);
		var postColor = this.getColorAfter(scaleValue);

		if (preColor[0] == postColor[0]) {
			result = preColor[1];
		} else {
			var colorProportion = (scaleValue - preColor[0]) / (postColor[0] - preColor[0]);
			var redValue = this.interpolateColorComponent(preColor[1].substring(1,3), postColor[1].substring(1,3), colorProportion);
			var greenValue = this.interpolateColorComponent(preColor[1].substring(3,5), postColor[1].substring(3,5), colorProportion);
			var blueValue = this.interpolateColorComponent(preColor[1].substring(5), postColor[1].substring(5), colorProportion);

			result = '#' + redValue + greenValue + blueValue;
		}

		return result;
	}

	this.percentToRangeValue = function(percentage) {
		if (percentage < 0) {
			percentage = 0;
		} else if (percentage > 100) {
			percentage = 100;
		}

		return this.minValue + (this.maxValue - this.minValue) * (percentage / 100);
	}

	this.interpolateColorComponent = function(preValue, postValue, proportion) {
		var preNumber = parseInt(preValue, 16);
		var postNumber = parseInt(postValue, 16);

		var interpNumber = Math.round(preNumber + (postNumber - preNumber) * proportion).toString(16);
		if (interpNumber.length == 1) {
			interpNumber = '0' + interpNumber;
		}

		return interpNumber;
	}

	this.getColorBefore = function(scaleValue) {
		var result = null;
		for (var i = 0; i < this.scaleArray.length; i++) {
			if (this.scaleArray[i][0] <= scaleValue) {
				result = this.scaleArray[i];
			} else {
				break;
			}
		}

		if (result == null) {
			result = this.scaleArray[0];
		}

		return result;
	}

	this.getColorAfter = function(scaleValue) {
		var result = null;
		for (var i = 0; i < this.scaleArray.length; i++) {
			if (this.scaleArray[i][0] > scaleValue) {
				result = this.scaleArray[i];
				break;
			}
		}

		if (result == null) {
			result = this.scaleArray[scaleArray.length - 1];
		}

		return result;
	}
}

function colorOK(color) {
	return /^#[0-9A-F]{6}$/i.test(color);
}