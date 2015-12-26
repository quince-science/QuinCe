package uk.ac.exeter.QuinCe.jobs;

import java.util.Date;

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
	

	public JobSummary(long id, User owner, String className, Date submitted, String status, Date started, Date ended, double progress) {
		this.id = id;
		this.owner = owner;
		this.className = className;
		this.submitted = submitted;
		this.status = status;
		this.started = started;
		this.ended = ended;
		this.progress = progress;
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
}
