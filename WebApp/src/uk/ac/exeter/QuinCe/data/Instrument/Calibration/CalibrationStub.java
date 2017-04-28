package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.Date;

import uk.ac.exeter.QuinCe.data.Instrument.CalibrationDB;

/**
 * <p>
 *   A read-only stub object for a single set of sensor calibrations for an instrument.
 * </p>
 * 
 * <p>
 *   This stub contains the information required to retrieve the full calibration coefficients
 *   for the instrument's sensors, using the {@link CalibrationDB#getCalibrationCoefficients(javax.sql.DataSource, CalibrationStub)}
 *   method.
 * </p>
 * 
 * @author Steve Jones
 * @see CalibrationDB
 */
public class CalibrationStub {

	/**
	 * The database ID of the calibration
	 */
	private long id;
	
	/**
	 * The database ID of the instrument to which the calibration belongs
	 */
	private long instrumentId;
	
	/**
	 * The date of the calibration
	 */
	private Date date;
	
	/**
	 * Simple constructor that stores the stub fields
	 * @param id The database ID
	 * @param instrumentId The instrument ID
	 * @param date The calibration date
	 */
	public CalibrationStub (long id, long instrumentId, Date date) {
		this.id = id;
		this.instrumentId = instrumentId;
		this.date = date;
	}

	/**
	 * Returns the database ID
	 * @return The database ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns the database ID of the instrument that this calibration belongs to
	 * @return The instrument ID
	 */
	public long getInstrumentId() {
		return instrumentId;
	}
	
	/**
	 * Returns the calibration date
	 * @return The calibration date
	 */
	public Date getDate() {
		return date;
	}
}
