package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for storing and retrieving gas standards information from the database
 * @author Steve Jones
 *
 */
public class ExternalStandardDB extends CalibrationDB {

	/**
	 * The calibration type for gas standards
	 */
	public static final String EXTERNAL_STANDARD_CALIBRATION_TYPE = "EXTERNAL_STANDARD";
	
	/**
	 * The singleton instance of the class
	 */
	private static ExternalStandardDB instance = null;
	
	/**
	 * Basic constructor
	 */
	public ExternalStandardDB() {
		super();
	}
	
	/**
	 * Retrieve the singleton instance of the class
	 * @return The singleton
	 */
	public static ExternalStandardDB getInstance() {
		if (null == instance) {
			instance = new ExternalStandardDB();
		}
		
		return instance;
	}
	
	/**
	 * Destroy the singleton instance
	 */
	public static void destroy() {
		instance = null;
	}
	
	@Override
	public List<String> getTargets(DataSource dataSource, long instrumentId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		List<String> standardNames = InstrumentDB.getRunTypes(dataSource, instrumentId, RunTypeCategory.EXTERNAL_STANDARD_CATEGORY.getCode());
		if (standardNames.size() == 0) {
			throw new RecordNotFoundException("No external standard names found for instrument " + instrumentId);
		}

		return Collections.unmodifiableList(standardNames);
	}
	
	@Override
	public String getCalibrationType() {
		return EXTERNAL_STANDARD_CALIBRATION_TYPE;
	}
}
