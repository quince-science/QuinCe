package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * <b>NOTE: THIS IS NOT CURRENTLY FUNCTIONAL PENDING A REVIEW OF CALCULATION
 * REQUIREMENTS.</b> {@link MeasurementLocator} for the AVSCO2 sensor.
 */
public class ASVCO2MeasurementLocator extends MeasurementLocator {

  /**
   * Value indicating that the sensor is taking water measurements.
   */
  private static final String WATER_MODE = "ep";

  /**
   * Value indicating that the sensor is taking atmospheric measurements.
   */
  private static final String ATM_MODE = "ap";

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset, DatasetSensorValues allSensorValues)
    throws MeasurementLocatorException {

    throw new MeasurementLocatorException("Needs reviewing");

    /*
     * try { SensorsConfiguration sensorConfig = ResourceManager.getInstance()
     * .getSensorsConfiguration();
     *
     * Variable waterVar = sensorConfig.getInstrumentVariable("ASVCO₂ Water");
     * Variable atmVar =
     * sensorConfig.getInstrumentVariable("ASVCO₂ Atmosphere");
     *
     * HashMap<Long, String> waterRunTypes = new HashMap<Long, String>();
     * waterRunTypes.put(waterVar.getId(), Measurement.MEASUREMENT_RUN_TYPE);
     *
     * HashMap<Long, String> atmRunTypes = new HashMap<Long, String>();
     * atmRunTypes.put(atmVar.getId(), Measurement.MEASUREMENT_RUN_TYPE);
     *
     * DatasetSensorValues sensorValues = DataSetDataDB.getSensorValues(conn,
     * instrument, dataset.getId(), false, true);
     *
     * SensorValuesList runTypes = sensorValues.getRunTypes();
     *
     * // Loop through all the rows, examining the zero/flush columns to decide
     * // what to do List<Measurement> measurements = new
     * ArrayList<Measurement>( sensorValues.getTimes().size());
     *
     * for (LocalDateTime recordTime : sensorValues.getTimes()) {
     *
     * SensorValuesListValue runTypeValue = runTypes
     * .getValueOnOrBefore(recordTime); String runType = null == runTypeValue ?
     * null : runTypeValue.getStringValue();
     *
     * // Records from external files (i.e. SST/Salinity) will not have run //
     * types. Ignore them. if (null != runType) { if (runType.equals(WATER_MODE)
     * && instrument.hasVariable(waterVar)) { measurements .add(new
     * Measurement(dataset.getId(), recordTime, waterRunTypes)); }
     *
     * if (runType.equals(ATM_MODE) && instrument.hasVariable(atmVar)) {
     * measurements .add(new Measurement(dataset.getId(), recordTime,
     * atmRunTypes)); } } }
     *
     * return measurements; } catch (Exception e) { throw new
     * MeasurementLocatorException(e); }
     */
  }

}
