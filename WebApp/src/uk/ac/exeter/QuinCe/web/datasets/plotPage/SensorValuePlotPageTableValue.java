package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.Arrays;
import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;

public class SensorValuePlotPageTableValue implements PlotPageTableValue {

  private final SensorValue sensorValue;

  /**
   * Builds a {@link PlotPageTableValue} from a {@link SensorValue}.
   *
   * @param sensorValue
   *          The {@link SensorValue}.
   * @param used
   *          Whether the value is used in a calculation.
   * @throws RoutineException
   *           If the QC message cannot be extracted.
   */
  public SensorValuePlotPageTableValue(SensorValue sensorValue) {
    this.sensorValue = sensorValue;
  }

  @Override
  public long getId() {
    return sensorValue.getId();
  }

  @Override
  public String getValue() {
    return null == sensorValue ? ""
      : StringUtils.formatNumber(sensorValue.getValue());
  }

  @Override
  public Object getRawValue() {
    return sensorValue;
  }

  @Override
  public Flag getQcFlag(DatasetSensorValues allSensorValues) {
    return sensorValue.getDisplayFlag(allSensorValues);
  }

  @Override
  public String getQcMessage(DatasetSensorValues allSensorValues,
    boolean replaceNewlines) {
    String result = "*Error getting QC message*";

    try {
      String message = sensorValue.getDisplayQCMessage(allSensorValues);
      result = message;
    } catch (RoutineException e) {
      ExceptionUtils.printStackTrace(e);
    }

    if (replaceNewlines) {
      result = StringUtils.replaceNewlines(result);
    }

    return result;
  }

  @Override
  public boolean getFlagNeeded() {
    return sensorValue.flagNeeded();
  }

  /**
   * Get the {@link SensorValue} that is the basis for this column.
   *
   * @return The sensor value.
   */
  public SensorValue getSensorValue() {
    return sensorValue;
  }

  @Override
  public boolean isNull() {
    return null == sensorValue;
  }

  @Override
  public char getType() {
    return PlotPageTableValue.MEASURED_TYPE;
  }

  @Override
  public Collection<Long> getSources() {
    return Arrays.asList(sensorValue.getId());
  }
}
