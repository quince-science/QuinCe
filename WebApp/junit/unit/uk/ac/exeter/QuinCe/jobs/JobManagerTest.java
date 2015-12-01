package unit.uk.ac.exeter.QuinCe.jobs;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.User.NoSuchUserException;
import uk.ac.exeter.QuinCe.jobs.BadProgressException;
import uk.ac.exeter.QuinCe.jobs.UnrecognisedStatusException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobClassTypeException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobConstructorException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobClassNotFoundException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.NoSuchJobException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import unit.uk.ac.exeter.QuinCe.jobs.TestJobs.TenSecondJob;

public class JobManagerTest extends BaseJobTest {
	
	private static final String NOT_EXISTING_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.ThisIsNotAnExistingClass";
	
	private static final String NOT_INHERITING_JOB_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.NotInheritingJob";
	
	private static final String NO_PARAM_CONSTRUCTOR_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.NoParamConstructorJob";
	
	private static final String CONN_ONLY_CONSTRUCTOR_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.ConnOnlyConstructorJob";

	private static final String WRONG_LIST_TYPE_CONSTRUCTOR_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.WrongListTypeConstructorJob";

	private static final String GET_JOBS_QUERY = "SELECT id, owner, submitted, class, parameters, status, started, ended, thread_name, progress, stack_trace FROM job";
	
	private static final String GET_STATUS_QUERY = "SELECT status FROM job WHERE id = ?";
	
	private static final String GET_PROGRESS_QUERY = "SELECT progress FROM job WHERE id = ?";
	
	private static final String GET_STARTED_JOB_QUERY = "SELECT status, started FROM job WHERE id = ?";
	
	private static final String GET_FINISHED_JOB_QUERY = "SELECT status, ended FROM job WHERE id = ?";

	private static final String GET_ERROR_JOB_QUERY = "SELECT status, ended, stack_trace FROM job WHERE id = ?";
	
