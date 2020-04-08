package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;

public class ValueCalculators {

  private HashMap<String, ValueCalculator> calculators;

  private static ValueCalculators instance = null;

  private ValueCalculators() {
    calculators = new HashMap<String, ValueCalculator>();
  }

  public static ValueCalculators getInstance() {
    if (null == instance) {
      instance = new ValueCalculators();
    }

    return instance;
  }

  public Double calculateValue(MeasurementValues measurementValues,
    String sensorType, Map<String, ArrayList<Measurement>> allMeasurements,
    Map<Long, SearchableSensorValuesList> allSensorValues, DataReducer reducer,
    Connection conn) throws Exception {

    return getCalculator(sensorType).calculateValue(measurementValues,
      allMeasurements, allSensorValues, reducer, conn);
  }

  private ValueCalculator getCalculator(String sensorType)
    throws ValueCalculatorException {

    if (!calculators.containsKey(sensorType)) {
      calculators.put(sensorType, initCalculator(sensorType));
    }

    return calculators.get(sensorType);
  }

  private static ValueCalculator initCalculator(String sensorType)
    throws ValueCalculatorException {

    ValueCalculator result = null;

    try {
      switch (sensorType) {
      case "Equilibrator Pressure": {
        result = new EquilibratorPressureCalculator();
        break;
      }
      case "xCO₂ (with standards)": {
        result = new xCO2InGasWithStandardsCalculator();
        break;
      }
      case "Atmospheric Pressure at Sea Level": {
        result = new AtmosphericPressureAtSeaLevelCalculator();
        break;
      }
      default: {
        result = new DefaultValueCalculator(sensorType);
      }
      }
    } catch (Exception e) {
      throw new ValueCalculatorException(sensorType, e);
    }

    return result;
  }
}
