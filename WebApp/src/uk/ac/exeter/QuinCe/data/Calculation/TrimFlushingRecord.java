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

	public static final int RAW_DATA = 0;
	
	public static final int GAS_STANDARDS_DATA = 1;
	
	private int recordType;
	
	private int row;
	
	@Deprecated
	private DateTime dateTime;
	
	private long runTypeId;
	
	private boolean ignore = false;
	
	@Deprecated
	public TrimFlushingRecord(int recordType, int row, DateTime dateTime, long runTypeId) {
		this.recordType = recordType;
		this.row = row;
		this.dateTime = dateTime;
		this.runTypeId = runTypeId;
	}
	
	public boolean runTypeEquals(long runType) {
		return runTypeId == runType;
	}
	
	@Deprecated
	public boolean checkPreFlushing(DateTime preFlushLimit) {
		boolean result = false;
		
		if (dateTime.isBefore(preFlushLimit) || dateTime.isEqual(preFlushLimit)) {
			ignore = true;
			result = true;
		}
		
		return result;
	}
		
	@Deprecated
	public boolean checkPostFlushing(DateTime postFlushLimit) {
		boolean result = false;
		
		if (dateTime.isAfter(postFlushLimit) || dateTime.isEqual(postFlushLimit)) {
			ignore = true;
			result = true;
		}
		
		return result;
	}

	public boolean getIgnore() {
		return ignore;
	}
	
	public int getRecordType() {
		return recordType;
	}
	
	@Deprecated
	public DateTime getDateTime() {
		return dateTime;
	}
	
	public long getRunTypeId() {
		return runTypeId;
	}
	
	public int getRow() {
		return row;
	}
	
	@Override
	public int compareTo(TrimFlushingRecord o) {
		// Just compare the row numbers
		return row - o.row;
	}
	
	
	
}
