package uk.ac.exeter.QuinCe.data;

import java.util.Calendar;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * A basic class holding information about a data file
 * @author Steve Jones
 *
 */
public class FileInfo {

	/**
	 * The internal code for the raw data extraction job stage
	 */
	public static final int JOB_CODE_EXTRACT = 0;
	
	/**
	 * The human-readable text for the raw data extraction job stage
	 */
	public static final String JOB_NAME_EXTRACT = "Extracting data";
	
	/**
	 * The job class for raw data extraction
	 */
	public static final String JOB_CLASS_EXTRACT = "uk.ac.exeter.QuinCe.jobs.files.ExtractRawDataJob";
	
	public static final int JOB_CODE_INITIAL_CHECK = 1;
	
	private static final String JOB_NAME_INITIAL_CHECK = "Initial data check";

	private static final String JOB_CLASS_INITIAL_CHECK = "uk.ac.exeter.QuinCe.jobs.files.AutoQCJob";
	
	/**
	 * The internal code for the flushing time trimming job stage
	 */
	public static final int JOB_CODE_TRIM_FLUSHING = 2;
	
	/**
	 * The human-readable text for the flushing time trimming job stage
	 */
	private static final String JOB_NAME_TRIM_FLUSHING = "Trim flushing time";
	
	/**
	 * The job class for flushing time trimming
	 */
	private static final String JOB_CLASS_TRIM_FLUSHING = "uk.ac.exeter.QuinCe.jobs.files.TrimFlushingJob";
	
	/**
	 * The internal job code for the data reduction job stage
	 */
	public static final int JOB_CODE_REDUCTION = 3;
	
	/**
	 * The human-readable text for the data reduction job stage
	 */
	private static final String JOB_NAME_REDUCTION = "Data reduction";
	
	/**
	 * The job class for data reduction
	 */
	private static final String JOB_CLASS_REDUCTION = "uk.ac.exeter.QuinCe.jobs.files.DataReductionJob";
	
	/**
	 * The internal code for the automatic QC stage
	 */
	public static final int JOB_CODE_AUTO_QC = 4;
	
	/**
	 * The human-readable text for the automatic QC stage
	 */
	private static final String JOB_NAME_AUTO_QC = "Automatic QC";
	
	/**
	 * The job class for automatic QC
	 */
	private static final String JOB_CLASS_AUTO_QC = "uk.ac.exeter.QuinCe.jobs.files.AutoQCJob";

	/**
	 * The internal code for the user QC stage
	 */
	public static final int JOB_CODE_USER_QC = 5;
	
	/**
	 * The human-readable text for the user QC stage
	 */
	private static final String JOB_NAME_USER_QC = "User QC";

	/**
	 * The internal code indicating that data recalculation is required
	 */
	public static final int JOB_CODE_NEEDS_RECALC = 5;
	
	/**
	 * The human-readable text indicating that data recalculation is required
	 */
	public static final String JOB_NAME_NEEDS_RECALC = "Recalculation Required";

	/**
	 * The file's database ID
	 */
	private long fileId;
	
	/**
	 * The ID of the instrument to which this file belongs
	 */
	private long instrumentId;
	
	/**
	 * The name of the instrument to which this file belongs
	 */
	private String instrument;
	
	/**
	 * The filename of the file
	 */
	private String fileName;
	
	/**
	 * The current job being performed on the file
	 */
	private int currentJob;
	
	/**
	 * The date on which the file was last touched
	 */
	private Calendar lastTouched;
	
	/**
	 * The date of the first measurement in the file
	 */
	private Calendar startDate;
	
	/**
	 * The number of records in the file
	 */
	private int recordCount;
	
	/**
	 * The delete flag for the file
	 */
	private boolean deleteFlag;
	
	/**
	 * The number of atmospheric measurement records in the file
	 */
	private int atmosphericMeasurementCount = 0;
	
	/**
	 * The number of ocean measurement records in the file
	 */
	private int oceanMeasurementCount = 0;
	
