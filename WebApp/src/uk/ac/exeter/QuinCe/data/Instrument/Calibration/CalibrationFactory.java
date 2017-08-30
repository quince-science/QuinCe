package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Factory for creating {@link Calibration} objects
 * @author Steve Jones
 *
 */
public class CalibrationFactory {

	/**
	 * Create a calibration for a specified calibration target
	 * @param dataSource A data source
	 * @param instrumentId The instrument ID
	 * @param type The calibration type
	 * @param target The calibration target
	 * @return The new calibration
	 * @throws CalibrationException If the calibration cannot be created
	 */
	public static Calibration createCalibration(DataSource dataSource, long instrumentId, String type, String target) throws CalibrationException {
		Calibration result;
		
		switch (type) {
		case GasStandard.GAS_STANDARD_CALIBRATION_TYPE: {
			try {
				result = makeGasStandard(dataSource, instrumentId, target);
			} catch (CalibrationException e) {
				throw e;
			} catch (Exception e) {
				throw new CalibrationException(e); 
			}
			break;
		}
		default: {
			throw new UnrecognisedCalibrationTypeException(type);
		}
		}
		
		
		return result;
	}
	
	/**
	 * Create a Gas Standard calibration object
	 * @param dataSource A data source
	 * @param instrumentId The instrument ID
	 * @param target The calibration target
	 * @return The new Gas Standard object
	 * @throws MissingParamException If any parameters to internal calls are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the instrument cannot be found
	 * @throws UnknownCalibrationTargetException If the specified target does not exist for the instrument
	 */
	private static GasStandard makeGasStandard(DataSource dataSource, long instrumentId, String target) throws MissingParamException, DatabaseException, RecordNotFoundException, UnknownCalibrationTargetException {
		
		GasStandard result;
		
		if (null == target) {
			result = new GasStandard(instrumentId);
		} else {
			List<String> standardNames = GasStandardDB.getInstance().getTargets(dataSource, instrumentId);
			if (!standardNames.contains(target)) {
				throw new UnknownCalibrationTargetException(instrumentId, GasStandard.GAS_STANDARD_CALIBRATION_TYPE, target);
			}
			
			result = new GasStandard(instrumentId, target);
		}
		
		return result;
	}
}
