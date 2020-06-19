package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.AutoQCResult;

/**
 * An extended version of the {@link SensorValue} that includes the run type
 * relevant to that value.
 *
 * @author Steve Jones
 *
 */
public class RunTypeSensorValue extends SensorValue {

  private final String runType;

  public RunTypeSensorValue(long databaseId, long datasetId, long columnId,
    LocalDateTime time, String value, AutoQCResult autoQc, Flag userQcFlag,
    String userQcMessage, String runType) {

    super(databaseId, datasetId, columnId, time, value, autoQc, userQcFlag,
      userQcMessage);
    this.runType = runType;
  }

  /**
   * Create a clone of an existing {@link SensorValue} object and add a run
   * type.
   *
   * @param sensorValue
   *          The sensor value.
   * @param runType
   *          The run type.
   */
  public RunTypeSensorValue(SensorValue sensorValue, String runType) {
    super(sensorValue.getId(), sensorValue.getDatasetId(),
      sensorValue.getColumnId(), sensorValue.getTime(), sensorValue.getValue(),
      sensorValue.getAutoQcResult(), sensorValue.getUserQCFlag(),
      sensorValue.getUserQCMessage());

    this.runType = runType;
  }

  public String getRunType() {
    return runType;
  }
}