	/**
	 * The number of gas standard records in the file
	 */
	private int standardsCount = 0;
	
	/**
	 * The number of records marked as Good during automatic QC
	 */
	private int qcGoodCount = 0;
	
	/**
	 * The number of records marked as Questionable during automatic QC
	 */
	private int qcQuestionableCount = 0;
	
	/**
	 * The number of records marked as Bad during automatic QC
	 */
	private int qcBadCount = 0;
	
	private int qcFatalCount = 0;

	/**
	 * The number of records marked that have not been marked by automatic QC
	 */
	private int qcNotSetCount = 0;
	
	/**
	 * The number of records that have been assigned a Good WOCE flag
	 */
	private int woceGoodCount = 0;
	
	/**
	 * The number of records that have been assigned an Assumed Good WOCE flag
	 */
	private int woceAssumedGoodCount = 0;
	
	/**
	 * The number of records that have been assigned a Questionable WOCE flag
	 */
	private int woceQuestionableCount = 0;
	
	/**
	 * The number of records that have been assigned a Bad WOCE flag
	 */
	private int woceBadCount = 0;

	private int woceFatalCount = 0;
	
	/**
	 * The number of records that not been assigned a WOCE flag
	 */
	private int woceNotSetCount = 0;
	
	/**
	 * The number of records that must have a WOCE flag assigned
	 */
	private int woceNeededCount = 0;
	
	/**
	 * The number of records that have been flagged to be ignored by the system
	 */
	private int ignoredCount = 0;
	
	/**
	 * Creates a {@code FileInfo} object for a specific data file.
	 * 
	 * All information is required except for the flag counts: these must be
	 * populated using the appropriate {@code set} methods.
	 * 
	 * @param fileId The database ID of the data file
	 * @param instrumentId The database ID of the instrument to which the file belongs
	 * @param instrument The name of the instrument to which the file belongs
	 * @param fileName The file's original filename
	 * @param startDate The first date in the file
	 * @param recordCount The total number of records in the file
	 * @param currentJob The current job that is running (or scheduled to run) on the file
	 * @param lastTouched The time at which the user last touched the file
	 * @param atmosphericMeasurementsCount The number of atmospheric measurements in the file
	 * @param oceanMeasurementsCount The number of ocean measurements in the file
	 * @param standardsCount The number of gas standards measurements in the file
	 */
	public FileInfo(long fileId, long instrumentId, String instrument, String fileName, Calendar startDate, int recordCount,
			boolean deleteFlag, int currentJob, Calendar lastTouched, int atmosphericMeasurementsCount, int oceanMeasurementsCount, int standardsCount) {
		this.fileId = fileId;
		this.instrumentId = instrumentId;
		this.instrument = instrument;
		this.fileName = fileName;
		this.startDate = startDate;
		this.recordCount = recordCount;
		this.deleteFlag = deleteFlag;
		this.currentJob = currentJob;
		this.lastTouched = lastTouched;
		this.atmosphericMeasurementCount = atmosphericMeasurementsCount;
		this.oceanMeasurementCount = oceanMeasurementsCount;
		this.standardsCount = standardsCount;
		this.qcNotSetCount = getMeasurementCount();
		this.woceNeededCount = getMeasurementCount();
	}

	/**
	 * @return the fileID
	 */
	public long getFileId() {
		return fileId;
	}

	/**
	 * @return the instrument
	 */
	public String getInstrument() {
		return instrument;
	}
	
	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Get the date of the first measurement record in the file
	 * @return The first measurement date
	 */
	public Calendar getStartDate() {
		return startDate;
	}
	
	/**
	 * Get the date of the first measurement record formatted as a YYYY-MM-dd string
	 * @return The formatted first measurement date
	 */
	public String getStartDateString() {
		return DateTimeUtils.formatDate(startDate);
	}
	
	/**
	 * Return the number of measurement records in the file
	 * @return The number of records
	 */
	public int getRecordCount() {
		return recordCount;
	}

