package unit.uk.ac.exeter.QuinCe.jobs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import unit.uk.ac.exeter.QuinCe.jobs.TestJobs.TenSecondJob;

/**
 * Tests for the Job class
 * @author Steve Jones
 *
 */
public class JobTest extends BaseJobTest {
	
	@Test(expected=MissingParamException.class)
	public void testConstructorMissingConnection() throws Exception {
		new TenSecondJob(null, null, 1, new ArrayList<String>());
	}
	
	@Test(expected=InvalidJobParametersException.class)
	public void testConstructorNullParameters() throws Exception {
		new TenSecondJob(getDataSource(), null, 1, null);
	}
	
	@Test(expected=InvalidJobParametersException.class)
	public void testConstructorTooFewParameters() throws Exception {
		new TenSecondJob(getDataSource(), null, 1, new ArrayList<String>());
	}
	
	@Test(expected=InvalidJobParametersException.class)
	public void testConstructorTooManyParameters() throws Exception {
		List<String> params = new ArrayList<String>();
		params.add("1");
		params.add("2");
		
		new TenSecondJob(getDataSource(), null, 1, params);
	}

	@Test(expected=InvalidJobParametersException.class)
	public void testConstructorInvalidParameter() throws Exception {
		List<String> params = new ArrayList<String>();
		params.add("M");

		new TenSecondJob(getDataSource(), null, 1, params);
	}
	
	@Test
	public void testConstructorGood() throws Exception {
		List<String> params = new ArrayList<String>();
		params.add("1");
		
		Job newJob = new TenSecondJob(getDataSource(), null, 1, params);
		assertNotNull(newJob);
	}
	
	@Test
	public void testSetProgress() throws Exception {
		long jobID = createTestJob(2);
		JobThread thread = new JobThread(false);
		thread.setupJob(JobManager.getJob(getDataSource(), null, jobID));
		thread.run();
		
		// Sleep for 14 seconds, then get the progress. It should be 50%
		Thread.sleep(14000);
	}
}
