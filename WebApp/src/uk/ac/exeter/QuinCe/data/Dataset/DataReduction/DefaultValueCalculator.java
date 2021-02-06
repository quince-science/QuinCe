package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class DefaultValueCalculator extends ValueCalculator {

  private SensorType sensorType;

  protected DefaultValueCalculator(String sensorType)
    throws SensorTypeNotFoundException {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();
    this.sensorType = sensorConfig.getSensorType(sensorType);
  }

  protected DefaultValueCalculator(SensorType sensorType) {
    this.sensorType = sensorType;
  }

  @Override
  public Double calculateValue(MeasurementValues measurementValues,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, DataReducer reducer, Connection conn)
    throws Exception {

    return -12D;

    /*
     * MeanCalculator mean = new MeanCalculator();
     * 
     * if (null != measurementValues.get(sensorType)) {
     * 
     * for (MeasurementValue value : measurementValues.get(sensorType)) {
     * 
     * SensorValue priorValue = null == value.getPrior() ? null :
     * allSensorValues.getById(value.getPrior());
     * 
     * SensorValue postValue = null == value.getPost() ? null :
     * allSensorValues.getById(value.getPost());
     * 
     * mean.add(interpolate(priorValue, postValue,
     * measurementValues.getMeasurement().getTime())); } }
     * 
     * return mean.mean();
     */
  }
}
