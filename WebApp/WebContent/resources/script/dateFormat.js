const MAX_MONTH_DATES = [31, -1, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

// Some date formatting functions for getting strings in the UTC timezone.
// If this get too big we should consider switch to a library such as Moment.js
// but at the minute that's overkill.

// Also stuff for PrimeFaces DatePicker

// Convert a date to yyyy-MM-dd HH:mm:ss format
// in the UTC timezone
function makeUTCDateTime(date) {
  let string = date.getUTCFullYear();
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
  let string = date.getUTCFullYear();
  string += new String(date.getUTCMonth() + 1).padStart(2, '0');
  string += new String(date.getUTCDate()).padStart(2, '0');
  
  return string;
}

function formatForTable(date) {
  let string = date.getFullYear();
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

/*
 * Functions for PrimeFaces DatePicker
 */
function isLeapYear(year) {
  return ((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0);
}

function autoSelectDay(newMonthOneBased, newYear) {
  let pickerId = event.currentTarget.id;
  let regex = /.*:(.*)_panel/
  let inputName = regex.exec(pickerId)[1];

  let currentDate = PF(inputName).getDate();
  if (currentDate) {
    let newMonth = newMonthOneBased - 1;
    let newDay = currentDate.getDate();

    let maxDay = -1;
    if (newMonth == 1) {
      if (isLeapYear(newYear)) {
        maxDay = 29;
      } else {
        maxDay = 28;
      }
    } else {
      maxDay = MAX_MONTH_DATES[newMonth];
    }
  
    if (newDay > maxDay) {
      newDay = maxDay;
    }

    let newDate = new Date(newYear, newMonth, newDay, currentDate.getHours(), currentDate.getMinutes(), currentDate.getSeconds());
    PF(inputName).setDate(newDate);
  }	
}

