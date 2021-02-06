package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class AtmosphericPressureMeasurementValueCalculator
  extends MeasurementValueCalculator {

  private static final double MOLAR_MASS_AIR = 28.97e-3;

  private final SensorType atmPresSensorType;

  private final SensorType equTempSensorType;

  public AtmosphericPressureMeasurementValueCalculator()
    throws SensorTypeNotFoundException {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    this.atmPresSensorType = sensorConfig.getSensorType("Atmospheric Pressure");
    this.equTempSensorType = sensorConfig
      .getSensorType("Equilibrator Temperature");
  }

  @Override
  public MeasurementValue calculate(Instrument instrument,
    Measurement measurement, SensorType sensorType,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn) throws MeasurementValueCalculatorException {

    MeasurementValue atmPresValue = new DefaultMeasurementValueCalculator()
      .calculate(instrument, measurement, atmPresSensorType, allMeasurements,
        allSensorValues, conn);

    return atmPresValue;

    /*
     * MeasurementValue equTempValue = new DefaultMeasurementValueCalculator()
     * .calculate(instrument, measurement, equTempSensorType, allMeasurements,
     * allSensorValues, conn);
     * 
     * Float sensorHeight = reducer.getFloatProperty("atm_pres_sensor_height");
     * 
     * Double correction = (atmPresValue.getCalculatedValue() * MOLAR_MASS_AIR)
     * / (Calculators.kelvin(equTempValue.getCalculatedValue()) * 8.314) * 9.8
     * sensorHeight;
     * 
     * MeasurementValue result = new MeasurementValue(sensorType);
     * result.addSensorValues(atmPresValue, allSensorValues);
     * result.addSensorValues(equTempValue, allSensorValues);
     * result.incrMemberCount(atmPresValue.getMemberCount());
     * result.setCalculatedValue(atmPresValue.getCalculatedValue() +
     * correction);
     */

    // return result;
  }

}
