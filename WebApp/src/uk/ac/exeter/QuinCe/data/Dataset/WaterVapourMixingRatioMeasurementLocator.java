package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Very hacky measurement locator for Water Vapour Mixing Ratio in Harald
 * Soderman's data.
 *
 * <p>
 * This should have its own Run Type definitions, but that isn't supported yet.
 * So instead we look up the run types for the dExcess variable and make
 * measurements where there's measurements for that.
 * </p>
 *
 * <p>
 * This should eventually be fixed by Issue #2279.
 * </p>
 */
public class WaterVapourMixingRatioMeasurementLocator
  extends MeasurementLocator {

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset) throws MeasurementLocatorException {

    List<Measurement> measurements = new ArrayList<Measurement>();

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      Variable dExcessVar = sensorConfig.getInstrumentVariable("D-Excess");
      Variable var = sensorConfig
        .getInstrumentVariable("Water Vapour Mixing Ratio");

      TreeMap<LocalDateTime, String> runTypes = getRunTypes(conn, dataset,
        instrument);

      DatasetSensorValues sensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataset.getId(), false, true);

      for (LocalDateTime recordTime : sensorValues.getTimes()) {
        RunTypeCategory category = instrument
          .getRunTypeCategory(dExcessVar.getId(), runTypes.get(recordTime));
        if (category.isMeasurementType()) {
          HashMap<Long, String> runTypesMap = new HashMap<Long, String>();
          runTypesMap.put(var.getId(), Measurement.MEASUREMENT_RUN_TYPE);

          measurements
            .add(new Measurement(dataset.getId(), recordTime, runTypesMap));
        }
      }
    } catch (Exception e) {
      throw new MeasurementLocatorException(e);
    }

    return measurements;
  }
}
