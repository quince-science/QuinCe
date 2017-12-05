package uk.ac.exeter.QuinCe.data.Calculation;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import uk.ac.exeter.QCRoutines.config.ColumnConfig;
import uk.ac.exeter.QCRoutines.data.DataRecord;
import uk.ac.exeter.QCRoutines.data.DataRecordException;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public abstract class CalculationRecord extends DataRecord {
	
	/**
	 * Dummy set of date/time columns for the QCRoutines code.
	 * Set up in the {@code static} block
	 */
	private static TreeSet<Integer> dateTimeColumns;

	/**
	 * Dummy latitude column for the QCRoutines code
	 */
	private static final int LATITUDE_COLUMN = -1;
	
	/**
	 * Dummy longitude column for the QCRoutines code
	 */
	private static final int LONGITUDE_COLUMN = -1;
	
	// Set up the dummy date/time columns
	static {
		dateTimeColumns = new TreeSet<Integer>();
		dateTimeColumns.add(-1);
	}
	
	/**
	 * The dataset ID
	 */
	private long datasetId;
	
	/**
	 * The record date
	 */
	private LocalDateTime date = null;
	
	/**
	 * The longitude
	 */
	private Double longitude = null;
	
	/**
	 * The latitude
	 */
	private Double latitude = null;
	
	/**
	 * Create an empty calculation record for a given measurement in a given data set
	 * @param datasetId The dataset ID
	 * @param measurementId The measurement ID
	 * @param columnConfig The column configuration for the QC routines
	 */
	public CalculationRecord(long datasetId, int measurementId, ColumnConfig columnConfig) {
		super(measurementId, columnConfig);
		this.datasetId = datasetId;
	}
	
	/**
	 * Get the dataset ID
	 * @return The dataset ID
	 */
	public long getDatasetId() {
		return datasetId;
	}

	@Override
	public DateTime getTime() throws DataRecordException {
		return new org.joda.time.DateTime(DateTimeUtils.dateToLong(date), DateTimeZone.UTC);
	}

	@Override
	public TreeSet<Integer> getDateTimeColumns() {
		return dateTimeColumns;
	}

	@Override
	public double getLongitude() throws DataRecordException {
		return longitude;
	}

	@Override
	public int getLongitudeColumn() {
		return LONGITUDE_COLUMN;
	}

	@Override
	public double getLatitude() throws DataRecordException {
		return latitude;
	}

	@Override
	public int getLatitudeColumn() {
		return LATITUDE_COLUMN;
	}

	/**
	 * Set the record date
	 * @param date The date
	 */
	public void setDate(LocalDateTime date) {
		this.date = date;
	}
	
	/**
	 * Get the record date.
	 * 
	 * Note that this returns the date as a
	 * {@code java.time.LocalDateTime} object;
	 * the {@link #getTime()} method returns
	 * a {@code joda.time.DateTime} object for use with
	 * the QC_Routines library.
	 * 
	 * @return The record date
	 */
	public LocalDateTime getDate() {
		return date;
	}
	
	/**
	 * Set the longitude
	 * @param longitude The longitude
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
	/**
	 * Set the latitude
	 * @param latitude The latitude
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	
	/**
	 * Load the record data from the database.
	 * 
	 * Loads the position and date information itself, and then
	 * calls the {@link #loadCalculationData(Connection)} method to get the calculation data.
	 * 
	 * @param conn A database connection
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the record is not in the database
	 */
	public void loadData(Connection conn) throws MissingParamException, DatabaseException, RecordNotFoundException {
		DataSetDB.getDateAndPosition(conn, this);
		loadCalculationData(conn);
	}
	
	/**
	 * Load the calculation data specific to the this instance of the {@code CalculationRecord}.
	 * @param conn A database connection
	 */
	public abstract void loadCalculationData(Connection conn);
}
