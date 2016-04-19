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
	
	public static final String JOB_NAME_EXTRACT = "Extracting data";
	
	public static final String JOB_CLASS_EXTRACT = "uk.ac.exeter.QuinCe.jobs.files.ExtractRawDataJob";
	
	public static final int JOB_CODE_REDUCTION = 1;
	
	public static final String JOB_NAME_REDUCTION = "Data reduction";
	
	public static final String JOB_CLASS_REDUCTION = "uk.ac.exeter.QuinCe.jobs.files.DataReductionJob";
	
	public static final int JOB_CODE_AUTO_QC = 2;
	
	public static final String JOB_NAME_AUTO_QC = "Automatic QC";
	
	public static final String JOB_CLASS_AUTO_QC = "uk.ac.exeter.QuinCe.jobs.files.AutoQCJob";

	public static final int JOB_CODE_USER_QC = 3;
	
	public static final String JOB_NAME_USER_QC = "User QC";

	public static final int JOB_CODE_NEEDS_RECALC = 4;
	
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
	 * The date of the first measurment in the file
	 */
	private Calendar startDate;
	
	/**
	 * The number of records in the file
	 */
	private int recordCount;
	
	/**
	 * The number of atmospheric measurement records in the file
	 */
	private int atmosphericMeasurementCount = -1;
	
	/**
	 * The number of ocean measurement records in the file
	 */
	private int oceanMeasurementCount = -1;
	
	/**
	 * The number of gas standard records in the file
	 */
	private int standardsCount = -1;
	
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
			int currentJob, Calendar lastTouched, int atmosphericMeasurementsCount, int oceanMeasurementsCount, int standardsCount) {
		this.fileId = fileId;
		this.instrumentId = instrumentId;
		this.instrument = instrument;
		this.fileName = fileName;
		this.startDate = startDate;
		this.recordCount = recordCount;
		this.currentJob = currentJob;
		this.lastTouched = lastTouched;
		this.atmosphericMeasurementCount = atmosphericMeasurementsCount;
		this.oceanMeasurementCount = oceanMeasurementsCount;
		this.standardsCount = standardsCount;
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
	 * Return the days remaining before the data file will be purged,
	 * @return The time remaining.
	 */
	public int getTimeRemaining() {
		Calendar now = Calendar.getInstance();
		now.setTimeInMillis(System.currentTimeMillis());
		int daysRemaining = DateTimeUtils.getDaysBetween(lastTouched, now);
		return 60 - daysRemaining;
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
}
