// jsColorScale - a Javascript library for making and using color scales.

function ColorScale(scaleArray) {

	this.scaleArray = scaleArray;
	this.scaleMinIndex = scaleArray[0][0];
	this.scaleMaxIndex = scaleArray[scaleArray.length - 1][0];
	this.minValue = this.scaleMinIndex;
	this.maxValue = this.scaleMaxIndex;

	this.setValueRange = function(min, max) {
		this.minValue = min;
		this.maxValue = max;
	}

	this.drawScale = function(canvas) {
		
		var ctx = canvas.getContext('2d');
		for (var i = 1; i <= canvas.width; i++) {
			var point1 = i - 1;
			var point2 = i;

			ctx.fillStyle = this.getColor(point1 / canvas.width);
			ctx.fillRect(point1, 0, point2, canvas.height);
		}
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