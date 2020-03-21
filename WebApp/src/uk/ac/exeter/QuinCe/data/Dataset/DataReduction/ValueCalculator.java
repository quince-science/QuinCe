package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public abstract class ValueCalculator {

  public abstract Double calculateValue(MeasurementValues measurementValues,
    Map<String, ArrayList<Measurement>> allMeasurements, Connection conn)
    throws Exception;

  protected Double mean(MutableDouble sum, MutableInt count) {

    Double mean = Double.NaN;
    if (count.intValue() > 0) {
      mean = sum.doubleValue() / count.intValue();
    }

    return mean;
  }

  protected Map<Long, SensorValue> getSensorValues(
    MeasurementValues measurementValues, SensorType sensorType, Connection conn)
    throws MissingParamException, DatabaseException, InvalidFlagException {

    HashMap<Long, SensorValue> result = new HashMap<Long, SensorValue>();

    List<Long> ids = getSensorValueIds(measurementValues.get(sensorType));
    List<SensorValue> sensorValues = DataSetDataDB.getSensorValuesById(conn,
      measurementValues.getMeasurement().getDatasetId(), ids);

    for (SensorValue value : sensorValues) {
      result.put(value.getId(), value);
    }

    return result;
  }

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

  protected double interpolate(double x0, double y0, double x1, double y1,
    LocalDateTime measurementTime) {
    // Target time
    double x = DateTimeUtils.dateToLong(measurementTime);

    return (y0 * (x1 - x) + y1 * (x - x0)) / (x1 - x0);
  }

  private List<Long> getSensorValueIds(List<MeasurementValue> values) {
    List<Long> result = new ArrayList<Long>(values.size() * 2);

    for (MeasurementValue value : values) {
      result.add(value.getPrior());
      if (value.hasPost()) {
        result.add(value.getPost());
      }
    }

    return result;
  }

  private boolean isMissing(Double value) {
    return null == value || value.isNaN();
  }
}
