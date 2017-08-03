package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.TreeMap;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Object to hold all the details of an instrument
 * @author Steve Jones
 *
 */
public class Instrument {
	
	////////////// *** FIELDS *** ///////////////
	
	/**
	 * The instrument's ID in the database
	 */
	private long databaseID = DatabaseUtils.NO_DATABASE_RECORD;
	
	/**
	 * The ID of the owner of the instrument
	 */
	private long ownerId;
	
	/**
	 * The name of the instrument
	 */
	private String name = null;
	
	/**
	 * The instrument's file format definitions
	 */
	private InstrumentFileSet fileDefinitions = null;
	
	/**
	 * The assignment of columns in data files to sensors
	 */
	private SensorAssignments sensorAssignments = null;
	
	/**
	 * The run types for the instrument
	 */
	private TreeMap<String, RunTypeCategory> runTypes = null;
	
	/**
	 * The flushing time at the start of each run
	 */
	private int preFlushingTime = 0;
	
	/**
	 * The flushing time at the end of each run
	 */
	private int postFlushingTime = 0;
	
	/**
	 * The minimum water flow
	 */
	private int minimumWaterFlow = -1;
	
	/**
	 * Constructor for a complete instrument with no database ID
	 * @param owner The instrument's owner
	 * @param name The instrument name
	 * @param fileDefinitions The file format definitions
	 * @param sensorAssignments The sensor assignments
	 * @param runTypes The run types
	 * @param preFlushingTime The pre-flushing time
	 * @param postFlushingTime The post-flushing time
	 * @param minimumWaterFlow The minimum water flow
	 */
	public Instrument(User owner, String name, InstrumentFileSet fileDefinitions, SensorAssignments sensorAssignments, TreeMap<String, RunTypeCategory> runTypes, int preFlushingTime, int postFlushingTime, int minimumWaterFlow) {
		this.ownerId = owner.getDatabaseID();
		this.name = name;
		this.fileDefinitions = fileDefinitions;
		this.sensorAssignments = sensorAssignments;
		this.runTypes = runTypes;
		this.preFlushingTime = preFlushingTime;
		this.postFlushingTime = postFlushingTime;
		this.minimumWaterFlow = minimumWaterFlow;
	}
	
	
	/**
	 * Validate that all required information for the Instrument is present
	 * @throws InstrumentException If the instrument is not valid
	 */
	public void validate() throws InstrumentException {
		// TODO Write it!
	}
	
	/**
	 * Returns the ID of the instrument in the database
	 * @return The ID of the instrument in the database
	 */
	public long getDatabaseId() {
		return databaseID;
	}
	
	/**
	 * Sets the ID of the instrument in the database
	 * @param databaseID The database ID
	 */
	public void setDatabaseId(long databaseID) {
		this.databaseID = databaseID;
	}
	
	/**
	 * Returns the database ID of the owner of the instrument
	 * @return The ID of the owner of the instrument
	 */
	public long getOwnerId() {
		return ownerId;
	}

	/**
	 * Get the instrument's name
	 * @return The name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the pre-flushing time
	 * @return The pre-flushing time
	 */
	public int getPreFlushingTime() {
		return preFlushingTime;
	}
	
	/**
	 * Sets the pre-flushing time
	 * @param preFlushingTime The pre-flushing time
	 */
	public void setPreFlushingTime(int preFlushingTime) {
		this.preFlushingTime = preFlushingTime;
	}
	
	/**
	 * Returns the post-flushing time
	 * @return The post-flushing time
	 */
	public int getPostFlushingTime() {
		return postFlushingTime;
	}
	
	/**
	 * Sets the post-flushing time
	 * @param postFlushingTime The post-flushing time
	 */
	public void setPostFlushingTime(int postFlushingTime) {
		this.postFlushingTime = postFlushingTime;
	}
	
	/**
	 * Get the minimum water flow
	 * @return The minimum water flow
	 */
	public int getMinimumWaterFlow() {
		return minimumWaterFlow;
	}
	
	/**
	 * Set the minimum water flow
	 * @param minimumWaterFlow The minimum water flow
	 */
	public void getMinimumWaterFlow(int minimumWaterFlow) {
		this.minimumWaterFlow = minimumWaterFlow;
	}
}