	/**
	 * Get the job name for the current job that is running or scheduled to run
	 * on the file.
	 * @return The job name
	 */
	public String getJobName() {
		String result;
		
		switch (currentJob) {
		case JOB_CODE_EXTRACT: {
			result = JOB_NAME_EXTRACT;
			break;
		}
		case JOB_CODE_REDUCTION: {
			result = JOB_NAME_REDUCTION;
			break;
		}
		case JOB_CODE_AUTO_QC: {
			result = JOB_NAME_AUTO_QC;
			break;
		}
		case JOB_CODE_USER_QC: {
			result = JOB_NAME_USER_QC;
			break;
		}
		default: {
			result = "Unknown";
		}
		}
		
		return result;
	}
	
	/**
	 * @return the currentJob
	 */
	public int getCurrentJob() {
		return currentJob;
	}

	/**
	 * @return the lastTouched
	 */
	public Calendar getLastTouched() {
		return lastTouched;
	}
		
	/**
	 * Returns the ID of the instrument to which this file belongs
	 * @return The instrument ID
	 */
	public long getInstrumentId() {
		return instrumentId;
	}
	
	/**
	 * Get the number of atmospheric measurements in the file
	 * @return The number of atmospheric measurements 
	 */
	public int getAtmosphericMeasurementCount() {
		return atmosphericMeasurementCount;
	}
	
	/**
	 * Get the number of ocean measurements in the file
	 * @return The number of ocean measurements 
	 */
	public int getOceanMeasurementCount() {
		return oceanMeasurementCount;
	}
	
	/**
	 * Get the number of gas standards measurements in the file
	 * @return The number of gas standards measurements
	 */
	public int getStandardsCount() {
		return standardsCount;
	}
	
	/**
	 * Set the number of atmospheric measurements in the file
	 * @param atmosphericMeasurementCount The number of atmospheric measurements
	 */
	public void setAtmosphericMeasurementCount(int atmosphericMeasurementCount) {
		this.atmosphericMeasurementCount = atmosphericMeasurementCount;
	}
	
	/**
	 * Set the number of ocean measurements in the file
	 * @param oceanMeasurementCount The number of ocean measurements
	 */
	public void setOceanMeasurementCount(int oceanMeasurementCount) {
		this.oceanMeasurementCount = oceanMeasurementCount;
	}
	
	/**
	 * Set the number of gas standards measurements in the file
	 * @param standardsCount The number of gas standards measurements
	 */
	public void setStandardsCount(int standardsCount) {
		this.standardsCount = standardsCount;
	}
	
	/**
	 * Returns a String listing the number of each type of measurement in the file.
	 * 
	 * <p>
	 *   The string will be of the format {@code [ocean]/[atmospheric]/[standards]}.
	 * </p>
	 * <p>
	 *   If the raw data is being extracted this value will not be available, so the String
	 *   {@code "Processing..."} will be returned instead.
	 * </p> 
	 * @return A list of the numbers of each type of measurement
	 */
	public String getRecordBreakdown() {
		StringBuffer result = new StringBuffer();
		
		if (currentJob == JOB_CODE_EXTRACT) {
			result.append("Processing...");
		} else {
			result.append(oceanMeasurementCount);
			result.append(" / ");
			result.append(atmosphericMeasurementCount);
			result.append(" / ");
			result.append(standardsCount);
		}
		
		return result.toString();
	}
	
	/**
	 * Get the number of actual measurements in the file.
	 * 
	 * This is the sum of the ocean and atmospheric measurements.
	 * 
	 * @return The total number of measurements in the file
	 */
	public int getMeasurementCount() {
		return atmosphericMeasurementCount + oceanMeasurementCount;
	}

	/**
	 * Get the number of records that have been marked Good during automatic QC
	 * @return The number of Good records from the automatic QC
	 */
	public int getQcGoodCount() {
		return qcGoodCount;
	}

	/**
	 * Set the number of records that have been marked Good during automatic QC
	 * @param qcGoodCount The number of Good records from the automatic QC
	 */
	public void setQcGoodCount(int qcGoodCount) {
		this.qcGoodCount = qcGoodCount;
	}

