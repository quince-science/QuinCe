package unit.uk.ac.exeter.QuinCe.jobs.TestJobs;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;

public class WrongListTypeConstructorJob extends Job {

	public WrongListTypeConstructorJob(DataSource dataSource, Properties config, long id, List<Integer> params) throws Exception {
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
