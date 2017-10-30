package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileLine;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Class to load the raw data for a data set
 * @author Steve Jones
 *
 */
public abstract class DataSetRawData {

	/**
	 * Averaging mode for no averaging
	 */
	public static final int AVG_MODE_NONE = 0;
	
	/**
	 * Human readable string for the no-averaging mode
	 */
	public static final String AVG_MODE_NONE_NAME = "None";
	
	/**
	 * Averaging mode for averaging every minute
	 */
	public static final int AVG_MODE_MINUTE = 1;
	
	/**
	 * Human-readable string for the every-minute averaging mode
	 */
	public static final String AVG_MODE_MINUTE_NAME = "Every minute";
	
	/**
	 * The data set to which this data belongs
	 */
	private DataSet dataSet;
	
	/**
	 * The file definitions for the data set
	 */
	private List<FileDefinition> fileDefinitions;
	
	/**
	 * The data extracted from the data files that are encompassed by the data set
	 */
	protected List<List<DataFileLine>> data;
	
	/**
	 * The rows from each file that are currently being processed.
	 * These rows are discovered using #nextRecord
	 */
	protected List<List<Integer>> selectedRows = null;
	
	/**
	 * Current position pointers for each file definition
	 */
	protected List<Integer> rowPositions = null;
	
