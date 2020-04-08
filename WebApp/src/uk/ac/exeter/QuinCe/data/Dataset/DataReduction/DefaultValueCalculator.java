package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
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
    Map<Long, SearchableSensorValuesList> allSensorValues, Connection conn)
    throws Exception {

    MeanCalculator mean = new MeanCalculator();

    calculateValue(measurementValues, conn, mean);

    return mean.mean();
  }

  protected void calculateValue(MeasurementValues measurementValues,
    Connection conn, MeanCalculator mean) throws Exception {

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
  }

  private double interpolate(SensorValue prior, SensorValue post,
    LocalDateTime measurementTime) {

    double x0 = DateTimeUtils.dateToLong(prior.getTime());
    double y0 = prior.getDoubleValue();
    double x1 = DateTimeUtils.dateToLong(post.getTime());
    double y1 = post.getDoubleValue();

    return interpolate(x0, y0, x1, y1, measurementTime);
  }
}
