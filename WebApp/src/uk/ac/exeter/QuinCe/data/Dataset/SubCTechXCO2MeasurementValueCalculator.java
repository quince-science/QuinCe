package uk.ac.exeter.QuinCe.data.Dataset;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Custom version of the {@link XCO2MeasurementValueCalculator} for SubCTech
 * sensors.
 *
 * <p>
 * This provides fixed {@link SensorType}s and other information.
 * </p>
 */
public class SubCTechXCO2MeasurementValueCalculator
  extends XCO2MeasurementValueCalculator {

  public SubCTechXCO2MeasurementValueCalculator()
    throws SensorTypeNotFoundException {

    super("SubCTech xCO₂", "SubCTech xH₂O");
  }

  @Override
  protected boolean dryingRequired(Instrument instrument, Variable variable) {
    return true;
  }

  @Override
  protected MeasurementValueCalculator getCO2MeasurementValueCalculator() {
    return new SubCTechMeasurementValueCalculator();
  }

  @Override
  protected MeasurementValueCalculator getXH2OMeasurementValueCalculator() {
    return new SubCTechMeasurementValueCalculator();
  }

}
