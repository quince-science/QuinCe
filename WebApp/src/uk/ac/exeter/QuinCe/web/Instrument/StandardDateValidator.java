package uk.ac.exeter.QuinCe.web.Instrument;

/**
 * Checks dates of new gas standards to ensure that no standard
 * with that date already exists.
 * @author Steve Jones
 *
 */
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
