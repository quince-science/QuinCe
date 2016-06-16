// Flag colors
var badColor = "rgb(255,0,0)";
var questionableColor = "rgb(255,200,0)";
var goodColor = "rgb(0,200,0)";
var assumedGoodColor = "rgb(118,211,118)";
var notSetColor = "rgb(180,180,180)";
var neededColor = "rgb(244,56,244)";
var ignoredColor = "rgb(50,50,50)";

function drawFlagBar(elementId, recordCount, bad, questionable, good, assumedGood, notSet, needed, ignored) {
	var canvas = $(elementId).get(0);
	var ctx = canvas.getContext('2d');
	
	var barFilled = 0;
	
	ctx.fillStyle = badColor;
	ctx.fillRect(barFilled * canvas.width, 0, (bad / recordCount) * canvas.width, canvas.height);
	barFilled += (bad / recordCount);
	
	ctx.fillStyle = questionableColor;
	ctx.fillRect(barFilled * canvas.width, 0, (questionable / recordCount) * canvas.width, canvas.height);
	barFilled += (questionable / recordCount);
	
	ctx.fillStyle = goodColor;
	ctx.fillRect(barFilled * canvas.width, 0, (good / recordCount) * canvas.width, canvas.height);
	barFilled += (good / recordCount);
	
	ctx.fillStyle = assumedGoodColor;
	ctx.fillRect(barFilled * canvas.width, 0, (assumedGood / recordCount) * canvas.width, canvas.height);
	barFilled += (assumedGood / recordCount);
	
	ctx.fillStyle = notSetColor;
	ctx.fillRect(barFilled * canvas.width, 0, (notSet / recordCount) * canvas.width, canvas.height);
	barFilled += (notSet / recordCount);
	
	ctx.fillStyle = neededColor;
	ctx.fillRect(barFilled * canvas.width, 0, (needed / recordCount) * canvas.width, canvas.height);
	barFilled += (needed / recordCount);
	
	ctx.fillStyle = ignoredColor;
	ctx.fillRect(barFilled * canvas.width, 0, (ignored / recordCount) * canvas.width, canvas.height);
	barFilled += (ignored / recordCount);
}
