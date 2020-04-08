package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.MeanCalculator;
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
    Map<Long, SearchableSensorValuesList> allSensorValues, DataReducer reducer,
    Connection conn) throws Exception {

    MeanCalculator mean = new MeanCalculator();

    if (null != measurementValues.get(sensorType)) {
      Map<Long, SensorValue> sensorValues = getSensorValues(measurementValues,
        sensorType, conn);

      for (MeasurementValue value : measurementValues.get(sensorType)) {

        SensorValue priorValue = sensorValues.get(value.getPrior());

        if (!value.hasPost()) {
          mean.add(priorValue.getDoubleValue());
        } else {
          SensorValue postValue = sensorValues.get(value.getPost());
          mean.add(interpolate(priorValue, postValue,
            measurementValues.getMeasurement().getTime()));
        }
      }
    }

    return mean.mean();
  }
}
