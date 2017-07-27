package uk.ac.exeter.QuinCe.data.Calculation;

import org.joda.time.DateTime;

/**
 * A object representing the subset of data required to establish
 * which records from raw_data and gas_standards_data should be flagged
 * as IGNORED to remove the flushing time from calculations.
 * 
 * @author Steve Jones
 *
 */
public class TrimFlushingRecord implements Comparable<TrimFlushingRecord> {

	/**
	 * Indicates a measurement record
	 */
	public static final int RAW_DATA = 0;
	
	/**
	 * Indicates a gas standard record
	 */
	public static final int GAS_STANDARDS_DATA = 1;
	
	/**
	 * The record type
	 */
	private int recordType;
	
	/**
	 * The row number
	 */
	private int row;
	
	/**
	 * The date/time of the record
	 */
	private DateTime dateTime;
	
	/**
	 * The run type
	 */
	private long runTypeId;
	
	/**
	 * Indicates whether or not the record should be ignored
	 */
	private boolean ignore = false;
	
	/**
	 * Constructor
	 * @param recordType The record type
	 * @param row The row number
	 * @param dateTime The date/time
	 * @param runTypeId The run type
	 */
	public TrimFlushingRecord(int recordType, int row, DateTime dateTime, long runTypeId) {
		this.recordType = recordType;
		this.row = row;
		this.dateTime = dateTime;
		this.runTypeId = runTypeId;
	}
	
	/**
	 * Determine whether this record has the specified run type
	 * @param runType The desired run type
	 * @return {@code true} if the record has the specified run type; {@code false} otherwise
	 */
	public boolean runTypeEquals(long runType) {
		return runTypeId == runType;
	}
	
	/**
	 * Determine whether or not this record is within the specified pre-flushing limit
	 * @param preFlushLimit The pre-flushing limit
	 * @return {@code true} if the record is within the pre-flushing limit; {@code false} if it is not
	 */
	public boolean checkPreFlushing(DateTime preFlushLimit) {
		boolean result = false;
		
		if (dateTime.isBefore(preFlushLimit) || dateTime.isEqual(preFlushLimit)) {
			ignore = true;
			result = true;
		}
		
		return result;
	}
		
	/**
	 * Determine whether or not this record is within the specified post-flushing limit
	 * @param postFlushLimit The post-flushing limit
	 * @return {@code true} if the record is within the post-flushing limit; {@code false} if it is not
	 */
	public boolean checkPostFlushing(DateTime postFlushLimit) {
		boolean result = false;
		
		if (dateTime.isAfter(postFlushLimit) || dateTime.isEqual(postFlushLimit)) {
			ignore = true;
			result = true;
		}
		
		return result;
	}

	/**
	 * Determine whether this record will be ignored because
	 * it is during the instrument's flushing period
	 * @return {@code true} if the record will be ignored; {@code false} otherwise
	 */
	public boolean getIgnore() {
		return ignore;
	}
	
	/**
	 * Get the record type
	 * @return The record type
	 */
	public int getRecordType() {
		return recordType;
	}
	
	/**
	 * Get the record's date/time
	 * @return The date/time
	 */
	public DateTime getDateTime() {
		return dateTime;
	}
	
	/**
	 * Get the record's run type
	 * @return The run type
	 */
	public long getRunTypeId() {
		return runTypeId;
	}
	
	/**
	 * Get the record's row number
	 * @return The row number
	 */
	public int getRow() {
		return row;
	}
	
	@Override
	public int compareTo(TrimFlushingRecord o) {
		// Just compare the row numbers
		return row - o.row;
	}
}
