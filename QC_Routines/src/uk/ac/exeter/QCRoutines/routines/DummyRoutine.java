package uk.ac.exeter.QCRoutines.routines;

import java.util.List;

import uk.ac.exeter.QCRoutines.Routine;
import uk.ac.exeter.QCRoutines.RoutineException;
import uk.ac.exeter.QCRoutines.data.DataRecord;

public class DummyRoutine extends Routine {

	public DummyRoutine() {
		super();
	}
	
	@Override
	public void initialise(List<String> parameters) throws RoutineException {
		System.out.println("Dummy routine initialised with parameter '" + parameters.get(0) + "'");
		
	}

	@Override
	public void processRecords(List<DataRecord> records) throws RoutineException {
		System.out.println("Dummy QC Routine was passed " + records.size() + " records");
	}

}
