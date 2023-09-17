package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ASVCO2MeasurementLocator extends MeasurementLocator {

  private static final String WATER_MODE = "ep";

  private static final String ATM_MODE = "ap";

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset) throws MeasurementLocatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      Variable waterVar = sensorConfig.getInstrumentVariable("ASVCO₂ Water");
      Variable atmVar = sensorConfig.getInstrumentVariable("ASVCO₂ Atmosphere");

      HashMap<Long, String> waterRunTypes = new HashMap<Long, String>();
      waterRunTypes.put(waterVar.getId(), Measurement.MEASUREMENT_RUN_TYPE);

      HashMap<Long, String> atmRunTypes = new HashMap<Long, String>();
      atmRunTypes.put(atmVar.getId(), Measurement.MEASUREMENT_RUN_TYPE);

      DatasetSensorValues sensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataset.getId(), false, true);

      SensorValuesList runTypes = sensorValues.getRunTypes();

      // Loop through all the rows, examining the zero/flush columns to decide
      // what to do
      List<Measurement> measurements = new ArrayList<Measurement>(
        sensorValues.getTimes().size());

      for (LocalDateTime recordTime : sensorValues.getTimes()) {

        SensorValuesListValue runTypeValue = runTypes
          .getValueOnOrBefore(recordTime);
        String runType = null == runTypeValue ? null
          : runTypeValue.getStringValue();

        // Records from external files (i.e. SST/Salinity) will not have run
        // types. Ignore them.
        if (null != runType) {
          if (runType.equals(WATER_MODE) && instrument.hasVariable(waterVar)) {
            measurements
              .add(new Measurement(dataset.getId(), recordTime, waterRunTypes));
          }

          if (runType.equals(ATM_MODE) && instrument.hasVariable(atmVar)) {
            measurements
              .add(new Measurement(dataset.getId(), recordTime, atmRunTypes));
          }
        }
      }

      return measurements;
    } catch (Exception e) {
      throw new MeasurementLocatorException(e);
    }
  }

}
