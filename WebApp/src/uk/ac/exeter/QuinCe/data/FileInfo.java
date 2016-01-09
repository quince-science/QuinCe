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
	
	public static final int JOB_CODE_LOADING = 1;
	
	public static final String JOB_NAME_LOADING = "Loading additional data";
	
	public static final int JOB_CODE_REDUCTION = 2;
	
	public static final String JOB_NAME_REDUCTION = "Data reduction";
	
	public static final int JOB_CODE_AUTO_QC = 3;
	
	public static final String JOB_NAME_AUTO_QC = "Automatic QC";
	
	public static final int JOB_CODE_USER_QC = 4;
	
	public static final String JOB_NAME_USER_QC = "User QC";

	public static final int STATUS_CODE_WAITING = -1;
	
	public static final String STATUS_NAME_WAITING = "Waiting";
	
	public static final int STATUS_CODE_ERROR = -2;
	
	public static final String STATUS_NAME_ERROR = "Error";

/**
	 * The file's database ID
	 */
	private long fileID;
	
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
	 * The status of the current job
	 */
	private int jobStatus;
	
	/**
	 * The date on which the file was last touched
	 */
	private Calendar lastTouched;

	/**
	 * Constructor for all values
	 * @param fileID The file ID
	 * @param instrument The instrument name
	 * @param fileName The filename
	 * @param currentJob The current job
	 * @param jobStatus The current job status
	 * @param lastTouched The date that the file was last touched
	 */
	public FileInfo(long fileID, String instrument, String fileName, int currentJob, int jobStatus, Calendar lastTouched) {
		this.fileID = fileID;
		this.instrument = instrument;
		this.fileName = fileName;
		this.currentJob = currentJob;
		this.jobStatus = jobStatus;
		this.lastTouched = lastTouched;
	}

	/**
	 * @return the fileID
	 */
	public long getFileID() {
		return fileID;
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
		case JOB_CODE_LOADING: {
			result = JOB_NAME_LOADING;
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
	 * Determines whether the current job is running or not
	 * @return {@code true} if the job is running; {@false} if it is not.
	 */
	public boolean getJobRunning() {
		return jobStatus >= 0;
	}
	
	/**
	 * Returns the status of the current job as a String.
	 * For the Waiting or Error statuses, those strings are returned.
	 * Otherwise the value of the field followed by '%' is returned.
	 * @return The job status
	 */
	public String getJobStatusString() {
		String result;
		
		switch(jobStatus) {
		case STATUS_CODE_WAITING: {
			result = STATUS_NAME_WAITING;
			break;
		}
		case STATUS_CODE_ERROR: {
			result = STATUS_NAME_ERROR;
			break;
		}
		default: {
			result = jobStatus + "%";
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
	 * @return the jobStatus
	 */
	public int getJobStatus() {
		return jobStatus;
	}

	/**
	 * @return the lastTouched
	 */
	public Calendar getLastTouched() {
		return lastTouched;
	}
	
	
}
