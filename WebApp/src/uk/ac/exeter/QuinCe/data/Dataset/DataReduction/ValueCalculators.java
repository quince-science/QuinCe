package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class ValueCalculators {

  private HashMap<SensorType, ValueCalculator> calculators;

  private static ValueCalculators instance = null;

  private ValueCalculators() {
    calculators = new HashMap<SensorType, ValueCalculator>();
  }

  public static ValueCalculators getInstance() {
    if (null == instance) {
      instance = new ValueCalculators();
    }

    return instance;
  }

  public Double calculateValue(MeasurementValues measurementValues,
    SensorType sensorType, Map<String, ArrayList<Measurement>> allMeasurements,
    Map<Long, SearchableSensorValuesList> allSensorValues, Connection conn)
    throws Exception {

    return getCalculator(sensorType).calculateValue(measurementValues,
      allMeasurements, allSensorValues, conn);

  }

  private ValueCalculator getCalculator(SensorType sensorType)
    throws ValueCalculatorException {
    if (!calculators.containsKey(sensorType)) {
      calculators.put(sensorType, initCalculator(sensorType));
    }

    return calculators.get(sensorType);
  }

  private static ValueCalculator initCalculator(SensorType sensorType)
    throws ValueCalculatorException {

    ValueCalculator result = null;

    try {
      switch (sensorType.getName()) {
      case "Equilibrator Pressure": {
        result = new EquilibratorPressureCalculator();
        break;
      }
      case "xCO₂ (with standards)": {
        result = new xCO2InGasWithStandardsCalculator();
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
