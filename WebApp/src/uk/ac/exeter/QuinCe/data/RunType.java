package uk.ac.exeter.QuinCe.data;

import java.io.Serializable;

import uk.ac.exeter.QuinCe.database.DatabaseUtils;

/**
 * Represents a single run type for an instrument
 * @author Steve Jones
 *
 */
public class RunType implements Comparable<RunType>, Serializable {

	private static final long serialVersionUID = -3429047807578653646L;

	/**
	 * Indicates that the run type should be ignored
	 */
	public static final int RUN_TYPE_NONE = -1;
	
	/**
	 * Indicates that the run type is for sea water CO2
	 */
	public static final int RUN_TYPE_WATER = 0;
	
	/**
	 * Indicates that teh run type is for atmospheric CO2
	 */
	public static final int RUN_TYPE_ATMOSPHERIC = 1;
	
	/**
	 * Indicates that the run type is for a gas standard
	 */
	public static final int RUN_TYPE_STANDARD = 2;
	
	/**
	 * The database ID of the run type
	 */
	private long databaseId;
	
	/**
	 * The database ID of the instrument to which this
	 * run type belongs
	 */
	private long instrumentID;
	
	/**
	 * The name of the run type
	 */
	private String name;
	
	/**
	 * The type
	 */
	private int runType;
	
	/**
	 * Constructor for all details
	 * @param instrumentID The ID of the instrument to which the RunType belongs
	 * @param name The name of the run type
	 * @param runType The type of the run type
	 */
	public RunType(long id, long instrumentID, String name, int runType) {
		this.databaseId = id;
		this.instrumentID = instrumentID;
		this.name = name;
		this.runType = runType;
	}
	
	/**
	 * Constructer for a run type that is not yet associated
	 * with an instrument
	 * @param name The name of the run type
	 * @param runType The type of the run type
	 */
	public RunType(String name, int runType) {
		this.databaseId = DatabaseUtils.NO_DATABASE_RECORD;
		this.instrumentID = DatabaseUtils.NO_DATABASE_RECORD;
		this.name = name;
		this.runType = runType;
	}
	
	/////////////// *** METHODS *** /////////////////
	
	/////////////// *** GETTERS AND SETTERS *** //////////////
	
	/**
	 * Returns the database ID of this run type
	 * @return The database ID
	 */
	public long getDatabaseId() {
		return databaseId;
	}
	
	/**
	 * Returns the ID of the instrument to which this
	 * run type belongs
	 * @return The ID of the instrument to which this run type belongs
	 */
	public long getInstrumentID() {
		return instrumentID;
	}

	/**
	 * Set the instrument ID
	 * @param instrumentID The instrument ID
	 */
	public void setInstrumentID(long instrumentID) {
		this.instrumentID = instrumentID;
	}
	
	/**
	 * Returns the run type name
	 * @return The run type name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the type of this run type
	 * @return The type of this run type
	 */
	public int getRunType() {
		return runType;
	}
	
	/**
	 * Determines whether or not this run type is a measurement,
	 * as opposed to a standard or an ignored type
	 * @return {@code true} if the run type is a measurement; {@code false} otherwise
	 */
	public boolean isMeasurementRunType() {
		return (runType == RUN_TYPE_ATMOSPHERIC || runType == RUN_TYPE_WATER);
	}
	
	/**
	 * Determines whether or not this run type is a for a gas standard,
	 * as opposed to a measurement or an ignored type
	 * @return {@code true} if the run type is for a gas standard; {@code false} otherwise
	 */
	public boolean isStandardRunType() {
		return runType == RUN_TYPE_STANDARD;
	}
	
	/**
	 * Determines whether or not this run type should be ignored.
	 * @return {@code true} if the run type should be ignored; {@code false} if it should be used.
	 */
	public boolean isIgnoredRunType() {
		return (runType == RUN_TYPE_NONE);
	}
	
	/**
	 * Returns the integer code for the run type
	 * @return The run type code
	 */
	public int getCode() {
		return runType;
	}

	/**
	 * Run types are compared by name
	 */
	@Override
	public int compareTo(RunType o) {
		return name.compareTo(o.getName());
	}
}
