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
	 * The date of the calibration
	 */
	private Date date;
	
	/**
	 * Simple constructor for the two fields
	 * @param id The database ID
	 * @param date The calibration date
	 */
	public CalibrationStub (long id, Date date) {
		this.id = id;
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
	 * Returns the calibration date
	 * @return The calibration date
	 */
	public Date getDate() {
		return date;
	}
	
}
