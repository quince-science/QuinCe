package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public abstract class ValueCalculator {

  public abstract Double calculateValue(MeasurementValues measurementValues,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, DataReducer reducer, Connection conn)
    throws Exception;

  protected double interpolate(LocalDateTime x0, double y0, LocalDateTime x1,
    double y1, LocalDateTime measurementTime) {

    return interpolate(DateTimeUtils.dateToLong(x0), y0,
      DateTimeUtils.dateToLong(x1), y1, measurementTime);

  }

  protected double interpolate(LocalDateTime x0, Double y0, LocalDateTime x1,
    Double y1, LocalDateTime measurementTime) throws ValueCalculatorException {

    double result;

    if (isMissing(y0) && isMissing(y1)) {
      throw new ValueCalculatorException("No values to interpolate");
    } else if (isMissing(y0)) {
      result = y1;
    } else if (isMissing(y1)) {
      result = y0;
    } else {
      result = interpolate(x0, y0.doubleValue(), x1, y1.doubleValue(),
        measurementTime);
    }

    return result;
  }

  protected double interpolate(SensorValue prior, SensorValue post,
    LocalDateTime measurementTime) {

    double x0 = DateTimeUtils.dateToLong(prior.getTime());
    double y0 = prior.getDoubleValue();
    double x1 = DateTimeUtils.dateToLong(post.getTime());
    double y1 = post.getDoubleValue();

    return interpolate(x0, y0, x1, y1, measurementTime);
  }

  protected double interpolate(double x0, double y0, double x1, double y1,
    LocalDateTime measurementTime) {
    // Target time
    double x = DateTimeUtils.dateToLong(measurementTime);

    return (y0 * (x1 - x) + y1 * (x - x0)) / (x1 - x0);
  }

  private boolean isMissing(Double value) {
    return null == value || value.isNaN();
  }
}
