package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class XCO2MeasurementValueCalculator extends MeasurementValueCalculator {

  protected final SensorType xco2SensorType;

  protected final SensorType xh2oSensorType;

  private static List<String> ALWAYS_DRY;

  static {
    ALWAYS_DRY = new ArrayList<String>();
    ALWAYS_DRY.add("Underway Marine pCO₂ from ¹²CO₂/¹³CO₂");
    ALWAYS_DRY.add("Underway Atmospheric pCO₂ from ¹²CO₂/¹³CO₂");
  }

  public XCO2MeasurementValueCalculator() throws SensorTypeNotFoundException {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    this.xco2SensorType = sensorConfig.getSensorType("xCO₂ (with standards)");
    this.xh2oSensorType = sensorConfig.getSensorType("xH₂O (with standards)");
  }

  protected XCO2MeasurementValueCalculator(String xco2SensorType,
    String xh2oSensorType) throws SensorTypeNotFoundException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    this.xco2SensorType = sensorConfig.getSensorType(xco2SensorType);
    this.xh2oSensorType = sensorConfig.getSensorType(xh2oSensorType);
  }

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    // Get the xCO2 as a simple value. Because it's a core sensor it will only
    // contain one
    MeasurementValue xCO2 = getCO2MeasurementValueCalculator().calculate(
      instrument, dataSet, timeReference, variable, xco2SensorType,
      allMeasurements, allSensorValues, conn);

    try {
      if (xCO2.getMemberCount() > 0 && dryingRequired(instrument, variable)) {

        MeasurementValue xH2O = getXH2OMeasurementValueCalculator().calculate(
          instrument, dataSet, timeReference, variable, xh2oSensorType,
          allMeasurements, allSensorValues, conn);

        // result = new MeasurementValue(xco2SensorType);
        xCO2.addSensorValues(xCO2, allSensorValues);
        xCO2.addSupportingSensorValues(xH2O, allSensorValues);

        xCO2.setCalculatedValue(
          dry(xCO2.getCalculatedValue(), xH2O.getCalculatedValue()));
      }
    } catch (RoutineException e) {
      throw new MeasurementValueCalculatorException(
        "Error extraction QC information", e);
    }

    return xCO2;
  }

  protected MeasurementValueCalculator getCO2MeasurementValueCalculator() {
    return new DefaultMeasurementValueCalculator();
  }

  protected MeasurementValueCalculator getXH2OMeasurementValueCalculator() {
    return new DefaultMeasurementValueCalculator();
  }

  protected boolean dryingRequired(Instrument instrument, Variable variable)
    throws MeasurementValueCalculatorException {

    boolean result;

    if (ALWAYS_DRY.contains(variable.getName())) {
      result = true;
    } else {
      TreeSet<SensorAssignment> co2Assignments = instrument
        .getSensorAssignments().get(xco2SensorType);

      // TODO We assume there's only one CO2 sensor. Handle more.
      if (co2Assignments.size() > 1) {
        throw new MeasurementValueCalculatorException(
          "Cannot handle multiple CO2 sensors yet!");
      }

      SensorAssignment assignment = co2Assignments.first();

      result = assignment.getDependsQuestionAnswer();
    }

    return result;
  }

  /**
   * Calculate dried CO2 using a moisture measurement
   *
   * @param co2
   *          The measured CO2 value
   * @param xH2O
   *          The moisture value
   * @return The 'dry' CO2 value
   */
  private double dry(Double co2, Double xH2O) {
    return co2 / (1.0 - (xH2O / 1000));
  }
}
