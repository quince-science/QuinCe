package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Root object for a single measurement in a dataset
 *
 * @author Steve Jones
 *
 */
public class Measurement {

  /**
   * The measurement's database ID
   */
  private long id;

  /**
   * The ID of the dataset to which this measurement belongs
   */
  private final long datasetId;

  /**
   * The measured variable
   */
  private final InstrumentVariable variable;

  /**
   * The timestamp of the measurement
   */
  private final LocalDateTime time;

  /**
   * The run type of the measurement (optional)
   */
  private final String runType;

  /**
   * Constructor for a brand new measurement that is not yet in the database
   * @param datasetId The ID of the dataset to which the measurement belongs
   * @param variable The variable that is measured
   * @param time The timestamp of the measurement
   * @param runType The run type of the measurement
   */
  public Measurement(long datasetId, InstrumentVariable variable,
    LocalDateTime time, String runType) {

    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.datasetId = datasetId;
    this.variable = variable;
    this.time = time;
    this.runType = runType;
  }

  /**
   * Constructor for a measurement from the database
   * @param id The measurement's database ID
   * @param datasetId The ID of the dataset to which the measurement belongs
   * @param variable The variable that is measured
   * @param time The timestamp of the measurement
   * @param runType The run type of the measurement
   */
  public Measurement(long id, long datasetId, InstrumentVariable variable,
    LocalDateTime time, String runType) {

    this.id = id;
    this.datasetId = datasetId;
    this.variable = variable;
    this.time = time;
    this.runType = runType;
  }

  /**
   * Set the database ID for this measurement
   * @param id The database ID
   */
  protected void setDatabaseId(long id) {
    this.id = id;
  }

  /**
   * Get the database ID of this measurement
   * @return The measurement ID
   */
  public long getId() {
    return id;
  }

  /**
   * Get the database ID of the dataset to which this measurement belongs
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  /**
   * Get the variable measured in this measurement
   * @return The measured variable
   */
  public InstrumentVariable getVariable() {
    return variable;
  }

  /**
   * Get the time of the measurement
   * @return The measurement time
   */
  public LocalDateTime getTime() {
    return time;
  }

  public String getRunType() {
    return runType;
  }
}
