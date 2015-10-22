package unit.uk.ac.exeter.QuinCe.jobs.TestJobs;

import java.sql.Connection;
import java.util.List;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;

public class WrongListTypeConstructorJob extends Job {

	public WrongListTypeConstructorJob(Connection conn, List<Integer> params) throws Exception {
		super(conn, null);
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
