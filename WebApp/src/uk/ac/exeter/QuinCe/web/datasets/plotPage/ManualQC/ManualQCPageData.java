package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.data.DatasetMeasurementData;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSets;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;
import uk.ac.exeter.QuinCe.web.datasets.data.MeasurementDataException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ManualQCPageData extends DatasetMeasurementData {

  protected List<String> measurementRunTypes;

  public ManualQCPageData(Instrument instrument, FieldSets fieldSets,
    DataSet dataSet) throws Exception {
    super(instrument, fieldSets, dataSet);
  }

  /**
   * Add a set of values, filtering out unwanted values. The default filter
   * removes values for columns that are internally calibrated where the run
   * type is not a measurement. This has the effect of removing all values taken
   * during internal calibration.
   *
   * Override this method to filter the supplied values according to need.
   *
   * @param runType
   * @param time
   * @param values
   * @throws MeasurementDataException
   */
  @Override
  public void filterAndAddValuesAction(String runType, LocalDateTime time,
    Map<Long, FieldValue> values) throws MeasurementDataException {

    try {
      // Filter out values based on run type.
      // If a value has internal calibrations, and we aren't on a
      // measurement run type, that value is removed.

      Map<Field, FieldValue> valuesToAdd = new HashMap<Field, FieldValue>();

      for (Map.Entry<Long, FieldValue> entry : values.entrySet()) {

        // We don't keep internal calibration values
        SensorType sensorType = instrument.getSensorAssignments()
          .getSensorTypeForDBColumn(entry.getKey());
        if (!sensorType.hasInternalCalibration()
          || measurementRunTypes.contains(runType)) {
          valuesToAdd.put(fieldSets.getField(entry.getKey()), entry.getValue());
        }
      }

      addValues(time, valuesToAdd);
    } catch (RecordNotFoundException e) {
      throw new MeasurementDataException("Failed to look up sensor type", e);
    }
  }

  /**
   * Initialise information required for filterAndAddValues
   */
  @Override
  protected void initFilter() {
    // Get the list of run type values that indicate measurements
    measurementRunTypes = instrument.getMeasurementRunTypes();
  }

  @Override
  protected void load(List<LocalDateTime> times)
    throws MeasurementDataException {

    try {
      DataSetDataDB.loadMeasurementData(
        ResourceManager.getInstance().getDBDataSource(), this, times);
    } catch (Exception e) {
      throw new MeasurementDataException("Error loading data from database", e);
    }
  }
}
