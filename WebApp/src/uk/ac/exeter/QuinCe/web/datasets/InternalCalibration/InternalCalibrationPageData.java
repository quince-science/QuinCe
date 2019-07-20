package uk.ac.exeter.QuinCe.web.datasets.InternalCalibration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.FileColumn;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.PlotPage.Field;
import uk.ac.exeter.QuinCe.web.PlotPage.FieldSets;
import uk.ac.exeter.QuinCe.web.PlotPage.FieldValue;
import uk.ac.exeter.QuinCe.web.PlotPage.Data.PlotPageData;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class InternalCalibrationPageData extends PlotPageData {

  private List<String> internalCalibrationRunTypes;

  private Map<Long, FileColumn> columns;

  public InternalCalibrationPageData(Instrument instrument, FieldSets fieldSets) throws Exception {
    super(instrument, fieldSets);
  }

  @Override
  public void filterAndAddValuesAction(String runType, LocalDateTime time,
    Map<Long, FieldValue> values) throws RecordNotFoundException {

    if (internalCalibrationRunTypes.contains(runType)) {
      Map<Field, FieldValue> addValues = new HashMap<Field, FieldValue>();

      for (Map.Entry<Long, FieldValue> entry : values.entrySet()) {

        FileColumn column = columns.get(entry.getKey());
        RunTypeField destinationField = new RunTypeField(runType, column);
        addValues.put(destinationField, entry.getValue());

      }

      addValues(time, addValues);
    }
  }

  @Override
  protected void initFilter() throws Exception {

    columns = new HashMap<Long, FileColumn>();

    List<FileColumn> instrumentColumns = InstrumentDB.getSensorColumns(ResourceManager.getInstance().getDBDataSource(), instrument.getDatabaseId());
    for (FileColumn column : instrumentColumns) {
      columns.put(column.getColumnId(), column);
    }

    // Get the list of run type values that indicate measurements
    internalCalibrationRunTypes = instrument.getInternalCalibrationRunTypes();
  }
}
