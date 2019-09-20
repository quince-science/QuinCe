package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
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

public class InternalCalibrationPageData extends DatasetMeasurementData {

  private List<String> internalCalibrationRunTypes;

  private Map<Long, FileColumn> columns;

  public InternalCalibrationPageData(Instrument instrument, FieldSets fieldSets,
    DataSet dataSet) throws Exception {
    super(instrument, fieldSets, dataSet);
  }

  @Override
  public void filterAndAddValuesAction(String runType, LocalDateTime time,
    Map<Long, FieldValue> values)
    throws MeasurementDataException, MissingParamException {

    if (internalCalibrationRunTypes.contains(runType)) {
      Map<Field, FieldValue> addValues = new HashMap<Field, FieldValue>();

      for (Map.Entry<Long, FieldValue> entry : values.entrySet()) {

        FileColumn column = columns.get(entry.getKey());
        RunTypeField destinationField = new RunTypeField(
          FieldSet.BASE_FIELD_SET, runType, column);
        addValues.put(destinationField, entry.getValue());

      }

      addValues(time, addValues);
    }
  }

  @Override
  protected void initFilter() throws MeasurementDataException {

    try {
      columns = new HashMap<Long, FileColumn>();

      List<FileColumn> instrumentColumns = InstrumentDB.getSensorColumns(
        ResourceManager.getInstance().getDBDataSource(),
        instrument.getDatabaseId());
      for (FileColumn column : instrumentColumns) {
        columns.put(column.getColumnId(), column);
      }

      // Get the list of run type values that indicate measurements
      internalCalibrationRunTypes = instrument.getInternalCalibrationRunTypes();
    } catch (Exception e) {
      throw new MeasurementDataException("Error looking up insturment details",
        e);
    }
  }

  @Override
  protected void load(List<LocalDateTime> times)
    throws MeasurementDataException {
    // TODO Auto-generated method stub

  }

  @Override
  protected void loadFieldAction(List<Field> fields)
    throws MeasurementDataException {
    // TODO Auto-generated method stub

  }
}
