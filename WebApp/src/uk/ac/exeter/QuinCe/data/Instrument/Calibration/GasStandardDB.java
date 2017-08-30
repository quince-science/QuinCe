package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for storing and retrieving gas standards information from the database
 * @author Steve Jones
 *
 */
public class GasStandardDB extends CalibrationDB {

	/**
	 * The singleton instance of the class
	 */
	private static GasStandardDB instance = null;
	
	/**
	 * The run type code for gas standards
	 */
	public static final String GAS_STANDARD_RUNTYPE = "STD";

	/**
	 * Basic constructor
	 */
	public GasStandardDB() {
		super();
	}
	
	/**
	 * Retrieve the singleton instance of the class
	 * @return The singleton
	 */
	public static GasStandardDB getInstance() {
		if (null == instance) {
			instance = new GasStandardDB();
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
		List<String> standardNames = InstrumentDB.getRunTypes(dataSource, instrumentId, GAS_STANDARD_RUNTYPE);
		if (standardNames.size() == 0) {
			throw new RecordNotFoundException("No gas standard names found for instrument " + instrumentId);
		}

		return Collections.unmodifiableList(standardNames);
	}
	
	@Override
	public String getCalibrationType() {
		return GasStandard.GAS_STANDARD_CALIBRATION_TYPE;
	}
}
