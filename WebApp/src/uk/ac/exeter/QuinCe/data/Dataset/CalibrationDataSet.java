package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Represents a set of calibration data records for a data set. This is
 * a standard list with some extra functions for data reduction routines
 * @author Steve Jones
 *
 */
public class CalibrationDataSet extends TreeSet<DataSetRawDataRecord> {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -5614215614210710902L;

	/**
	 * Parameter to indicate searches must find the record before a date
	 */
	public static final int BEFORE = -1;
	
	/**
	 * Parameter to indicate searches must find the record after a date
	 */
	public static final int AFTER = 1;
	
	/**
	 * Get the calibration record that relates to the specified calibration target, and
	 * which was recorded immediately before the specified date
	 * @param recordDate The date
	 * @param calibrationTarget The calibration target
	 * @return The matching record
	 */
	public DataSetRawDataRecord getCalibrationBefore(LocalDateTime recordDate, String calibrationTarget) {
		// Make a random access version of the record set
		List<DataSetRawDataRecord> recordsList = getRecordsWithRunType(calibrationTarget);
		return searchForClosest(recordsList, recordDate, BEFORE);
	}
	
	/**
	 * Get the calibration record that relates to the specified calibration target, and
	 * which was recorded immediately before the specified date
	 * @param recordDate The date
	 * @param calibrationTarget The calibration target
	 * @return The matching record
	 */
	public DataSetRawDataRecord getCalibrationAfter(LocalDateTime recordDate, String calibrationTarget) {
		// Make a random access version of the record set
		List<DataSetRawDataRecord> recordsList = getRecordsWithRunType(calibrationTarget);
		return searchForClosest(recordsList, recordDate, AFTER);
	}
	
	/**
	 * Get a list of the records that have the specified run type
	 * @param runType The run type
	 * @return The matching records
	 */
	private List<DataSetRawDataRecord> getRecordsWithRunType(String runType) {
		
		List<DataSetRawDataRecord> result = new ArrayList<DataSetRawDataRecord>();
		
		for (DataSetRawDataRecord record : this) {
			if (record.getRunType().equalsIgnoreCase(runType)) {
				result.add(record);
			}
		}
		
		return result;
	}
	
	/**
	 * Find a record immediately preceding or following a given date in a list of records
	 * @param records The records to be searched
	 * @param date The date
	 * @param direction The search direction. {@link #BEFORE} indicates that the record must precede the date;
	 * 				    {@link #AFTER} that the record must follow the date
	 * @return The matched record, or {@code null} if no matching record is found
	 */
	private DataSetRawDataRecord searchForClosest(List<DataSetRawDataRecord> records, LocalDateTime date, int direction) {
		
		DataSetRawDataRecord result = null;

		// TODO This should be done as a binary search. See issue #590
		int position;
		
		if (direction == BEFORE) {
			position = records.size();
		} else {
			position = -1;
		}
		
		boolean searchEnded = false;
		while (!searchEnded) {
			
			position += direction;
			if (direction == BEFORE) {
				if (position < 0) {
					searchEnded = true;
				} else {
					DataSetRawDataRecord record = records.get(position);
					if (record.compareTo(date) < 0) {
						result = record;
						searchEnded = true;
					}
					
				}
			} else {
				if (position >= records.size()) {
					searchEnded = true;
				} else {
					DataSetRawDataRecord record = records.get(position);
					if (record.compareTo(date) > 0) {
						result = record;
						searchEnded = true;
					}
				}
			}
		}
		
		return result;
	}
}
