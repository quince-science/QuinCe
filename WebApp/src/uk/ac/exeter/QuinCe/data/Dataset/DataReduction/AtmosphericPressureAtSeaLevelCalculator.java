package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class AtmosphericPressureAtSeaLevelCalculator extends ValueCalculator {

  private final SensorType atmosPressureSensorType;

  private final SensorType equTempSensorType;

  private static final double MOLAR_MASS_AIR = 28.97e-3;

  public AtmosphericPressureAtSeaLevelCalculator()
    throws SensorTypeNotFoundException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    this.atmosPressureSensorType = sensorConfig
      .getSensorType("Atmospheric Pressure");
    this.equTempSensorType = sensorConfig
      .getSensorType("Equilibrator Temperature");
  }

  @Override
  public Double calculateValue(MeasurementValues measurementValues,
    Map<String, ArrayList<Measurement>> allMeasurements,
    Map<Long, SearchableSensorValuesList> allSensorValues, DataReducer reducer,
    Connection conn) throws Exception {

    // Get the atmospheric pressure value
    DefaultValueCalculator pressureValueCalculator = new DefaultValueCalculator(
      atmosPressureSensorType);
    Double pressure = pressureValueCalculator.calculateValue(measurementValues,
      allMeasurements, allSensorValues, reducer, conn);

    // Get the temperature value
    DefaultValueCalculator tempValueCalculator = new DefaultValueCalculator(
      equTempSensorType);
    Double temperature = tempValueCalculator.calculateValue(measurementValues,
      allMeasurements, allSensorValues, reducer, conn);

    Float sensorHeight = reducer.getVariableAttribute("atm_pres_sensor_height");

    Double correction = (pressure * MOLAR_MASS_AIR)
      / (Calculators.kelvin(temperature) * 8.314) * 9.8 * sensorHeight;

    return pressure + correction;
  }
}
