package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileLine;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Class to load the raw data for a data set
 * @author Steve Jones
 *
 */
public class DataSetRawData {

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
	 * The data extracted from the data files that are encompassed by the data set
	 */
	private Map<FileDefinition, List<DataFileLine>> data;
	
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
	public DataSetRawData(DataSource dataSource, DataSet dataSet, Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException, DataFileException {
		
		this.dataSet = dataSet;
		data = new TreeMap<FileDefinition, List<DataFileLine>>();
		
		for (FileDefinition fileDefinition : instrument.getFileDefinitions()) {
			List<DataFile> dataFiles = DataFileDB.getFiles(dataSource, fileDefinition, dataSet.getStart(), dataSet.getEnd());
			extractDefinitionData(fileDefinition, dataFiles);
		}
	}
	
	/**
	 * Extract the rows from the data files that are encompassed by the data set
	 * @param fileDefinition The file definition
	 * @param dataFiles The data files for the definition
	 * @throws DataFileException If the data cannot be extracted from the file
	 */
	private void extractDefinitionData(FileDefinition fileDefinition, List<DataFile> dataFiles) throws DataFileException {
		
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
		
		// Store the extracted data
		data.put(fileDefinition, fileData);
	}
	
	/**
	 * Get the available averaging modes as a map
	 * @return The averaging modes
	 */
	public static Map<String, Integer> averagingModes() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		
		map.put(AVG_MODE_NONE_NAME, AVG_MODE_NONE);
		map.put(AVG_MODE_MINUTE_NAME, AVG_MODE_MINUTE);
		
		return map;
	}
}
