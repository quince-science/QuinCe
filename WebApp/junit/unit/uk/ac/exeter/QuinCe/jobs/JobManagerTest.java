package unit.uk.ac.exeter.QuinCe.jobs;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.NoSuchUserException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobClassTypeException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobConstructorException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobClassNotFoundException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;

public class JobManagerTest extends BaseDbTest {

	private static final String TEN_SECOND_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.TenSecondJob";
	
	private static final String NOT_EXISTING_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.ThisIsNotAnExistingClass";
	
	private static final String NOT_INHERITING_JOB_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.NotInheritingJob";
	
	private static final String NO_PARAM_CONSTRUCTOR_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.NoParamConstructorJob";
	
	private static final String CONN_ONLY_CONSTRUCTOR_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.ConnOnlyConstructorJob";

	private static final String WRONG_LIST_TYPE_CONSTRUCTOR_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.WrongListTypeConstructorJob";

	private static final String GET_JOBS_STATEMENT = "SELECT id, owner, submitted, class, parameters, status, started, thread_name, progress FROM job";
	
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
		
		PreparedStatement stmt = getConnection().prepareStatement(GET_JOBS_STATEMENT);
		ResultSet storedJobs = stmt.executeQuery();
		
		assertTrue(storedJobs.next());
		assertEquals(0, storedJobs.getInt(2));
		assertNotNull(storedJobs.getTimestamp(3));
		assertEquals(TEN_SECOND_JOB_CLASS, storedJobs.getString(4));
		
		String storedParams = storedJobs.getString(5);
		List<String> paramsList = StringUtils.delimitedToList(storedParams);
		assertEquals(tenSecondJobParams, paramsList);
		
		assertEquals(Job.WAITING_STATUS, storedJobs.getString(6));
		assertNull(storedJobs.getString(7));
		assertEquals(0.0, storedJobs.getFloat(8), 0);
	}
	
	@Test
	public void createJobGoodWithOwner() throws Exception {
		long jobID = JobManager.addJob(getConnection(), testUser, TEN_SECOND_JOB_CLASS, tenSecondJobParams);
		assertNotEquals(JobManager.NOT_ADDED, jobID);

		PreparedStatement stmt = getConnection().prepareStatement(GET_JOBS_STATEMENT);
		ResultSet storedJobs = stmt.executeQuery();
		
		assertTrue(storedJobs.next());
		assertEquals(testUser.getDatabaseID(), storedJobs.getInt(2));
		assertNotNull(storedJobs.getTimestamp(3));
		assertEquals(TEN_SECOND_JOB_CLASS, storedJobs.getString(4));
		
		String storedParams = storedJobs.getString(5);
		List<String> paramsList = StringUtils.delimitedToList(storedParams);
		assertEquals(tenSecondJobParams, paramsList);
		
		assertEquals(Job.WAITING_STATUS, storedJobs.getString(6));
		assertNull(storedJobs.getString(7));
		assertEquals(0.0, storedJobs.getFloat(8), 0);
	}
	
	@After
	public void tearDown() throws Exception {
		deleteAllJobs();
		destroyTestUser();
	}
	
	private void deleteAllJobs() throws Exception {
		PreparedStatement stmt = getConnection().prepareStatement("DELETE FROM job");
		stmt.execute();
	}
}