	/**
	 * Constructor - loads and extracts the data for the data set
	 * @param dataSource A data source
	 * @param dataSet The data set
	 * @param instrument The instrument to which the data set belongs
	 * @throws RecordNotFoundException If no data files are found within the data set
	 * @throws DatabaseException If a database error occurs 
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DataFileException If the data cannot be extracted from the files
	 */
	protected DataSetRawData(DataSource dataSource, DataSet dataSet, Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException, DataFileException {
		
		this.dataSet = dataSet;
		fileDefinitions = new ArrayList<FileDefinition>();
		data = new ArrayList<List<DataFileLine>>();
		selectedRows = new ArrayList<List<Integer>>();
		rowPositions = new ArrayList<Integer>();
		
		
		for (FileDefinition fileDefinition : instrument.getFileDefinitions()) {
			fileDefinitions.add(fileDefinition);
			
			List<DataFile> dataFiles = DataFileDB.getFiles(dataSource, fileDefinition, dataSet.getStart(), dataSet.getEnd());
			data.add(extractData(dataFiles));
			
			selectedRows.add(null);
			rowPositions.add(-1);
		}
	}
	
	/**
	 * Extract the rows from the data files that are encompassed by a data set
	 * @param dataFiles The data files for the definition
	 * @throws DataFileException If the data cannot be extracted from the file
	 */
	private List<DataFileLine> extractData(List<DataFile> dataFiles) throws DataFileException {
		
		// The data from the files
		ArrayList<DataFileLine> fileData = new ArrayList<DataFileLine>();
		
		// Loop through each file
		for (DataFile file : dataFiles) {			
			// Size the data array
			fileData.ensureCapacity(fileData.size() + file.getRecordCount());
			
			// Skip any lines before the data set start date
			int currentLine = file.getFirstDataLine();
			while (file.getDate(currentLine).isBefore(dataSet.getStart())) {
				currentLine++;
			}
			
			// Copy lines until either the end of the file or we pass the data set's end date
			while (currentLine < file.getLineCount()) {
				if (file.getDate(currentLine).isAfter(dataSet.getEnd())) {
					break;
				} else {
					fileData.add(new DataFileLine(file, currentLine));
					currentLine++;
				}
			}
		}
		
		return fileData;
	}
	
	/**
	 * Get the available averaging modes as a map
	 * @return The averaging modes
	 */
	public static Map<String, Integer> averagingModes() {
		// TODO Move this to a static block
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		
		map.put(AVG_MODE_NONE_NAME, AVG_MODE_NONE);
		map.put(AVG_MODE_MINUTE_NAME, AVG_MODE_MINUTE);
		
		return map;
	}
	
	/**
	 * Navigate to the next record in the data set.
	 * 
	 * <p>
	 *   This will collect all rows that are relevant under the averaging scheme
	 *   for the data set
	 * </p>
	 * 
	 * @return {@code true} if a new record is discovered; {@code false} if there are no more records in the data set
	 * @throws DataSetException If an error occurs during record selection
	 */
	public boolean nextRecord() throws DataSetException {
		
		boolean found = false;
		clearSelectedRows();
		
		int currentFile = 0;
		while (!allRowsMatch()) {

			if (!selectNextRow(currentFile)) {
				// We've gone off the end of this file, so we can't select a record
				// across all files
				break;
			} else {
				// If the rows aren't equal, then reset and continue from here
				if (!selectedRowsMatch(currentFile)) {
					resetOtherFiles(currentFile);
				}
				
				// Select the next file
				currentFile++;
				if (currentFile == fileDefinitions.size()) {
					currentFile = 0;
				}
			}
		}
		
		if (allRowsMatch()) {
			found = true;

			try {
				StringBuilder message = new StringBuilder();
				for (int i = 0; i < fileDefinitions.size(); i++) {
					message.append(selectedRows.get(i).get(0));
					message.append(' ');
					message.append(data.get(i).get(selectedRows.get(i).get(0)).getDate());
					message.append(';');
				}
				System.out.println(message.toString());
			} catch (Exception e) {
				throw new DataSetException(e);
			}
		}
		
		return found;
	}
	
	/**
	 * Determine whether or not matching lines have been found
	 * for all files in the data set
	 * @return {@code true} if all files have matching lines; {@code false} otherwise
	 */
	private boolean allRowsMatch() throws DataSetException {
		boolean match = true;
		
		for (int i = 0; match && i < selectedRows.size(); i++) {
			if (null == selectedRows.get(i)) {
				match = false;
			}
		}

		if (match) {
			match = selectedRowsMatch(0);
		}
		
		return match;
	}
	
	/**
	 * Clear the selected rows from all files except the specified one
	 * @param file The source file that should not be cleared
	 */
	private void resetOtherFiles(int file) {
		for (int i = 0; i < selectedRows.size(); i++) {
			if (i != file) {
				selectedRows.set(i, null);
			}
		}
	}
	
	/**
	 * Determine whether all the files that have selected rows
	 * match the selected rows in the specified file. Files that
	 * do not have rows assigned are not checked.
	 * 
	 * @param file The file to compare
	 * @return {@code true} if the all the assigned files match; {@code false} otherwise
	 * @throws DataSetException If an error occurs during the checks
	 */
	private boolean selectedRowsMatch(int file) throws DataSetException {
		boolean match = true;
		
		for (int i = 0; match && i < selectedRows.size(); i++) {
			if (i != file) {
				match = rowSelectionsMatch(file, i);
			}
		}
		
		return match;
	}
	
	/**
	 * Compare the selected rows for two files to see if they match
	 * @param file1 The first file
	 * @param file2 The second file
	 * @return {@code true} if the selected rows match; {@code false} if they do not
	 * @throws DataSetException If an error occurs during the examination
	 */
	protected abstract boolean rowSelectionsMatch(int file1, int file2) throws DataSetException;
	
	/**
	 * Select the next row(s) in the specified file
	 * @param fileIndex The file index
	 * @return {@code true} if the next row is found; {@code false} if the end of the file has been reached
	 * @throws DataSetException If an error occurs during row selection
	 */
	protected abstract boolean selectNextRow(int fileIndex) throws DataSetException;

	/**
	 * Get a selected line from a file other than the specified file.
	 * 
	 * The file from which the line is taken is not defined. If no other
	 * files have selected lines, the method returns {@code null}.
	 * @return A selected line from another file
	 */
	protected DataFileLine getOtherSelectedLine(int fileIndex) {
		
		DataFileLine line = null;
		
		for (int i = 0; i < selectedRows.size(); i++) {
			if (i != fileIndex) {
				List<Integer> rows = selectedRows.get(i);
				if (null != rows) {
					line = data.get(i).get(rows.get(0));
					break;
				}
			}
		}
		
		return line;
	}
	
	/**
	 * Reset the line processing back to the start of the files
	 */
	public void reset() {
		selectedRows = new ArrayList<List<Integer>>(fileDefinitions.size());
		rowPositions = new ArrayList<Integer>(fileDefinitions.size());

		for (int i = 0; i < fileDefinitions.size(); i++) {
			selectedRows.add(null);
			rowPositions.add(-1);
		}
	}
	
	/**
	 * Clear the selected rows data
	 */
	private void clearSelectedRows() {
		selectedRows = new ArrayList<List<Integer>>(fileDefinitions.size());
		for (int i = 0; i < fileDefinitions.size(); i++) {
			selectedRows.add(null);
		}
	}
}
