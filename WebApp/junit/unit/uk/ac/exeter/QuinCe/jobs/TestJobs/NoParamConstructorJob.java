package unit.uk.ac.exeter.QuinCe.jobs.TestJobs;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;

public class NoParamConstructorJob extends Job {

	public NoParamConstructorJob() throws Exception {
		super(null, null);
	}

	@Override
	protected void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void validateParameters() throws InvalidJobParametersException {
		// TODO Auto-generated method stub
		
	}
	
}
