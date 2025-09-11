package uk.ac.exeter.QuinCe.data.Dataset;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Factory class to get the {@link MeasurementValueCollector} for a given
 * {@link Variable}.
 */
public class MeasurementValueCollectorFactory {

  public static MeasurementValueCollector getCollector(Variable variable) {

    MeasurementValueCollector result;

    switch (variable.getName()) {
    case "Pro Oceanus CO₂ Water":
    case "Pro Oceanus CO₂ Atmosphere": {
      result = new ProOceanusMeasurementValueCollector();
      break;
    }
    case "CONTROS pCO₂": {
      result = new ControsPco2MeasurementValueCollector();
      break;
    }
    default: {
      result = new DefaultMeasurementValueCollector();
    }
    }

    return result;
  }
}
