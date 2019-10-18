package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Instrument.FileColumn;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.datasets.data.DatasetMeasurementData;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSet;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSets;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;
import uk.ac.exeter.QuinCe.web.datasets.data.MeasurementDataException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@SuppressWarnings("serial")
public class InternalCalibrationPageData extends DatasetMeasurementData {

  private List<String> internalCalibrationRunTypes;

  private RunTypePeriods runTypePeriods;

  private Map<Long, FileColumn> calibrationColumns;

  public InternalCalibrationPageData(Instrument instrument, FieldSets fieldSets,
    DataSet dataSet) throws Exception {
    super(instrument, fieldSets, dataSet);

    // Since we always keep all data loaded for this implementation,
    // all column fields are marked as loaded too
    for (Field field : fieldSets.getFields()) {
      loadedFields.put(field, true);
    }
  }

  @Override
  public void filterAndAddValues(String runType, LocalDateTime time,
    Map<Long, FieldValue> values)
    throws MeasurementDataException, MissingParamException {

    if (internalCalibrationRunTypes.contains(runType)) {
      Map<Field, FieldValue> valuesToAdd = new HashMap<Field, FieldValue>();

      for (Map.Entry<Long, FieldValue> entry : values.entrySet()) {

        FileColumn column = calibrationColumns.get(entry.getKey());

        // We ignore any values that aren't in the calibratable columns
        if (null != column) {
          // TODO We shouldn't need to create this all the time - keep a cache
          // somewhere
          RunTypeField destinationField = new RunTypeField(
            FieldSet.BASE_FIELD_SET, runType, column);

          valuesToAdd.put(destinationField, entry.getValue());
        }
      }

      addValues(time, valuesToAdd);
    }
  }

  @Override
  protected void initFilter() throws MeasurementDataException {

    try {
      calibrationColumns = new HashMap<Long, FileColumn>();

      // We only want the columns that have calibrations
      List<FileColumn> calibratedColumns = InstrumentDB
        .getCalibratedSensorColumns(
          ResourceManager.getInstance().getDBDataSource(),
          instrument.getDatabaseId());

      for (FileColumn column : calibratedColumns) {
        calibrationColumns.put(column.getColumnId(), column);
      }

      // Get the list of run type values that indicate measurements
      internalCalibrationRunTypes = instrument.getInternalCalibrationRunTypes();
      runTypePeriods = DataSetDataDB.getRunTypePeriods(
        ResourceManager.getInstance().getDBDataSource(), instrument, dataSet,
        internalCalibrationRunTypes);

    } catch (Exception e) {
      throw new MeasurementDataException("Error looking up insturment details",
        e);
    }
  }

  @Override
  public final void addTimes(Collection<LocalDateTime> times)
    throws MeasurementDataException {

    // Add only those times that are in the set of allowed run types (i.e. the
    // internal calibration run types)
    times.stream().filter(t -> runTypePeriods.contains(t))
      .forEach(this::addTime);

    // Load all the data
    loadRows(0, size());
  }

  @Override
  protected void load(List<LocalDateTime> times)
    throws MeasurementDataException {

    if (times.size() > 0) {
      try {
        DataSetDataDB.loadQCSensorValues(
          ResourceManager.getInstance().getDBDataSource(), this, times);
      } catch (Exception e) {
        throw new MeasurementDataException("Error loading data from database",
          e);
      }
    }
  }

  @Override
  protected void loadFieldAction(List<Field> fields)
    throws MeasurementDataException {
    // We always load all data up front, so we don't need to load individual
    // fields
  }
}
