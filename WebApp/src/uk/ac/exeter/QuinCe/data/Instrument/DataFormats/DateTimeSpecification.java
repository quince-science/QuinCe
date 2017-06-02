package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;

/**
 * Defines how the date and time are stored in a data file
 * @author Steve Jones
 *
 */
public class DateTimeSpecification {

		/**
		 * Date and time in a single field
		 */
		public static final int FORMAT_SINGLE_FIELD = 0;
		
		/**
		 * Date in a single field, time in multiple fields
		 */
		public static final int FORMAT_SINGLE_DATE_MULTIPLE_TIME = 1;
		
		/**
		 * Date in multiple fields, time in single field
		 */
		public static final int FORMAT_MULTIPLE_DATE_SINGLE_TIME = 2;
		
		/**
		 * Date and time in multiple fields
		 */
		public static final int FORMAT_MULTIPLE_DATE_MULTIPLE_TIME = 3;

		/**
		 * Decimal Julian day
		 */
		public static final int FORMAT_JULIAN_DAY = 4;
		
		/**
		 * The format used in this specification
		 */
		private int format = FORMAT_SINGLE_FIELD;
		
		/**
		 * The format string for a single date/time field
		 */
		private String dateTimeFormat = "";
		
		/**
		 * The format string for a single date field
		 */
		private String dateFormat = "";
		
		/**
		 * The format string for a single time field
		 */
		private String timeFormat = "";
		
		/**
		 * The index for the single date/time column
		 */
		private int dateTimeColumn = -1;
		
		/**
		 * The index for the single date column
		 */
		private int dateColumn = -1;
		
		/**
		 * The index for the single time column
		 */
		private int timeColumn = -1;
		
		/**
		 * The index of the Julian day column
		 */
		private int julianDayColumn = -1;
		
		/**
		 * Indicates whether the data file contains
		 * the year to go with the Julian day. If not,
		 * the year must be specified for each data file.
		 * @see FileDefinition#getFileYear()
		 */
		private boolean hasYearColumn = false;
		
		/**
		 * The index of the year column
		 */
		private int yearColumn = -1;
		
		/**
		 * The index of the month column
		 */
		private int monthColumn = -1;
		
		/**
		 * The index of the day column
		 */
		private int dayColumn = -1;
		
		/**
		 * The index of the hour column
		 */
		private int hourColumn = -1;
		
		/**
		 * The index of the minute column
		 */
		private int minuteColumn = -1;
		
		/**
		 * The index of the second column
		 */
		private int secondColumn = -1;
		
		/**
		 * Get the JSON representation of this specification.
		 * 
		 * <p>
		 *   The JSON string is as follows:
		 * </p>
		 * <pre>
		 *   {
		 *     "format": <format>,
		 *     "dateTimeFormat": "<date/time format string>",
		 *     "dateFormat": "<date format string>",
		 *     "timeFormat": "<time format string>",
		 *     "dateTimeColumn": <dateTime column index>,
		 *     "dateColumn": <date column index>,
		 *     "timeColumn": <time column index>,
		 *     "julianDayColumn": <Julian day column index>,
		 *     "yearColumn": <year column>,
		 *     "monthColumn": <month column>,
		 *     "dayColumn": <day column>,
		 *     "hourColumn": <hour column>,
		 *     "minuteColumn": <minute column>,
		 *     "secondColumn": <second column>
		 *   }
		 * </pre>
		 * <p>
		 *   The format will be the integer value corresponding
		 *   to the chosen format. The JSON processor will need
		 *   to know how to translate these.
		 * </p>
		 * 
		 * @return The JSON string
		 */
		public String getJsonString() {
			StringBuilder json = new StringBuilder();
			
			json.append('{');
			
			json.append("\"format\":");
			json.append(format);
			json.append(",\"dateTimeFormat\":\"");
			json.append(dateTimeFormat);
			json.append("\",\"dateFormat\":\"");
			json.append(dateFormat);
			json.append("\",\"timeFormat\":\"");
			json.append(timeFormat);
			json.append("\",\"dateTimeColumn\":");
			json.append(dateTimeColumn);
			json.append(",\"dateColumn\":");
			json.append(dateColumn);
			json.append(",\"timeColumn\":");
			json.append(timeColumn);
			json.append(",\"julianDayColumn\":");
			json.append(julianDayColumn);
			json.append(",\"hasYearColumn\":");
			json.append(hasYearColumn);
			json.append(",\"yearColumn\":");
			json.append(yearColumn);
			json.append(",\"monthColumn\":");
			json.append(monthColumn);
			json.append(",\"dayColumn\":");
			json.append(dayColumn);
			json.append(",\"hourColumn\":");
			json.append(hourColumn);
			json.append(",\"minuteColumn\":");
			json.append(minuteColumn);
			json.append(",\"secondColumn\":");
			json.append(secondColumn);
			json.append('}');
			
			return json.toString();
		}
}
