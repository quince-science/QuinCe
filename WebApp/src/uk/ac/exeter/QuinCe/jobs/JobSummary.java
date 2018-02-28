package uk.ac.exeter.QuinCe.jobs;

import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils;

import uk.ac.exeter.QuinCe.User.User;

/**
 * Basic class containing the read-omly summary of a job. This cannot
 * be used to manipulate jobs - the {@link JobManager} must
 * be used for that.
 *
 * @author Steve Jones
 *
 */
public class JobSummary {

	/**
	 * The job's database ID
	 */
	private long id;

	/**
	 * The job's owner
	 */
	private User owner;

	/**
	 * The job's class name
	 */
	private String className;

	/**
	 * The date/time that the job was submitted
	 */
	private Date submitted;

	/**
	 * The current status of the job
	 */
	private String status;

	/**
	 * The date/time that the job was started
	 */
	@Deprecated
	private Date started;

	/**
	 * The date/time that the job finished
	 */
	@Deprecated
	private Date ended;

	/**
	 * The current progress of the job
	 */
	private double progress;

	/**
	 * The stack trace for the job (if an error occurred while it was running)
	 */
	private String stackTrace;

	/**
	 * Basic constructor - simply takes in all values for the summary
	 * @param id The job's database ID
	 * @param owner The job's owner
	 * @param className The job's class name
	 * @param submitted The date/time that the job was submitted
	 * @param status The current status of the job
	 * @param started The date/time that the job was started
	 * @param ended The date/time that the job finished
	 * @param progress The current progress of the job
	 * @param stackTrace The stack trace for the job
	 */
	@Deprecated
	public JobSummary(long id, User owner, String className, Date submitted, String status, Date started, Date ended, double progress, String stackTrace) {
		this.id = id;
		this.owner = owner;
		this.className = className;
		this.submitted = submitted;
		this.status = status;
		this.started = started;
		this.ended = ended;
		this.progress = progress;
		this.stackTrace = stackTrace;
	}

	/**
	 * Get the job's database ID
	 * @return The job's database ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * Get the job's owner
	 * @return The job's owner
	 */
	public User getOwner() {
		return owner;
	}

	/**
	 * Get the job's class name. The package is removed.
	 * @return The job's class name
	 */
	public String getClassName() {
		String[] classSplit = className.split("\\.");

		return classSplit[classSplit.length - 1];
	}

	/**
	 * Get the date/time that the job was submitted
	 * @return The date/time that the job was submitted
	 */
	public Date getSubmitted() {
		return submitted;
	}

	/**
	 * Get the current status of the job
	 * @return The current status of the job
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Get the date/time that the job was started
	 * @return The date/time that the job was started
	 */
	@Deprecated
	public Date getStarted() {
		return started;
	}

	/**
	 * Get the date/time that the job finished
	 * @return The date/time that the job finished
	 */
	@Deprecated
	public Date getEnded() {
		return ended;
	}

	/**
	 * Get the current progress of the job
	 * @return The current progress of the job
	 */
	public double getProgress() {
		return progress;
	}

	/**
	 * Get the stack trace for the job
	 * @return The stack trace for the job
	 */
	public String getStackTrace() {
		return stackTrace;
	}

	/**
	 * Get the stack trace for the job, formatted in HTML
	 * @return The stack trace for the job in HTML
	 */
	public String getStackTraceAsHtml() {
		String result = StringEscapeUtils.escapeHtml4(stackTrace);

		if (null != stackTrace) {
			result = result.replace("\n", "<br/>");
		}

		return result;
	}
}
