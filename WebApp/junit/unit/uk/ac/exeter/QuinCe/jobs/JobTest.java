package unit.uk.ac.exeter.QuinCe.jobs;

import org.junit.Test;

import uk.ac.exeter.QuinCe.utils.MissingDataException;
import unit.uk.ac.exeter.QuinCe.jobs.TestJobs.TenSecondJob;

/**
 * Tests for the Job class
 * @author Steve Jones
 *
 */
public class JobTest extends BaseJobTest {
	
	@Test(expected=MissingDataException.class)
	public void constructorNoDB() throws Exception {
		new TenSecondJob(null, 1, null);
	}
}
