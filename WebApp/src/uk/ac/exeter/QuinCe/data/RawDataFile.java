package uk.ac.exeter.QuinCe.data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;

/**
 * Handles data files in their raw text form.
 * @author Steve Jones
 *
 */
public class RawDataFile {

	/**
	 * The instrument to which this file belongs
	 */
	private Instrument instrument;
	
	/**
	 * The filename
	 */
	private String fileName = null;
	
	/**
	 * The raw data
	 */
	private byte[] rawData = null;
	
	/**
	 * The character set to use when extracting the file data
	 */
	private Charset charSet = null;
	
	/**
	 * The file data as a list of lists. The outer list represents rows,
	 * and the inner list the fields within each row
	 */
	private List<List<String>> contents = null;
	
	/**
	 * The date of the first CO2 line in the file
	 */
	private Calendar startDate = null;
	
	/**
	 * The number of CO2 lines in the file
	 */
	private int recordCount = 0;
	
	/**
	 * Formatter for dates
	 */
	private SimpleDateFormat dateFormatter = null;
	
	/**
	 * Formatter for times
	 */
	private SimpleDateFormat timeFormatter = null;
	
	/**
	 * Initialises a new object, assuming the character set is UTF-8.
	 * @param instrument The instrument to which the file belongs
	 * @param fileName The file's name
	 * @param contents The file data
	 */
	public RawDataFile(Instrument instrument, String fileName, byte[] data) {
		this.instrument = instrument;
		this.fileName = fileName;
		this.charSet = StandardCharsets.UTF_8;
		this.rawData = data;
	}
	
	/**
	 * Initialises a new object with a specified character set
	 * @param instrumentID The instrument to which the file belongs
	 * @param fileName The file's name
	 * @param contents The file data
	 * @param charSet The character set of the data file
	 */
	public RawDataFile(Instrument instrument, String fileName, byte[] data, Charset charSet) {
		this.instrument = instrument;
		this.fileName = fileName;
		this.rawData = data;
		this.charSet = charSet;
	}
	
	/**
	 * Shortcut method to extract the file contents without storing any messages
	 * @throws RawDataFileException If the file cannot be extracted
	 * @throws IOException If there is an error reading the data
	 */
	private void readData() throws RawDataFileException, IOException {
		readData(null);
	}
	
