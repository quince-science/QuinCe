package uk.ac.exeter.QuinCe.jobs.test;

import java.util.List;
import java.util.Properties;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A test job that runs in chunks of 10 seconds.
 * It doesn't actually do anything.
 * @author Steve Jones
 *
 */
public class TenSecondJob extends Job {

	private int chunkCount = 1;
	
	public TenSecondJob(ResourceManager resourceManager, Properties config, long id, List<String> parameters) throws MissingParamException, InvalidJobParametersException {
		super(resourceManager, config, id, parameters);
		chunkCount = Integer.parseInt(parameters.get(0));
	}

	@Override
	protected void execute() throws JobFailedException {
		for (int i = 0; i < chunkCount; i++) {
			System.out.println("Job " + id + ": Chunk " + i + " of " + chunkCount);
			try {
				Thread.sleep(10000);
				setProgress(((double)i / (double)chunkCount) * 100);
			} catch(InterruptedException e) {
				// Don't care
			} catch(Exception e) {
				throw new JobFailedException(id, e);
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
