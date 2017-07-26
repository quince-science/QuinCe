package uk.ac.exeter.QuinCe.data.Instrument.Standards;

import java.util.Date;

/**
 * A read-only stub object for a single gas standard deployment.
 * @author Steve Jones
 *
 */
public class StandardStub {

	/**
	 * The ID of the standard's database record
	 */
	private long id;
	
	/**
	 * The ID of the instrument that this standard belongs to
	 */
	private long instrumentId;
	
	/**
	 * The date of the standard
	 */
	private Date deployedDate;
	
	/**
	 * Simple constructor for the two fields
	 * @param id The database ID
	 * @param instrumentId The ID of the instrument to which this standard belongs
	 * @param deployedDate The standard date
	 */
	public StandardStub (long id, long instrumentId, Date deployedDate) {
		this.id = id;
		this.instrumentId = instrumentId;
		this.deployedDate = deployedDate;
	}

	/**
	 * Returns the database ID
	 * @return The database ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns the database ID of the instrument that this standard belongs to
	 * @return The instrument ID
	 */
	public long getInstrumentId() {
		return instrumentId;
	}
	
	/**
	 * Returns the standard date
	 * @return The standard date
	 */
	public Date getDeployedDate() {
		return deployedDate;
	}
	
}
