package unit.uk.ac.exeter.QuinCe.jobs.TestJobs;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;

public class ConnOnlyConstructorJob extends Job {

	public ConnOnlyConstructorJob(DataSource dataSource) throws Exception {
		super(dataSource, null, 0, null);
	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void validateParameters() throws InvalidJobParametersException {
		// TODO Auto-generated method stub
		
	}
	
}