	@Test(expected=MissingParamException.class)
	public void createJobNullDB() throws Exception {
		JobManager.addJob(null, testUser, TEN_SECOND_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=NoSuchUserException.class)
	public void createJobMissingUser() throws Exception {
		User badUser = new User(-1, "nosuchuser@exeter.ac.uk", "Steve", "Jones");
		JobManager.addJob(getDataSource(), badUser, TEN_SECOND_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=JobClassNotFoundException.class)
	public void createJobMissingClass() throws Exception {
		JobManager.addJob(getDataSource(), testUser, NOT_EXISTING_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=InvalidJobClassTypeException.class)
	public void createJobNotInheritingJob() throws Exception {
		JobManager.addJob(getDataSource(), testUser, NOT_INHERITING_JOB_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=InvalidJobConstructorException.class)
	public void createJobNoParamConstructor() throws Exception {
		JobManager.addJob(getDataSource(), testUser, NO_PARAM_CONSTRUCTOR_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=InvalidJobConstructorException.class)
	public void createJobConnOnlyConstructor() throws Exception {
		JobManager.addJob(getDataSource(), testUser, CONN_ONLY_CONSTRUCTOR_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=InvalidJobConstructorException.class)
	public void createJobWrongListTypeConstructor() throws Exception {
		JobManager.addJob(getDataSource(), testUser, WRONG_LIST_TYPE_CONSTRUCTOR_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test
	public void createJobGoodNoOwner() throws Exception {
		long jobID = JobManager.addJob(getDataSource(), null, TEN_SECOND_JOB_CLASS, tenSecondJobParams);
		assertNotEquals(DatabaseUtils.NO_DATABASE_RECORD, jobID);
		assertNotEquals(0, jobID);
		
		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement(GET_JOBS_QUERY);
		ResultSet storedJobs = stmt.executeQuery();
		
		assertTrue(storedJobs.next());
		assertNotEquals(DatabaseUtils.NO_DATABASE_RECORD, storedJobs.getLong(1));
		assertNotEquals(0, storedJobs.getLong(1));
		assertEquals(0, storedJobs.getInt(2));
		assertNotNull(storedJobs.getTimestamp(3));
		assertEquals(TEN_SECOND_JOB_CLASS, storedJobs.getString(4));
		
		String storedParams = storedJobs.getString(5);
		List<String> paramsList = StringUtils.delimitedToList(storedParams);
		assertEquals(tenSecondJobParams, paramsList);
		
		assertEquals(Job.WAITING_STATUS, storedJobs.getString(6));
		assertNull(storedJobs.getTimestamp(7));
		assertNull(storedJobs.getTimestamp(8));
		assertNull(storedJobs.getString(9));
		assertEquals(0.0, storedJobs.getFloat(10), 0);
		assertNull(storedJobs.getString(11));
		connection.close();
	}
	
	@Test
	public void createJobGoodWithOwner() throws Exception {
		long jobID = createTestJob();
		assertNotEquals(DatabaseUtils.NO_DATABASE_RECORD, jobID);
		assertNotEquals(0, jobID);

		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement(GET_JOBS_QUERY);
		ResultSet storedJobs = stmt.executeQuery();
		
		assertTrue(storedJobs.next());
		assertNotEquals(DatabaseUtils.NO_DATABASE_RECORD, storedJobs.getLong(1));
		assertEquals(testUser.getDatabaseID(), storedJobs.getInt(2));
		assertNotNull(storedJobs.getTimestamp(3));
		assertEquals(TEN_SECOND_JOB_CLASS, storedJobs.getString(4));
		
		String storedParams = storedJobs.getString(5);
		List<String> paramsList = StringUtils.delimitedToList(storedParams);
		assertEquals(tenSecondJobParams, paramsList);
		
		assertEquals(Job.WAITING_STATUS, storedJobs.getString(6));
		assertNull(storedJobs.getTimestamp(7));
		assertNull(storedJobs.getTimestamp(8));
		assertNull(storedJobs.getString(9));
		assertEquals(0.0, storedJobs.getFloat(10), 0);
		assertNull(storedJobs.getString(11));
		connection.close();
	}
	
	@Test(expected=MissingParamException.class)
	public void setStatusNullDB() throws Exception {
		long jobID = createTestJob();
		JobManager.setStatus(null, jobID, Job.WAITING_STATUS);
	}
	
	@Test(expected=UnrecognisedStatusException.class)
	public void setStatusInvalidStatus() throws Exception {
		long jobID = createTestJob();
		JobManager.setStatus(getDataSource(), jobID, "INVALID_STATUS");
	}
	
	@Test(expected=NoSuchJobException.class)
	public void setStatusInvalidJob() throws Exception {
		JobManager.setStatus(getDataSource(), 0, Job.WAITING_STATUS);
	}
	
	@Test
	public void setStatusWaiting() throws Exception {
		assertTrue(runSetStatusTest(Job.WAITING_STATUS));
	}
	
	@Test
	public void setStatusRunning() throws Exception {
		assertTrue(runSetStatusTest(Job.RUNNING_STATUS));
	}
	
	@Test
	public void setStatusFinished() throws Exception {
		assertTrue(runSetStatusTest(Job.FINISHED_STATUS));
	}
	
	@Test
	public void setStatusError() throws Exception {
		assertTrue(runSetStatusTest(Job.ERROR_STATUS));
	}
	
	@Test(expected=MissingParamException.class)
	public void setProgressNullDB() throws Exception {
		long jobID = createTestJob();
		JobManager.setProgress(null, jobID, 12.4);
	}
	
	@Test(expected=NoSuchJobException.class)
	public void setProgressInvalidJob() throws Exception {
		JobManager.setProgress(getDataSource(), 0, 0);
	}
	
	@Test(expected=BadProgressException.class)
	public void setProgressNegative() throws Exception {
		long jobID = createTestJob();
		JobManager.setProgress(getDataSource(), jobID, -1);
	}
	
	@Test(expected=BadProgressException.class)
	public void setProgressOver9000() throws Exception {
		long jobID = createTestJob();
		JobManager.setProgress(getDataSource(), jobID, 9001);
	}
	
	@Test
	public void setProgressZero() throws Exception {
		double testedProgress = setProgressWorker(0.0);
		assertEquals(0.0, testedProgress, 0);
	}
	
	@Test
	public void setProgressOneHundred() throws Exception {
		double testedProgress = setProgressWorker(100.0);
		assertEquals(100.0, testedProgress, 0);
	}
	
	@Test
	public void setProgressGood() throws Exception {
		double testedProgress = setProgressWorker(50.7);
		assertEquals(50.7, testedProgress, 0);
	}
	
	private double setProgressWorker(double progress) throws Exception {
		long jobID = createTestJob();
		JobManager.setProgress(getDataSource(), jobID, progress);
		
		double progressResult = -1;
		
		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement(GET_PROGRESS_QUERY);
		stmt.setLong(1, jobID);
		
		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			progressResult = result.getDouble(1);
		}
		connection.close();
		
		return progressResult;
	}
	

	
	@Test(expected=MissingParamException.class)
	public void startJobNullDB() throws Exception {
		long jobID = createTestJob();
		JobManager.startJob(null, jobID);
	}
	
	@Test(expected=NoSuchJobException.class)
	public void startJobNoSuchJob() throws Exception {
		JobManager.startJob(getDataSource(), 0);
	}
	
	@Test
	public void startJobGood() throws Exception {
		long jobID = createTestJob();
		JobManager.startJob(getDataSource(), jobID);
		
		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement(GET_STARTED_JOB_QUERY);
		stmt.setLong(1, jobID);
		
		String status = null;
		Timestamp time = null;
		
		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			status = result.getString(1);
			time = result.getTimestamp(2);
		}
		
		assertEquals(Job.RUNNING_STATUS, status);
		assertNotNull(time);
		connection.close();
	}
	
	@Test(expected=MissingParamException.class)
	public void finishJobNullDB() throws Exception {
		long jobID = createTestJob();
		JobManager.finishJob(null, jobID);
	}
	
	@Test(expected=NoSuchJobException.class)
	public void finishJobNoSuchJob() throws Exception {
		JobManager.finishJob(getDataSource(), 0);
	}
	
	@Test
	public void finishJobGood() throws Exception {
		long jobID = createTestJob();
		JobManager.finishJob(getDataSource(), jobID);
		
		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement(GET_FINISHED_JOB_QUERY);
		stmt.setLong(1, jobID);
		
		String status = null;
		Timestamp time = null;
		
		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			status = result.getString(1);
			time = result.getTimestamp(2);
		}
		
		assertEquals(Job.FINISHED_STATUS, status);
		assertNotNull(time);
		connection.close();
	}
	
	@Test(expected=MissingParamException.class)
	public void errorJobNullDB() throws Exception {
		long jobID = createTestJob();
		JobManager.errorJob(null, jobID, new Exception("Test exception"));
	}
	
	@Test(expected=NoSuchJobException.class)
	public void errorJobNoSuchJob() throws Exception {
		JobManager.errorJob(getDataSource(), 0, new Exception("Test exception"));
	}
	
	@Test
	public void errorJobGood() throws Exception {
		long jobID = createTestJob();
		JobManager.errorJob(getDataSource(), jobID, new Exception("Test exception"));
		
		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement(GET_ERROR_JOB_QUERY);
		stmt.setLong(1, jobID);
		
		String status = null;
		Timestamp time = null;
		String stackTrace = null;
		
		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			status = result.getString(1);
			time = result.getTimestamp(2);
			stackTrace = result.getString(3);
		}
		
		assertEquals(Job.ERROR_STATUS, status);
		assertNotNull(time);
		assertNotNull(stackTrace);
		connection.close();
	}
	
	@Test(expected=MissingParamException.class)
	public void getJobNoDB() throws Exception {
		JobManager.getJob(null, null, 0);
	}
	
	@Test(expected=NoSuchJobException.class)
	public void getJobNoSuchJob() throws Exception {
		JobManager.getJob(getDataSource(), null, 0);
	}
	
	@Test
	public void getJobGood() throws Exception {
		long jobID = createTestJob();
		Job storedJob = JobManager.getJob(getDataSource(), null, jobID);
		assertTrue(storedJob instanceof TenSecondJob);
	}
	
	@Test(expected=MissingParamException.class)
	public void getNextJobNullDataSource() throws Exception {
		JobManager.getNextJob(null, null);
	}
	
	@Test
	public void getNextJobEmpty() throws Exception {
		Job nextJob = JobManager.getNextJob(getDataSource(), null);
		assertNull(nextJob);
	}
	
	@Test
	public void getNextJobOne() throws Exception {
		long jobID = createTestJob();
		assertEquals(jobID, JobManager.getNextJob(getDataSource(), null).getID());
	}
	
	@Test
	public void getNextJobMultiple() throws Exception {
		long job1 = createTestJob();
		createTestJob();
		createTestJob();
		
		assertEquals(job1, JobManager.getNextJob(getDataSource(), null).getID());
	}
	
	private boolean runSetStatusTest(String status) throws Exception {
		
		long jobID = createTestJob();
		JobManager.setStatus(getDataSource(), jobID, status);
		
		boolean statusOK = false;
		
		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement(GET_STATUS_QUERY);
		stmt.setLong(1, jobID);
		
		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			if (result.getString(1).equalsIgnoreCase(status)) {
				statusOK = true;
			}
		}
		connection.close();
		
		return statusOK;
	}
}
