// Some date formatting functions for getting strings in the UTC timezone.
// If this get too big we should consider switch to a library such as Moment.js
// but at the minute that's overkill.

// Convert a date to yyyy-MM-dd HH:mm:ss format
// in the UTC timezone
function makeUTCDateTime(date) {
  var string = date.getUTCFullYear();
  string += '-';
  string += new String(date.getUTCMonth() + 1).padStart(2, '0');
  string += '-';
  string += new String(date.getUTCDate()).padStart(2, '0');
  string += ' ';
  string += new String(date.getUTCHours()).padStart(2, '0');
  string += ':';
  string += new String(date.getUTCMinutes()).padStart(2, '0');
  string += ':';
  string += new String(date.getUTCSeconds()).padStart(2, '0');
  
  return string;
}

//Convert a date to yyyyMMdd format
//in the UTC timezone
function makeUTCyyyymmdd(date) {
  var string = date.getUTCFullYear();
  string += new String(date.getUTCMonth() + 1).padStart(2, '0');
  string += new String(date.getUTCDate()).padStart(2, '0');
  
  return string;
}

function formatForTable(date) {
  var string = date.getFullYear();
  string += '-';
  string += new String(date.getMonth() + 1).padStart(2, '0');
  string += '-';
  string += new String(date.getDate()).padStart(2, '0');
  string += ' ';
  string += new String(date.getHours()).padStart(2, '0');
  string += ':';
  string += new String(date.getMinutes()).padStart(2, '0');
  string += ':';
  string += new String(date.getSeconds()).padStart(2, '0');
  
  return string;
}