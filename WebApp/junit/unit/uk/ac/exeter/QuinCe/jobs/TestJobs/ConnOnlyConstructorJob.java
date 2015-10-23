package unit.uk.ac.exeter.QuinCe.jobs.TestJobs;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;

public class ConnOnlyConstructorJob extends Job {

	public ConnOnlyConstructorJob(Connection conn) throws Exception {
		super(conn, 0, null);
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
