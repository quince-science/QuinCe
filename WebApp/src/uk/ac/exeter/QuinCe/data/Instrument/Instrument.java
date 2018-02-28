package uk.ac.exeter.QuinCe.data.Instrument;


import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawData;
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
	 * The averaging mode
	 */
	private int averagingMode = DataSetRawData.AVG_MODE_NONE;

	/**
	 * Platform code
	 */
	private String platformCode = null;

	/**
	 * Constructor for a complete instrument that's already in the database
	 * @param databaseId The instrument's database ID
	 * @param ownerId The instrument owner's database ID
	 * @param name The instrument name
	 * @param fileDefinitions The file format definitions
	 * @param sensorAssignments The sensor assignments
	 * @param preFlushingTime The pre-flushing time
	 * @param postFlushingTime The post-flushing time
	 * @param minimumWaterFlow The minimum water flow
	 * @param averagingMode The averaging mode
	 * @param platformCode The platform code
	 */
	public Instrument(long databaseId, long ownerId, String name, InstrumentFileSet fileDefinitions, SensorAssignments sensorAssignments, int preFlushingTime, int postFlushingTime, int minimumWaterFlow, int averagingMode, String platformCode) {
		this.databaseID = databaseId;
		this.ownerId = ownerId;
		this.name = name;
		this.fileDefinitions = fileDefinitions;
		this.sensorAssignments = sensorAssignments;
		this.preFlushingTime = preFlushingTime;
		this.postFlushingTime = postFlushingTime;
		this.minimumWaterFlow = minimumWaterFlow;
		this.averagingMode = averagingMode;
		this.setPlatformCode(platformCode);

		//TODO Validate averaging mode
	}

	/**
	 * Constructor for a complete instrument with no database ID
	 * @param owner The instrument's owner
	 * @param name The instrument name
	 * @param fileDefinitions The file format definitions
	 * @param sensorAssignments The sensor assignments
	 * @param preFlushingTime The pre-flushing time
	 * @param postFlushingTime The post-flushing time
	 * @param minimumWaterFlow The minimum water flow
	 * @param averagingMode The averaging mode
	 * @param platformCode The platform code
	 */
	public Instrument(User owner, String name, InstrumentFileSet fileDefinitions, SensorAssignments sensorAssignments, int preFlushingTime, int postFlushingTime, int minimumWaterFlow, int averagingMode, String platformCode) {
		this.ownerId = owner.getDatabaseID();
		this.name = name;
		this.fileDefinitions = fileDefinitions;
		this.sensorAssignments = sensorAssignments;
		this.preFlushingTime = preFlushingTime;
		this.postFlushingTime = postFlushingTime;
		this.minimumWaterFlow = minimumWaterFlow;
		this.averagingMode = averagingMode;
		this.platformCode = platformCode;

		//TODO Validate averaging mode
	}

	/**
	 * Validate that all required information for the Instrument is present
	 * @param checkDatabaseColumns Specifies whether or not database columns
	 *                             have been assigned and should be checked
	 * @throws InstrumentException If the instrument is not valid
	 */
	public void validate(boolean checkDatabaseColumns) throws InstrumentException {
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
	 * Get the instrument's file definitions
	 * @return The file definitions
	 */
	public InstrumentFileSet getFileDefinitions() {
		return fileDefinitions;
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
	public void setMinimumWaterFlow(int minimumWaterFlow) {
		this.minimumWaterFlow = minimumWaterFlow;
	}

	/**
	 * Get the sensor assignments
	 * @return The sensor assignments
	 */
	public SensorAssignments getSensorAssignments() {
		return sensorAssignments;
	}

	/**
	 * Get the averaging mode
	 * @return The averaging mode
	 */
	public int getAveragingMode() {
		return averagingMode;
	}

	/**
	 * @return the platformCode
	 */
	public String getPlatformCode() {
		return platformCode;
	}

	/**
	 * @param platformCode the platformCode to set
	 */
	public void setPlatformCode(String platformCode) {
		this.platformCode = platformCode;
	}
}
