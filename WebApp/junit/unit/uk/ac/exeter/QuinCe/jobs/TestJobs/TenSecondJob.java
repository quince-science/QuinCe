package unit.uk.ac.exeter.QuinCe.jobs.TestJobs;

import java.sql.Connection;
import java.util.List;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.utils.MissingDataException;

/**
 * A test job that takes five minutes to run.
 * It doesn't actually do anything.
 * @author Steve Jones
 *
 */
public class TenSecondJob extends Job {

	private int chunkCount = 1;
	
	public TenSecondJob(Connection connection, List<String> parameters) throws MissingDataException, InvalidJobParametersException {
		super(connection, parameters);
	}

	@Override
	protected void run() {
		for (int i = 0; i < chunkCount; i++) {
			try {
				Thread.sleep(10000);
			} catch(InterruptedException e) {
				// Don't care
			}
		}
	}

	@Override
	protected void validateParameters() throws InvalidJobParametersException {
		// For the test, we expect exactly one string, which is an integer
		
		if (null == parameters) {
			throw new InvalidJobParametersException("parameters are null");
		} else if (parameters.size() != 1) {
			throw new InvalidJobParametersException("Wrong number of parameters");
		} else {
			try {
				chunkCount = Integer.parseInt(parameters.get(0));
			} catch(NumberFormatException e) {
				throw new InvalidJobParametersException("It's not a number!");
			}
		}
	}
}
