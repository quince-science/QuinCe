package unit.uk.ac.exeter.QuinCe.jobs.TestJobs;

import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;

public class WrongListTypeConstructorJob extends Job {

	public WrongListTypeConstructorJob(DataSource dataSource, long id, List<Integer> params) throws Exception {
		super(dataSource, 0, null);
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
