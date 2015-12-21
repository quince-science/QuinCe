package uk.ac.exeter.QuinCe.web.Instrument;

public class StandardDateValidator extends InstrumentDateValidator {
	
	@Override
	public String getTable() {
		return "gas_standard_deployment";
	}

	@Override
	public String getField() {
		return "deployed_date";
	}
	
	@Override
	public String getErrorMessage() {
		return "A gas standard deployment with the specified date already exists";
	}

}
