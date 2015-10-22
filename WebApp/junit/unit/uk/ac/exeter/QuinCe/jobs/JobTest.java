package unit.uk.ac.exeter.QuinCe.jobs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.utils.MissingDataException;
import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;
import unit.uk.ac.exeter.QuinCe.jobs.TestJobs.TenSecondJob;

/**
 * Tests for the Job class
 * @author Steve Jones
 *
 */
public class JobTest extends BaseDbTest {
	
	@Test(expected=MissingDataException.class)
	public void testConstructorMissingConnection() throws Exception {
		new TenSecondJob(null, new ArrayList<String>());
	}
	
	@Test(expected=InvalidJobParametersException.class)
	public void testConstructorNullParameters() throws Exception {
		new TenSecondJob(getConnection(), null);
	}
	
	@Test(expected=InvalidJobParametersException.class)
	public void testConstructorTooFewParameters() throws Exception {
		new TenSecondJob(getConnection(), new ArrayList<String>());
	}
	
	@Test(expected=InvalidJobParametersException.class)
	public void testConstructorTooManyParameters() throws Exception {
		List<String> params = new ArrayList<String>();
		params.add("1");
		params.add("2");
		
		new TenSecondJob(getConnection(), params);
	}

	@Test(expected=InvalidJobParametersException.class)
	public void testConstructorInvalidParameter() throws Exception {
		List<String> params = new ArrayList<String>();
		params.add("M");

		new TenSecondJob(getConnection(), params);
	}
	
	@Test
	public void testConstructorGood() throws Exception {
		List<String> params = new ArrayList<String>();
		params.add("1");
		
		Job newJob = new TenSecondJob(getConnection(), params);
		assertNotNull(newJob);
	}
	
	
}
