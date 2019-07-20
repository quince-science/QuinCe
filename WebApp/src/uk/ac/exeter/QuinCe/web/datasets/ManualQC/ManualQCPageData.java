package uk.ac.exeter.QuinCe.web.datasets.ManualQC;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.PlotPage.Field;
import uk.ac.exeter.QuinCe.web.PlotPage.FieldSets;
import uk.ac.exeter.QuinCe.web.PlotPage.FieldValue;
import uk.ac.exeter.QuinCe.web.PlotPage.Data.PlotPageData;

public class ManualQCPageData extends PlotPageData {

  private List<String> measurementRunTypes;

  public ManualQCPageData(Instrument instrument, FieldSets fieldSets) throws Exception {
    super(instrument, fieldSets);
  }

  /**
   * Add a set of values, filtering out unwanted values. The default
   * filter removes values for columns that are internally calibrated
   * where the run type is not a measurement. This has the effect
   * of removing all values taken during internal calibration.
   *
   * Override this method to filter the supplied values according to need.
   *
   * @param runType
   * @param time
   * @param values
   * @throws RecordNotFoundException
   */
  @Override
  public void filterAndAddValuesAction(String runType, LocalDateTime time, Map<Long, FieldValue> values)
      throws RecordNotFoundException {

    // Filter out values based on run type.
    // If a value has internal calibrations, and we aren't on a
    // measurement run type, that value is removed.

    Map<Field, FieldValue> valuesToAdd = new HashMap<Field, FieldValue>();

    for (Map.Entry<Long, FieldValue> entry : values.entrySet()) {

      // We don't keep internal calibration values
      SensorType sensorType = instrument.getSensorAssignments().getSensorTypeForDBColumn(entry.getKey());
      if (!sensorType.hasInternalCalibration() || measurementRunTypes.contains(runType)) {
        valuesToAdd.put(fieldSets.getField(entry.getKey()), entry.getValue());
      }
    }

    addValues(time, valuesToAdd);
  }

  /**
   * Initialise information required for filterAndAddValues
   */
  @Override
  protected void initFilter() {
    // Get the list of run type values that indicate measurements
    measurementRunTypes = new ArrayList<String>(0);

    for (InstrumentVariable variable : instrument.getVariables()) {
      Map<Long, List<String>> variableRunTypes = instrument.getVariableRunTypes(variable);
      if (variableRunTypes.size() > 0 && variableRunTypes.containsKey(variable.getId())) {
        measurementRunTypes.addAll(variableRunTypes.get(variable.getId()));
      }
    }
  }

}
