package unit.uk.ac.exeter.QuinCe.jobs;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.User.NoSuchUserException;
import uk.ac.exeter.QuinCe.jobs.BadProgressException;
import uk.ac.exeter.QuinCe.jobs.UnrecognisedStatusException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobClassTypeException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobConstructorException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobClassNotFoundException;
import uk.ac.exeter.QuinCe.jobs.JobException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.NoSuchJobException;
import uk.ac.exeter.QuinCe.utils.MissingDataException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;

public class JobManagerTest extends BaseDbTest {

	private static final String TEN_SECOND_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.TenSecondJob";
	
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

	private List<String> tenSecondJobParams;
	
	@Before
	public void setUp() throws Exception {
		deleteAllJobs();
		destroyTestUser();
		createTestUser();
		tenSecondJobParams = new ArrayList<String>();
		tenSecondJobParams.add("1");
	}
	
	@Test(expected=NoSuchUserException.class)
	public void createJobMissingUser() throws Exception {
		User badUser = new User(-1, "nosuchuser@exeter.ac.uk", "Steve", "Jones");
		JobManager.addJob(getConnection(), badUser, TEN_SECOND_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=JobClassNotFoundException.class)
	public void createJobMissingClass() throws Exception {
		JobManager.addJob(getConnection(), testUser, NOT_EXISTING_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=InvalidJobClassTypeException.class)
	public void createJobNotInheritingJob() throws Exception {
		JobManager.addJob(getConnection(), testUser, NOT_INHERITING_JOB_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=InvalidJobConstructorException.class)
	public void createJobNoParamConstructor() throws Exception {
		JobManager.addJob(getConnection(), testUser, NO_PARAM_CONSTRUCTOR_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=InvalidJobConstructorException.class)
	public void createJobConnOnlyConstructor() throws Exception {
		JobManager.addJob(getConnection(), testUser, CONN_ONLY_CONSTRUCTOR_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test(expected=InvalidJobConstructorException.class)
	public void createJobWrongListTypeConstructor() throws Exception {
		JobManager.addJob(getConnection(), testUser, WRONG_LIST_TYPE_CONSTRUCTOR_JOB_CLASS, tenSecondJobParams);
	}
	
	@Test
	public void createJobGoodNoOwner() throws Exception {
		long jobID = JobManager.addJob(getConnection(), null, TEN_SECOND_JOB_CLASS, tenSecondJobParams);
		assertNotEquals(JobManager.NOT_ADDED, jobID);
		assertNotEquals(0, jobID);
		
		PreparedStatement stmt = getConnection().prepareStatement(GET_JOBS_QUERY);
		ResultSet storedJobs = stmt.executeQuery();
		
		assertTrue(storedJobs.next());
		assertNotEquals(JobManager.NOT_ADDED, storedJobs.getLong(1));
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
	}
	
	@Test
	public void createJobGoodWithOwner() throws Exception {
		long jobID = createTestJob();
		assertNotEquals(JobManager.NOT_ADDED, jobID);
		assertNotEquals(0, jobID);

		PreparedStatement stmt = getConnection().prepareStatement(GET_JOBS_QUERY);
		ResultSet storedJobs = stmt.executeQuery();
		
		assertTrue(storedJobs.next());
		assertNotEquals(JobManager.NOT_ADDED, storedJobs.getLong(1));
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
	}
	
	@Test(expected=UnrecognisedStatusException.class)
	public void setStatusInvalidStatus() throws Exception {
		long jobID = createTestJob();
		JobManager.setStatus(getConnection(), jobID, "INVALID_STATUS");
	}
	
	@Test(expected=NoSuchJobException.class)
	public void setStatusInvalidJob() throws Exception {
		JobManager.setStatus(getConnection(), 0, Job.WAITING_STATUS);
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
	
	@Test(expected=NoSuchJobException.class)
	public void setProgressInvalidJob() throws Exception {
		JobManager.setProgress(getConnection(), 0, 0);
	}
	
	@Test(expected=BadProgressException.class)
	public void setProgressNegative() throws Exception {
		long jobID = createTestJob();
		JobManager.setProgress(getConnection(), jobID, -1);
	}
	
	@Test(expected=BadProgressException.class)
	public void setProgressOver9000() throws Exception {
		long jobID = createTestJob();
		JobManager.setProgress(getConnection(), jobID, 9001);
	}
	
	@Test
	public void setProgressGood() throws Exception {
		long jobID = createTestJob();
		JobManager.setProgress(getConnection(), jobID, 50.7);
		
		double progress = -1;
		
		PreparedStatement stmt = getConnection().prepareStatement(GET_PROGRESS_QUERY);
		stmt.setLong(1, jobID);
		
		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			progress = result.getDouble(1);
		}
		
		assertEquals(50.7, progress, 0);
	}
	
	@Test(expected=NoSuchJobException.class)
	public void startJobNoSuchJob() throws Exception {
		JobManager.startJob(getConnection(), 0);
	}
	
	@Test
	public void startJobGood() throws Exception {
		long jobID = createTestJob();
		JobManager.startJob(getConnection(), jobID);
		
		PreparedStatement stmt = getConnection().prepareStatement(GET_STARTED_JOB_QUERY);
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
	}
	
	@Test(expected=NoSuchJobException.class)
	public void finishJobNoSuchJob() throws Exception {
		JobManager.finishJob(getConnection(), 0);
	}
	
	@Test
	public void finishJobGood() throws Exception {
		long jobID = createTestJob();
		JobManager.finishJob(getConnection(), jobID);
		
		PreparedStatement stmt = getConnection().prepareStatement(GET_FINISHED_JOB_QUERY);
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
	}
	
	@Test(expected=NoSuchJobException.class)
	public void errorJobNoSuchJob() throws Exception {
		JobManager.errorJob(getConnection(), 0, new Exception("Test exception"));
	}
	
	@Test
	public void errorJobGood() throws Exception {
		long jobID = createTestJob();
		JobManager.errorJob(getConnection(), jobID, new Exception("Test exception"));
		
		PreparedStatement stmt = getConnection().prepareStatement(GET_ERROR_JOB_QUERY);
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
	}
	

	@After
	public void tearDown() throws Exception {
		deleteAllJobs();
		destroyTestUser();
	}
	
	private boolean runSetStatusTest(String status) throws Exception {
		
		long jobID = createTestJob();
		JobManager.setStatus(getConnection(), jobID, status);
		
		boolean statusOK = false;
		
		PreparedStatement stmt = getConnection().prepareStatement(GET_STATUS_QUERY);
		stmt.setLong(1, jobID);
		
		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			if (result.getString(1).equalsIgnoreCase(status)) {
				statusOK = true;
			}
		}
		
		return statusOK;
	}
	
	private void deleteAllJobs() throws Exception {
		PreparedStatement stmt = getConnection().prepareStatement("DELETE FROM job");
		stmt.execute();
	}
	
	private long createTestJob() throws DatabaseException, MissingDataException, NoSuchUserException, JobClassNotFoundException, InvalidJobClassTypeException, InvalidJobConstructorException, JobException, Exception {
		return JobManager.addJob(getConnection(), testUser, TEN_SECOND_JOB_CLASS, tenSecondJobParams);
	}
}
