package uk.ac.exeter.QuinCe.data;

import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.GasStandardDB;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Holds the calibration coefficients for a single sensor
 * @author Steve Jones
 *
 */
public class StandardConcentration {

	/**
	 * The name of the gas standard
	 */
	private String standardName;
	
	/**
	 * The concentration of the standard
	 */
	private double concentration = 0.0;
		
	/////////// *** METHODS *** ////////////////////
	
	/**
	 * Creates a concentrations object with the specified standard name.
	 * @param sensorCode The standard name
	 */
	public StandardConcentration(String standardName) {
		this.standardName = standardName;
	}
	
	/**
	 * Construct a list of concentration objects for all the standards for given instrument
	 * @param instrument The instrument
	 * @return A list of concentration objects
	 * @throws ResourceException If the data source cannot be retrieved
	 * @throws RecordNotFoundException  If no standards are found for the instrument
	 * @throws DatabaseException If an error occurs while retrieving information from the database
	 * @throws MissingParamException If any of the parameters are missing
	 */
	public static List<StandardConcentration> initConcentrations(Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		List<StandardConcentration> result = new ArrayList<StandardConcentration>();
		
		for (String standardName : GasStandardDB.getStandardNames(ServletUtils.getDBDataSource(), instrument.getDatabaseId())) {
			result.add(new StandardConcentration(standardName));
		}
		
		return result;
	}
	
	
	/////////// *** GETTERS AND SETTERS *** //////////////

	/**
	 * Returns the standard name
	 * @return The standard name
	 */
	public String getStandardName() {
		return standardName;
	}

	/**
	 * Returns the concentration
	 * @return The concentration
	 */
	public double getConcentration() {
		return concentration;
	}

	/**
	 * Sets the concentration
	 * @param concentration The concentration
	 */
	public void setConcentration(double concentration) {
		this.concentration = concentration;
	}
}
