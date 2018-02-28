package uk.ac.exeter.QuinCe.data.Instrument;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * A stub object for an instrument, containing only information
 * useful for display in a list of instruments.
 * @author Steve Jones
 *
 */
public class InstrumentStub {

	/**
	 * The instrument's database ID
	 */
	private long id;

	/**
	 * The instrument's name
	 */
	private String name;

	/**
	 * Indicates whether or not the instrument has sensors that require
	 * calibration within QuinCe
	 */
	private boolean calibratableSensors;

	/**
	 * Simple constructor
	 * @param id The instrument's database ID
	 * @param name The instrument's name
	 * @param calibratableSensors Indicates the presence of sensors requiring calibration
	 */
	public InstrumentStub(long id, String name, boolean calibratableSensors) {
		this.id = id;
		this.name = name;
		this.calibratableSensors = calibratableSensors;
	}

	/**
	 * Returns the complete Instrument object for this stub
	 * @return The complete Instrument object
	 * @throws MissingParamException If the call to the database routine is incorrect (should never happen!)
	 * @throws DatabaseException If an error occurs while retrieving the data from the database
	 * @throws RecordNotFoundException If the instrument record cannot be found in the database
	 * @throws ResourceException If the data source cannot be retrieved
	 * @throws InstrumentException If any instrument details are invalid
	 */
	public Instrument getFullInstrument() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException, InstrumentException {
		return InstrumentDB.getInstrument(ServletUtils.getDBDataSource(), id, ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
	}

	///////// *** GETTERS AND SETTERS *** ///////////
	/**
	 * Returns the instrument's database ID
	 * @return The instrument's database ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * Return the instrument's name
	 * @return The instrument's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Determine whether or not the instrument has sensors that require
	 * calibration within QuinCe
	 * @return {@code true} if the instrument has sensors that need calibrating; {@code false if not}.
	 */
	public boolean getCalibratableSensors() {
		return calibratableSensors;
	}
}
