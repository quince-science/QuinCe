package uk.ac.exeter.QuinCe.web.datasets.export;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.data.DatasetMeasurementData;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSet;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSets;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;

public class ExportData extends DatasetMeasurementData {

  public ExportData(DataSource dataSource, Instrument instrument, ExportOption exportOption)
    throws Exception {

    super(instrument, generateFieldSets(dataSource, instrument, exportOption));
  }

  private static FieldSets generateFieldSets(
    DataSource dataSource, Instrument instrument, ExportOption exportOption)
      throws Exception {

    ColumnHeader dateTimeHeader = new ColumnHeader("Date/Time", "DTUT8601", null);
    FieldSets fieldSets = new FieldSets(new ExportField(dateTimeHeader, exportOption));

    ColumnHeader lonHeader = new ColumnHeader("Longitude", "ALONGP01", "degrees_east");
    fieldSets.addField(FieldSet.BASE_FIELD_SET, new ExportField(lonHeader, exportOption));

    ColumnHeader latHeader = new ColumnHeader("Longitude", "ALATGP01", "degrees_north");
    fieldSets.addField(FieldSet.BASE_FIELD_SET, new ExportField(latHeader, exportOption));

    // Depth is fixed for now. Will fix this when variable parameter support is fixed
    // (Issue #1284)
    ColumnHeader depthHeader = new ColumnHeader("Depth", "ADEPZZ01", "m");
    fieldSets.addField(FieldSet.BASE_FIELD_SET, new ExportField(depthHeader, exportOption));

    // Sensors
    SensorAssignments sensors = instrument.getSensorAssignments();
    List<InstrumentVariable> variables = instrument.getVariables();

    // For sensors, the fields are each sensor type.
    Set<SensorType> exportSensorTypes = new HashSet<SensorType>();

    if (exportOption.includeAllSensors()) {
      for (Map.Entry<SensorType, List<SensorAssignment>> entry : sensors.entrySet()) {
        if (entry.getValue().size() > 0) {
          exportSensorTypes.add(entry.getKey());
        }
      }
    } else {
      // Only use sensor types required by the instrument's variables
      for (InstrumentVariable variable : variables) {
        exportSensorTypes.addAll(variable.getAllSensorTypes());
      }
    }

    FieldSet sensorsFieldSet = fieldSets.addFieldSet(DataSetDataDB.SENSORS_FIELDSET,
      DataSetDataDB.SENSORS_FIELDSET_NAME);

    for (SensorType sensorType : exportSensorTypes) {
      fieldSets.addField(sensorsFieldSet,
        new ExportField(sensorType.getColumnHeader(), exportOption));
    }

    // Now the fields for each variable
    for (InstrumentVariable variable : variables) {

      FieldSet varFieldSet = fieldSets.addFieldSet(variable.getId(), variable.getName());
      List<ColumnHeader> variableHeaders = DataReducerFactory.getColumnHeaders(variable, exportOption);
      for (ColumnHeader header : variableHeaders) {
        fieldSets.addField(varFieldSet, new ExportField(header, exportOption));
      }
    }

    return fieldSets;
  }

  @Override
  protected void filterAndAddValuesAction(String runType, LocalDateTime time,
    Map<Long, FieldValue> values) throws RecordNotFoundException {
    // TODO Auto-generated method stub

  }

  @Override
  protected void initFilter() throws Exception {
    // TODO Auto-generated method stub

  }
}
