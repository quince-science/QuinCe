package uk.ac.exeter.QuinCe.data.Dataset;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Factory class to get the {@link MeasurementValueCollector} for a given
 * {@link Variable}.
 */
public class MeasurementValueCollectorFactory {

  public static MeasurementValueCollector getCollector(Variable variable) {

    MeasurementValueCollector result;

    switch (variable.getName()) {
    case "Pro Oceanus CO₂ Water": {
      throw new NotImplementedException();

      // result = new ProOceanusWaterMeasurementValueCollector();
      // break;
    }
    case "Pro Oceanus CO₂ Atmosphere": {
      throw new NotImplementedException();

      // result = new ProOceanusAtmosphereMeasurementValueCollector();
      // break;
    }
    default: {
      result = new DefaultMeasurementValueCollector();
    }
    }

    return result;
  }
}
