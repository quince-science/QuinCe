package uk.ac.exeter.QuinCe.data.Files;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.utils.DateTimeParseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Handles data files in their raw text form.
 * @author Steve Jones
 *
 */
@Deprecated
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
	
	private List<Calendar> dates = null;
	
	/**
	 * The number of CO2 lines in the file
	 */
	private int recordCount = 0;
	
	private List<List<String>> headerLines = null;
	
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
	
	public RawDataFile(Instrument instrument, String fileName, byte[] data, boolean fromStore) throws IOException, RawDataFileException {
		this.instrument = instrument;
		this.fileName = fileName;
		this.charSet = StandardCharsets.UTF_8;
		this.rawData = data;
		if (fromStore) {
			readDataFromStore();
		}
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
	 * Extract the file contents when retrieved from the file store
	 * @throws IOException
	 * @throws RawDataFileException
	 */
	private void readDataFromStore() throws IOException, RawDataFileException {
		/*
		boolean fileOK = true;
		int badLine = -1;
		String errorMessage = null;
		
		contents = new ArrayList<List<String>>();
		headerLines = new ArrayList<List<String>>(instrument.getHeaderLines());
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawData), charSet));		
		String line;
		int lineCount = 0;
		recordCount = 0;
		while ((line = in.readLine()) != null) {
			lineCount++;
		
			if (lineCount <= instrument.getHeaderLines()) {
				headerLines.add(Arrays.asList(line.split(",")));
			} else {
				List<String> lineList = new ArrayList<String>(instrument.getRawFileColumnCount());
				
				int linePos = 0;
				
				while (linePos > -1) {
					
					boolean fieldComplete = false;
					StringBuffer field = new StringBuffer();
					
					while(!fieldComplete) {
						int commaPos = line.indexOf(',', linePos);
						if (commaPos == -1) {
							field.append(line.substring(linePos));
							linePos = -1;
							fieldComplete = true;
						} else if (line.charAt(commaPos - 1) == '\\') {
							field.append(line.substring(linePos, commaPos - 1));
							field.append(',');
							linePos = commaPos + 1;
						} else {
							field.append(line.substring(linePos, commaPos));
							linePos = commaPos + 1;
							fieldComplete = true;
						}
					}
					
					lineList.add(field.toString());
				}
				if (lineList.size() != instrument.getRawFileColumnCount()) {
	
					fileOK = false;
					if (badLine < 0) {
						badLine = lineCount;
						errorMessage = "Incorrect number of columns";
					}
				} else {
					
					contents.add((List<String>) lineList);
					
					String runType = lineList.get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
					
					if (instrument.isMeasurementRunType(runType)) {
						recordCount++;
						if (null == startDate) {
							try {
								startDate = getDateFromLine(contents.size() - 1);
							} catch (DateTimeParseException|InstrumentException e) {
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
			throw new RawDataFileException(badLine, errorMessage);
		}
		
		*/
	}
	
	
	/**
	 * Extract the contents of the file ready to be processed
	 * @param messages A list to contain any error messages. Can be null.
	 * @throws RawDataFileException If the file cannot be extracted
	 * @throws IOException If there is an error reading the data
	 */
	private void readData(List<String> messages) throws RawDataFileException, IOException {
		/*
		boolean fileOK = true;
		int firstBadLine = -1;
		String firstBadMessage = null;
		
		contents = new ArrayList<List<String>>();
		headerLines = new ArrayList<List<String>>(instrument.getHeaderLines());
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawData), charSet));		
		String line;
		int lineCount = 0;
		recordCount = 0;
		while ((line = in.readLine()) != null) {
			lineCount++;

			if (lineCount <= instrument.getHeaderLines()) {
				headerLines.add(Arrays.asList(line.split(instrument.getColumnSplitString())));
			} else {
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
							} catch (DateTimeParseException|InstrumentException e) {
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
		*/
	}
	
	/**
	 * Returns a list of the dates of every non-ignored line in
	 * the data file (i.e. lines that are either measurements or gas standards).
	 * @return The list of dates
	 * @throws IOException 
	 */
	public List<Calendar> getDates(List<String> messages) throws RawDataFileException, IOException {
		
		if (null == dates) {
			createDatesList(messages);
		}
		
		return dates;
	}
	
	private void createDatesList(List<String> messages) throws RawDataFileException, IOException {
		/*
		
		boolean datesOK = true;
		int firstBadDate = -1;
		String firstBadMessage = null;
		dates = new ArrayList<Calendar>();
		
		if (null == contents) {
			readData(messages);
		}
		
		for (int i = 0; i < contents.size(); i++) {
			try {
				String runType = contents.get(i).get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
				if (!instrument.isIgnoredRunType(runType)) {
					dates.add(getDateFromLine(i));
				}
			} catch (DateTimeParseException|InstrumentException e) {
				datesOK = false;
				if (firstBadDate < 0) {
					firstBadDate = i + instrument.getHeaderLines();
					firstBadMessage = e.getMessage();
				}
				if (null != messages) {
					messages.add("Line " + (i + instrument.getHeaderLines()) + ": " + e.getMessage());
				}
			}
		}
		
		if (!datesOK) {
			throw new RawDataFileException(firstBadDate, firstBadMessage);
		}
		
		*/
	}
	
	/**
	 * Retrieve the contents of the file as a list of fields.
	 * This is a nested list. The outer list has one entry per line.
	 * Each entry is a list of Strings, with one entry per column.
	 * 
	 * @return The file contents
	 * @throws RawDataFileException If the contents cannot be processed
	 * @throws IOException If an I/O exception occurs while retrieving the data
	 */
	public List<List<String>> getContents() throws RawDataFileException, IOException {
		if (null == contents) {
			readData();
		}
		
		return contents;
	}
	
	/**
	 * Get the contents of the file as a CSV string - one record per line.
	 * Commas within fields are escaped.
	 * @return The contents of the file as a CSV string
	 * @throws RawDataFileException If the contents cannot be extracted
	 * @throws IOException If an error occurs while processing the file content
	 */
	public String getContentsAsString(boolean includeHeader) throws RawDataFileException, IOException {
		
		if (null == contents) {
			readData();
		}
		
		StringBuffer result = new StringBuffer();
		
		if (includeHeader) {
			for (List<String> headerLine : headerLines) {
				for (int i = 0; i < headerLine.size(); i++) {
					result.append(headerLine.get(i).replace(",", "\\,"));
					
					if (i < headerLine.size() - 1) {
						result.append(',');
					}
				}
				result.append('\n');
			}
		}
		
		for (List<String> line : contents) {
			for (int i = 0; i < line.size(); i++) {
				
				result.append(line.get(i).replace(",", "\\,"));
				
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
	 * @throws InstrumentException If the date/time format is not recognised
	 * @throws DateTimeParseException  If the date or time cannot be parsed from the line
	 */
	private Calendar getDateFromLine(int lineNumber) throws DateTimeParseException, InstrumentException {
		return null;
		/*
		List<String> line = contents.get(lineNumber);
		return instrument.getDateFromLine(line);
		*/
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
	
	public List<String> getLineData(int lineNumber) throws RawDataFileException, IOException {
		if (null == contents) {
			readData();
		}
		
		return contents.get(lineNumber);
	}
	
	public String getOriginalLine(int lineNumber) throws RawDataFileException, IOException {
		return null;
		/*
		List<String> fields = getLineData(lineNumber);
		
		StringBuffer result = new StringBuffer();
		
		for (int i = 0; i < fields.size(); i++) {
			result.append(fields.get(i));
			if (i < fields.size() - 1) {
				result.append(instrument.getSeparatorChar());
			}
		}
		
		return result.toString();
		*/
	}
	
	public int findLineByDate(Calendar date, int start) throws RawDataFileException, IOException {

		if (null == dates) {
			createDatesList(null);
		}
		
		int currentLine = start;
		
		while (dates.get(currentLine).before(date)) {
			currentLine++;
		}
		
		// If we've gone past the required date, the line is missing
		// so throw an exception
		if (!DateTimeUtils.datesEqual(dates.get(currentLine), date)) {
			throw new RawDataFileException(date);
		}
		
		return currentLine;
	}
	
	public List<List<String>> getHeaderLines() throws RawDataFileException, IOException {
		if (null == headerLines) {
			readData(null);
		}
		
		return headerLines;
	}
}