	/**
	 * Extract the contents of the file ready to be processed
	 * @param messages A list to contain any error messages. Can be null.
	 * @throws RawDataFileException If the file cannot be extracted
	 * @throws IOException If there is an error reading the data
	 */
	private void readData(List<String> messages) throws RawDataFileException, IOException {
		
		boolean fileOK = true;
		int firstBadLine = -1;
		String firstBadMessage = null;
		
		contents = new ArrayList<List<String>>();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawData), charSet));		
		String line;
		int lineCount = 0;
		recordCount = 0;
		while ((line = in.readLine()) != null) {
			lineCount++;

			// Skip header lines
			if (lineCount > instrument.getHeaderLines()) {
				String[] splitLine = line.split(instrument.getColumnSplitString());
				if (splitLine.length != instrument.getRawFileColumnCount()) {
					fileOK = false;
					if (firstBadLine < 0) {
						firstBadLine = lineCount;
						firstBadMessage = "Incorrect number of columns";
					}
					
					if (null != messages) {
						messages.add("Line " + lineCount + ": Incorrect number of columns");
					}
				} else {
					
					List<String> lineList = Arrays.asList(splitLine);
					contents.add((List<String>) lineList);
					
					String runType = lineList.get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
					
					
					if (instrument.isMeasurementRunType(runType)) {
						recordCount++;
						if (null == startDate) {
							try {
								startDate = getDateFromLine(contents.size() - 1);
							} catch (DateParseException e) {
								// Do nothing - the next parseable date will be used instead.
							}
						}
					}
				}
			}
		}
		in.close();
		
		if (!fileOK) {
			contents = null;
			throw new RawDataFileException(firstBadLine, firstBadMessage);
		}
	}
	
	/**
	 * Returns a list of the dates of every non-ignored line in
	 * the data file (i.e. lines that are either measurements or gas standards).
	 * @return The list of dates
	 * @throws IOException 
	 */
	public List<Calendar> getDates(List<String> messages) throws RawDataFileException, IOException {
		
		boolean datesOK = true;
		int firstBadDate = -1;
		String firstBadMessage = null;
		
		if (null == contents) {
			readData(messages);
		}
		
		List<Calendar> dates = new ArrayList<Calendar>(contents.size());

		for (int i = 0; i < contents.size(); i++) {
			try {
				dates.add(getDateFromLine(i));
			} catch (DateParseException e) {
				datesOK = false;
				if (firstBadDate < 0) {
					firstBadDate = i + instrument.getHeaderLines();
					firstBadMessage = e.getMessage();
				}
				messages.add("Line " + (i + instrument.getHeaderLines()) + ": " + e.getMessage());
			}
		}
		
		if (!datesOK) {
			throw new RawDataFileException(firstBadDate, firstBadMessage);
		}
		
		return dates;
	}
	
	/**
	 * Get the contents of the file as a CSV string - one record per line
	 * @return The contents of the file as a CSV string
	 * @throws RawDataFileException If the contents cannot be extracted
	 * @throws IOException If an error occurs while processing the file content
	 */
	public String getContentsAsString() throws RawDataFileException, IOException {
		
		if (null == contents) {
			readData();
		}
		
		StringBuffer result = new StringBuffer();
		
		for (List<String> line : contents) {
			for (int i = 0; i < line.size(); i++) {
				result.append(line.get(i));
				if (i < line.size() - 1) {
					result.append(',');
				}
			}
			result.append('\n');
		}
		
		return result.toString();
	}
	
	/**
	 * Retrieve the date and time from the specified line in the file
	 * @param lineNumber The line number (excluding header lines)
	 * @return A Calendar object representing the date and time
	 * @throws RawDataFileException If the date and/or time cannot be parsed from the line
	 */
	private Calendar getDateFromLine(int lineNumber) throws DateParseException, RawDataFileException {
		
		Calendar result = Calendar.getInstance(new SimpleTimeZone(0, "UTC"), Locale.ENGLISH);

		List<String> line = contents.get(lineNumber);

		// Need to do date and time separately.
		
		switch (instrument.getDateFormat()) {
		case Instrument.SEPARATE_FIELDS: {
			
			try {
				result.set(Calendar.YEAR, Integer.parseInt(line.get(instrument.getColumnAssignment(Instrument.COL_YEAR))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateParseException("Invalid year value " + line.get(instrument.getColumnAssignment(Instrument.COL_YEAR)));
			}
			
			try {
				result.set(Calendar.MONTH, Integer.parseInt(line.get(instrument.getColumnAssignment(Instrument.COL_MONTH))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateParseException("Invalid month value " + line.get(instrument.getColumnAssignment(Instrument.COL_MONTH)));
			}
			
			try {
				result.set(Calendar.DAY_OF_MONTH, Integer.parseInt(line.get(instrument.getColumnAssignment(Instrument.COL_DAY))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateParseException("Invalid day value " + line.get(instrument.getColumnAssignment(Instrument.COL_DAY)));
			}
			break;
		}
		default: {
			try {
				if (null == dateFormatter) {
					makeDateFormatter();
				}
				
				Calendar parsedDate = Calendar.getInstance();
				parsedDate.setTime(dateFormatter.parse(line.get(instrument.getColumnAssignment(Instrument.COL_DATE))));
				result.set(Calendar.YEAR, parsedDate.get(Calendar.YEAR));
				result.set(Calendar.MONTH, parsedDate.get(Calendar.MONTH));
				result.set(Calendar.DAY_OF_MONTH, parsedDate.get(Calendar.DAY_OF_MONTH));
				
			} catch (ParseException e) {
				throw new DateParseException("Invalid date value " + line.get(instrument.getColumnAssignment(Instrument.COL_DATE)));
			}
		}
		}
			

		// Now the time
		switch(instrument.getTimeFormat()) {
		case Instrument.SEPARATE_FIELDS: {
			try {
				result.set(Calendar.HOUR, Integer.parseInt(line.get(instrument.getColumnAssignment(Instrument.COL_HOUR))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateParseException("Invalid hour value " + line.get(instrument.getColumnAssignment(Instrument.COL_HOUR)));
			}
			
			try {
				result.set(Calendar.MINUTE, Integer.parseInt(line.get(instrument.getColumnAssignment(Instrument.COL_MINUTE))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateParseException("Invalid minute value " + line.get(instrument.getColumnAssignment(Instrument.COL_MINUTE)));
			}
			
			try {
				result.set(Calendar.SECOND, Integer.parseInt(line.get(instrument.getColumnAssignment(Instrument.COL_SECOND))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateParseException("Invalid second value " + line.get(instrument.getColumnAssignment(Instrument.COL_SECOND)));
			}
			break;
		}
		default: {
			try {
				if (null == timeFormatter) {
					makeTimeFormatter();
				}
	
				Calendar parsedTime = Calendar.getInstance();
				parsedTime.setTime(timeFormatter.parse(line.get(instrument.getColumnAssignment(Instrument.COL_TIME))));
				result.set(Calendar.HOUR, parsedTime.get(Calendar.HOUR));
				result.set(Calendar.MINUTE, parsedTime.get(Calendar.MINUTE));
				result.set(Calendar.SECOND, parsedTime.get(Calendar.SECOND));
			} catch (ParseException e) {
				throw new DateParseException("Invalid time value " + line.get(instrument.getColumnAssignment(Instrument.COL_TIME)));
			}
		}
		}
		
		return result;
	}
	
	/**
	 * Create a date formatter for parsing dates from the file.
	 * @throws RawDataFileException If the date format is not recognised
	 */
	private void makeDateFormatter() throws RawDataFileException {
		
		switch (instrument.getDateFormat()) {
		case Instrument.DATE_FORMAT_DDMMYY: {
			dateFormatter = new SimpleDateFormat("dd/MM/yy");
			break;
		}
		case Instrument.DATE_FORMAT_DDMMYYYY: {
			dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
			break;
		}
		case Instrument.DATE_FORMAT_MMDDYY: {
			dateFormatter = new SimpleDateFormat("MM/dd/yy");
			break;
		}
		case Instrument.DATE_FORMAT_MMDDYYYY: {
			dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
			break;
		}
		case Instrument.DATE_FORMAT_YYYYMMDD: {
			dateFormatter = new SimpleDateFormat("YYYYMMdd");
		}
		default: {
			throw new RawDataFileException(-1, "Unrecognised date format code '" + instrument.getDateFormat() + "'");
		}
		}
	}
	
	/**
	 * Create a date formatter for parsing times from the file
	 * @throws RawDataFileException If the time format is not recognised
	 */
	private void makeTimeFormatter() throws RawDataFileException {
	
		switch(instrument.getTimeFormat()) {
		case Instrument.TIME_FORMAT_COLON: {
			timeFormatter = new SimpleDateFormat("H:m:s");
			break;
		}
		case Instrument.TIME_FORMAT_NO_COLON: {
			timeFormatter = new SimpleDateFormat("Hms");
		}
		default: {
			throw new RawDataFileException(-1, "Unrecognised time format code '" + instrument.getTimeFormat() + "'");
		}
		}
	}
	
	/**
	 * Returns the filename for this file
	 * @return The filename
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Returns the first measurement date in the file
	 * @return The start date
	 */
	public Calendar getStartDate() {
		return startDate;
	}
	
	/**
	 * Returns the number of measurement records in the file
	 * @return The number of measurements
	 */
	public int getRecordCount() {
		return recordCount;
	}
}

/**
 * Basic exception for date parsing errors. Only used in this class.
 * @author Steve Jones
 *
 */
class DateParseException extends Exception {
	
	private static final long serialVersionUID = 7461251721860217369L;

	public DateParseException(String message) {
		super(message);
	}
}
