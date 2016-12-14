package uk.ac.exeter.QuinCe.jobs;

import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils;

import uk.ac.exeter.QuinCe.data.User;

/**
 * Basic class containing the summary of a job.
 * @author Steve Jones
 *
 */
public class JobSummary {

	private long id;
	
	private User owner;
	
	private String className;
	
	private Date submitted;
	
	private String status;
	
	private Date started;
	
	private Date ended;
	
	private double progress;
	
	private String stackTrace;
	

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

	public long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public String getClassName() {
		String[] classSplit = className.split("\\.");
		
		return classSplit[classSplit.length - 1];
	}
	
	public Date getSubmitted() {
		return submitted;
	}

	public String getStatus() {
		return status;
	}

	public Date getStarted() {
		return started;
	}

	public Date getEnded() {
		return ended;
	}

	public double getProgress() {
		return progress;
	}
	
	public String getStackTrace() {
		return stackTrace;
	}
	
	public String getStackTraceAsHtml() {
		String result = StringEscapeUtils.escapeHtml4(stackTrace);
		
		if (null != stackTrace) {
			result = result.replace("\n", "<br/>");
		}
		
		return result;
	}
}
