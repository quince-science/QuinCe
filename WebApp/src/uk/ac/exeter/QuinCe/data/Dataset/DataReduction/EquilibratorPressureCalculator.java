package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;

public class EquilibratorPressureCalculator extends ValueCalculator {

  @Override
  public Double calculateValue(MeasurementValues measurementValues,
    Map<String, ArrayList<Measurement>> allMeasurements, Connection conn)
    throws Exception {

    MutableDouble sum = new MutableDouble(0);
    MutableInt count = new MutableInt(0);

    // Calculate the absolute pressures
    DefaultValueCalculator absolutePressureCalculator = new DefaultValueCalculator(
      "Equilibrator Pressure (absolute)");
    absolutePressureCalculator.calculateValue(measurementValues, conn, sum,
      count);

    // Now get the differential pressures and ambient pressures, and calculate
    // the absolute equilibrator from those
    DefaultValueCalculator relativePressureCalculator = new DefaultValueCalculator(
      "Equilibrator Pressure (differential)");
    Double relativePressure = relativePressureCalculator
      .calculateValue(measurementValues, allMeasurements, conn);

    DefaultValueCalculator ambientPressureCalculator = new DefaultValueCalculator(
      "Ambient Pressure");
    Double ambientPressure = ambientPressureCalculator
      .calculateValue(measurementValues, allMeasurements, conn);

    if (!relativePressure.isNaN() && !ambientPressure.isNaN()) {
      sum.add(ambientPressure + relativePressure);
      count.increment();
    }

    return mean(sum, count);
  }

}