	/**
	 * Get the number of records that have been marked Questionable during automatic QC
	 * @return The number of Questionable records from the automatic QC
	 */
	public int getQcQuestionableCount() {
		return qcQuestionableCount;
	}

	/**
	 * Set the number of records that have been marked Questionable during automatic QC
	 * @param qcQuestionableCount The number of Questionable records from the automatic QC
	 */
	public void setQcQuestionableCount(int qcQuestionableCount) {
		this.qcQuestionableCount = qcQuestionableCount;
	}

	/**
	 * Get the number of records that have been marked Bad during automatic QC
	 * @return The number of Bad records from the automatic QC
	 */
	public int getQcBadCount() {
		return qcBadCount;
	}

	/**
	 * Set the number of records that have been marked Bad during automatic QC
	 * @param qcBadCount The number of Bad records from the automatic QC
	 */
	public void setQcBadCount(int qcBadCount) {
		this.qcBadCount = qcBadCount;
	}

	public int getQcFatalCount() {
		return qcFatalCount;
	}

	public void setQcFatalCount(int qcFatalCount) {
		this.qcFatalCount = qcFatalCount;
	}

	/**
	 * Get the number of records that do not have a flag from automatic QC
	 * @return The number of records that do not have a flag from automatic QC
	 */
	public int getQcNotSetCount() {
		return qcNotSetCount;
	}

	/**
	 * Set the number of records that do not have a flag from automatic QC
	 * @param qcNotSetCount The number of records that do not have a flag from automatic QC
	 */
	public void setQcNotSetCount(int qcNotSetCount) {
		this.qcNotSetCount = qcNotSetCount;
	}

	/**
	 * Get the number of records that have a Good WOCE flag
	 * @return The number of records that have a Good WOCE flag
	 */
	public int getWoceGoodCount() {
		return woceGoodCount;
	}

	/**
	 * Set the number of records that have a Good WOCE flag
	 * @param woceGoodCount The number of records that have a Good WOCE flag
	 */
	public void setWoceGoodCount(int woceGoodCount) {
		this.woceGoodCount = woceGoodCount;
	}

	/**
	 * Get the number of records that have an Assumed Good WOCE flag
	 * @return The number of records that have an Assumed Good WOCE flag
	 */
	public int getWoceAssumedGoodCount() {
		return woceAssumedGoodCount;
	}

	/**
	 * Set the number of records that have an Assumed Good WOCE flag
	 * @param woceAssumedGoodCount The number of records that have an Assumed Good WOCE flag
	 */
	public void setWoceAssumedGoodCount(int woceAssumedGoodCount) {
		this.woceAssumedGoodCount = woceAssumedGoodCount;
	}

	/**
	 * Get the number of records that have a Questionable WOCE flag
	 * @return The number of records that have a Questionable WOCE flag
	 */
	public int getWoceQuestionableCount() {
		return woceQuestionableCount;
	}

	/**
	 * Set the number of records that have a Questionable WOCE flag
	 * @param woceQuestionableCount The number of records that have a Questionable WOCE flag
	 */
	public void setWoceQuestionableCount(int woceQuestionableCount) {
		this.woceQuestionableCount = woceQuestionableCount;
	}

	/**
	 * Get the number of records that have a Bad WOCE flag
	 * @return The number of records that have a Bad WOCE flag
	 */
	public int getWoceBadCount() {
		return woceBadCount;
	}

	/**
	 * Set the number of records that have a Bad WOCE flag
	 * @param woceBadCount The number of records that have a Bad WOCE flag
	 */
	public void setWoceBadCount(int woceBadCount) {
		this.woceBadCount = woceBadCount;
	}

	public int getWoceFatalCount() {
		return woceFatalCount;
	}

	public void setWoceFatalCount(int woceFatalCount) {
		this.woceFatalCount = woceFatalCount;
	}

	/**
	 * Get the number of records that have not yet had a WOCE flag assigned
	 * @return The number of records that have not yet had a WOCE flag assigned
	 */
	public int getWoceNotSetCount() {
		return woceNotSetCount;
	}

