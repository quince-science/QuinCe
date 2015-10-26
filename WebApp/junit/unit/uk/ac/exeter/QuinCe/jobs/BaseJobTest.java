package unit.uk.ac.exeter.QuinCe.jobs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.User.NoSuchUserException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobClassTypeException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobConstructorException;
import uk.ac.exeter.QuinCe.jobs.JobClassNotFoundException;
import uk.ac.exeter.QuinCe.jobs.JobException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.utils.MissingDataException;
import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;

public abstract class BaseJobTest extends BaseDbTest {
	
	protected static final String TEN_SECOND_JOB_CLASS = "unit.uk.ac.exeter.QuinCe.jobs.TestJobs.TenSecondJob";

	protected List<String> tenSecondJobParams;

	@Before
	public void setUp() throws Exception {
		deleteAllJobs();
		destroyTestUser();
		createTestUser();
		tenSecondJobParams = new ArrayList<String>();
		tenSecondJobParams.add("1");
	}

	@After
	public void tearDown() throws Exception {
		deleteAllJobs();
		destroyTestUser();
	}

	protected long createTestJob() throws DatabaseException, MissingDataException, NoSuchUserException, JobClassNotFoundException, InvalidJobClassTypeException, InvalidJobConstructorException, JobException, Exception {
		return JobManager.addJob(getDataSource(), testUser, TEN_SECOND_JOB_CLASS, tenSecondJobParams);
	}
	
	protected long createTestJob(int chunks) throws DatabaseException, MissingDataException, NoSuchUserException, JobClassNotFoundException, InvalidJobClassTypeException, InvalidJobConstructorException, JobException, Exception {
		List<String> params = new ArrayList<String>();
		params.add(String.valueOf(chunks));
		return JobManager.addJob(getDataSource(), testUser, TEN_SECOND_JOB_CLASS, params);
	}

	private void deleteAllJobs() throws Exception {
		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement("DELETE FROM job");
		stmt.execute();
		connection.close();
	}
}
