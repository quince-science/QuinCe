package uk.ac.exeter.QuinCe.data;

import java.util.Date;

/**
 * A read-only stub object for a single calibration run.
 * @author Steve Jones
 *
 */
public class CalibrationStub {

	/**
	 * The ID of the calibration's database record
	 */
	private long id;
	
	/**
	 * The ID of the instrument that this calibration belongs to
	 */
	private long instrumentId;
	
	/**
	 * The date of the calibration
	 */
	private Date date;
	
	/**
	 * Simple constructor for the two fields
	 * @param id The database ID
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
