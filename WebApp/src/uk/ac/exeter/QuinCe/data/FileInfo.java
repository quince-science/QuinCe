package uk.ac.exeter.QuinCe.data;

import java.util.Calendar;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * A basic class holding information about a data file
 * @author Steve Jones
 *
 */
public class FileInfo {

	public static final int JOB_CODE_EXTRACT = 0;
	
	private static final String JOB_NAME_EXTRACT = "Extracting data";
	
	private static final String JOB_CLASS_EXTRACT = "uk.ac.exeter.QuinCe.jobs.files.ExtractRawDataJob";
	
	public static final int JOB_CODE_INITIAL_CHECK = 1;
	
	private static final String JOB_NAME_INITIAL_CHECK = "Initial data check";

	private static final String JOB_CLASS_INITIAL_CHECK = "uk.ac.exeter.QuinCe.jobs.files.AutoQCJob";
	
	public static final int JOB_CODE_TRIM_FLUSHING = 2;
	
	private static final String JOB_NAME_TRIM_FLUSHING = "Trim flushing time";
	
	private static final String JOB_CLASS_TRIM_FLUSHING = "uk.ac.exeter.QuinCe.jobs.files.TrimFlushingJob";
	
	public static final int JOB_CODE_REDUCTION = 3;
	
	private static final String JOB_NAME_REDUCTION = "Data reduction";
	
	private static final String JOB_CLASS_REDUCTION = "uk.ac.exeter.QuinCe.jobs.files.DataReductionJob";
	
	public static final int JOB_CODE_AUTO_QC = 4;
	
	private static final String JOB_NAME_AUTO_QC = "Automatic QC";
	
	private static final String JOB_CLASS_AUTO_QC = "uk.ac.exeter.QuinCe.jobs.files.AutoQCJob";

	public static final int JOB_CODE_USER_QC = 5;
	
	private static final String JOB_NAME_USER_QC = "User QC";

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
	 * The date of the first measurment in the file
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
	
	private int qcGoodCount = 0;
		
	private int qcQuestionableCount = 0;
	
	private int qcBadCount = 0;
	
	private int qcFatalCount = 0;
	
	private int qcNotSetCount = 0;
	
	private int woceGoodCount = 0;
	
	private int woceAssumedGoodCount = 0;
	
	private int woceQuestionableCount = 0;
	
	private int woceBadCount = 0;

	private int woceFatalCount = 0;
	
	private int woceNotSetCount = 0;
	
	private int woceNeededCount = 0;
	
	private int ignoredCount = 0;
	
	/**
	 * Constructor for all values
	 * @param fileId The file ID
	 * @param instrumentId The ID of the instrument to which this file belongs
	 * @param instrument The instrument name
	 * @param fileName The filename
	 * @param startDate The date of the first measurement in the file
	 * @param recordCount The number of measurement records in the file
	 * @param currentJob The current job
	 * @param jobStatus The current job status
	 * @param lastTouched The date that the file was last touched
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
	 * Get the job name for a given job code
	 * @param jobCode The job name
	 * @return The job code
	 */
	public String getJobName() {
		return getJobName(currentJob);
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
	
	public int getAtmosphericMeasurementCount() {
		return atmosphericMeasurementCount;
	}
	
	public int getOceanMeasurementCount() {
		return oceanMeasurementCount;
	}
	
	public int getStandardsCount() {
		return standardsCount;
	}
	
	public void setAtmosphericMeasurementCount(int atmosphericMeasurementCount) {
		this.atmosphericMeasurementCount = atmosphericMeasurementCount;
	}
	
	public void setOceanMeasurementCount(int oceanMeasurementCount) {
		this.oceanMeasurementCount = oceanMeasurementCount;
	}
	
	public void setStandardsCount(int standardsCount) {
		this.standardsCount = standardsCount;
	}
	
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
	
	public int getMeasurementCount() {
		return atmosphericMeasurementCount + oceanMeasurementCount;
	}

	public int getQcGoodCount() {
		return qcGoodCount;
	}

	public void setQcGoodCount(int qcGoodCount) {
		this.qcGoodCount = qcGoodCount;
	}

	public int getQcQuestionableCount() {
		return qcQuestionableCount;
	}

	public void setQcQuestionableCount(int qcQuestionableCount) {
		this.qcQuestionableCount = qcQuestionableCount;
	}

	public int getQcBadCount() {
		return qcBadCount;
	}

	public void setQcBadCount(int qcBadCount) {
		this.qcBadCount = qcBadCount;
	}

	public int getQcFatalCount() {
		return qcFatalCount;
	}

	public void setQcFatalCount(int qcFatalCount) {
		this.qcFatalCount = qcFatalCount;
	}

	public int getQcNotSetCount() {
		return qcNotSetCount;
	}

	public void setQcNotSetCount(int qcNotSetCount) {
		this.qcNotSetCount = qcNotSetCount;
	}

	public int getWoceGoodCount() {
		return woceGoodCount;
	}

	public void setWoceGoodCount(int woceGoodCount) {
		this.woceGoodCount = woceGoodCount;
	}

	public int getWoceAssumedGoodCount() {
		return woceAssumedGoodCount;
	}

	public void setWoceAssumedGoodCount(int woceAssumedGoodCount) {
		this.woceAssumedGoodCount = woceAssumedGoodCount;
	}

	public int getWoceQuestionableCount() {
		return woceQuestionableCount;
	}

	public void setWoceQuestionableCount(int woceQuestionableCount) {
		this.woceQuestionableCount = woceQuestionableCount;
	}

	public int getWoceBadCount() {
		return woceBadCount;
	}

	public void setWoceBadCount(int woceBadCount) {
		this.woceBadCount = woceBadCount;
	}

	public int getWoceFatalCount() {
		return woceFatalCount;
	}

	public void setWoceFatalCount(int woceFatalCount) {
		this.woceFatalCount = woceFatalCount;
	}

	public int getWoceNotSetCount() {
		return woceNotSetCount;
	}

	public void setWoceNotSetCount(int woceNotSetCount) {
		this.woceNotSetCount = woceNotSetCount;
	}

	public int getWoceNeededCount() {
		return woceNeededCount;
	}

	public void setWoceNeededCount(int woceNeededCount) {
		this.woceNeededCount = woceNeededCount;
	}

	public int getIgnoredCount() {
		return ignoredCount;
	}

	public void setIgnoredCount(int ignoredCount) {
		this.ignoredCount = ignoredCount;
	}
	
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
