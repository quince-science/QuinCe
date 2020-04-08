package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.utils.MeanCalculator;

public class EquilibratorPressureCalculator extends ValueCalculator {

  @Override
  public Double calculateValue(MeasurementValues measurementValues,
    Map<String, ArrayList<Measurement>> allMeasurements,
    Map<Long, SearchableSensorValuesList> allSensorValues, Connection conn)
    throws Exception {

    MeanCalculator mean = new MeanCalculator();

    // Calculate the absolute pressures
    DefaultValueCalculator absolutePressureCalculator = new DefaultValueCalculator(
      "Equilibrator Pressure (absolute)");
    absolutePressureCalculator.calculateValue(measurementValues, conn, mean);

    // Now get the differential pressures and ambient pressures, and calculate
    // the absolute equilibrator from those
    DefaultValueCalculator relativePressureCalculator = new DefaultValueCalculator(
      "Equilibrator Pressure (differential)");
    Double relativePressure = relativePressureCalculator.calculateValue(
      measurementValues, allMeasurements, allSensorValues, conn);

    DefaultValueCalculator ambientPressureCalculator = new DefaultValueCalculator(
      "Ambient Pressure");
    Double ambientPressure = ambientPressureCalculator.calculateValue(
      measurementValues, allMeasurements, allSensorValues, conn);

    if (!relativePressure.isNaN() && !ambientPressure.isNaN()) {
      mean.add(ambientPressure + relativePressure);
    }

    return mean.mean();
  }

}
