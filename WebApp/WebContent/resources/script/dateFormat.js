// Some date formatting functions for getting strings in the UTC timezone.
// If this get too big we should consider switch to a library such as Moment.js
// but at the minute that's overkill.

// Convert a date to yyyy-MM-dd HH:mm:ss format
// in the UTC timezone
function makeUTCDateTime(date) {
  var string = date.getUTCFullYear();
  string += '-';
  string += new String(date.getUTCMonth()).padStart(2, '0');
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
  string += new String(date.getUTCMonth()).padStart(2, '0');
  string += new String(date.getUTCDate()).padStart(2, '0');
  
  return string;
}