	/**
	 * Set the number of records that have not yet had a WOCE flag assigned
	 * @param woceNotSetCount The number of records that have not yet had a WOCE flag assigned
	 */
	public void setWoceNotSetCount(int woceNotSetCount) {
		this.woceNotSetCount = woceNotSetCount;
	}

	/**
	 * Get the number of records that must be assigned a WOCE flag
	 * @return The number of records must be assigned a WOCE flag
	 */
	public int getWoceNeededCount() {
		return woceNeededCount;
	}

	/**
	 * Set the number of records that must be assigned a WOCE flag
	 * @param woceNeededCount The number of records that must be assigned a WOCE flag
	 */
	public void setWoceNeededCount(int woceNeededCount) {
		this.woceNeededCount = woceNeededCount;
	}

	/**
	 * Get the number of records that have an Ignore WOCE flag
	 * @return The number of records that have an Ignore WOCE flag
	 */
	public int getIgnoredCount() {
		return ignoredCount;
	}

	/**
	 * Set the number of records that have an Ignore WOCE flag
	 * @param ignoredCount The number of records that have an Ignore WOCE flag
	 */
	public void setIgnoredCount(int ignoredCount) {
		this.ignoredCount = ignoredCount;
	}
	
	/**
	 * Reset all automatic QC and WOCE flag counts to zero.
	 */
	public void clearAllCounts() {
		qcGoodCount = 0;
		qcQuestionableCount = 0;
		qcBadCount = 0;
		qcNotSetCount = 0;
		woceGoodCount = 0;
		woceAssumedGoodCount = 0;
		woceQuestionableCount = 0;
		woceBadCount = 0;
		woceNotSetCount = 0;
		woceNeededCount = 0;
		ignoredCount = 0;
	}
	
	public boolean isQcable() {
		return !deleteFlag && currentJob == JOB_CODE_USER_QC;
	}
	
	public boolean isExportable() {
		return !deleteFlag && currentJob == JOB_CODE_USER_QC && woceNeededCount == 0;
	}
	
	public boolean isDeleteable() {
		return true;
	}
	
	public boolean isRecalculateable() {
		return !deleteFlag && currentJob == JOB_CODE_USER_QC;
	}
	
	public static String getJobClass(int jobCode) {
		String result;
		
		switch (jobCode) {
		case JOB_CODE_EXTRACT: {
			result = JOB_CLASS_EXTRACT;
			break;
		}
		case JOB_CODE_INITIAL_CHECK: {
			result = JOB_CLASS_INITIAL_CHECK;
			break;
		}
		case JOB_CODE_TRIM_FLUSHING: {
			result = JOB_CLASS_TRIM_FLUSHING;
			break;
		}
		case JOB_CODE_REDUCTION: {
			result = JOB_CLASS_REDUCTION;
			break;
		}
		case JOB_CODE_AUTO_QC: {
			result = JOB_CLASS_AUTO_QC;
			break;
		}
		default: {
			result = null;
		}
		}
		
		return result;
	}

	public static String getJobName(int jobCode) {
		String result = null;
		
		switch (jobCode) {
		case JOB_CODE_EXTRACT: {
			result = JOB_NAME_EXTRACT;
			break;
		}
		case JOB_CODE_INITIAL_CHECK: {
			result = JOB_NAME_INITIAL_CHECK;
			break;
		}
		case JOB_CODE_TRIM_FLUSHING: {
			result = JOB_NAME_TRIM_FLUSHING;
			break;
		}
		case JOB_CODE_REDUCTION: {
			result = JOB_NAME_REDUCTION;
			break;
		}
		case JOB_CODE_AUTO_QC: {
			result = JOB_NAME_AUTO_QC;
			break;
		}
		case JOB_CODE_USER_QC: {
			result = JOB_NAME_USER_QC;
			break;
		}
		default: {
			result = "Unknown";
		}
		}
		
		return result;
	}
}
