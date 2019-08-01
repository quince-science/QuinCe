package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.DateColumnGroupedSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;

/**
 * Factory class for Data Reducers
 * @author Steve Jones
 *
 */
public class DataReducerFactory {

  /**
   * Get the Data Reducer for a given variable and initialise it
   * @param variable The variable
   * @return The Data Reducer
   * @throws DataReductionException If the reducer cannot be retreived
   */
  public static DataReducer getReducer(
      Connection conn, Instrument instrument, InstrumentVariable variable,
      Map<String, Float> variableAttributes, CalibrationSet calibrationSet,
      List<Measurement> allMeasurements,
      DateColumnGroupedSensorValues groupedSensorValues)
      throws DataReductionException {

    DataReducer reducer;

    try {
      switch (variable.getName()) {
      case "Underway Marine pCO₂": {
        reducer = new UnderwayMarinePco2Reducer(variable, variableAttributes, allMeasurements, groupedSensorValues, calibrationSet);
        break;
      }
      case "Underway Atmospheric pCO₂": {
        reducer = new UnderwayAtmosphericPco2Reducer(variable, variableAttributes, allMeasurements, groupedSensorValues, calibrationSet);
        break;
      }
      default: {
        throw new DataReductionException("Cannot find reducer for variable " + variable.getName());
      }
      }
    } catch (Exception e) {
      throw new DataReductionException("Cannot initialise data reducer", e);
    }

    return reducer;
  }

  private static DataReducer getSkeletonReducer(InstrumentVariable variable) throws DataReductionException {

    DataReducer reducer = null;

    switch (variable.getName()) {
    case "Underway Marine pCO₂": {
      reducer = new UnderwayMarinePco2Reducer(variable, null, null, null, null);
      break;
    }
    case "Underway Atmospheric pCO₂": {
      reducer = new UnderwayAtmosphericPco2Reducer(variable, null, null, null, null);
      break;
    }
    default: {
      throw new DataReductionException("Cannot find reducer for variable " + variable.getName());
    }
    }

    return reducer;
  }

  /**
   * Get the calculation parameters for a given data reducer with their IDs
   * @param variable The variable for the data reducer
   * @return The calculation parameter names
   * @throws DataReductionException If the variable does not have a reducer
   */
  public static LinkedHashMap<String, Long> getCalculationParameters(
    InstrumentVariable variable) throws DataReductionException {

    DataReducer reducer = getSkeletonReducer(variable);
    List<String> parameterNames = reducer.getCalculationParameterNames();

    LinkedHashMap<String, Long> result = new LinkedHashMap<String, Long>();
    int i = -1;
    for (String name : parameterNames) {
      i++;
      result.put(name, makeId(variable, i));
    }

    return result;
  }

  public static TreeMap<Long, ColumnHeader> getColumnHeaders(InstrumentVariable variable,
    ExportOption exportOption) throws DataReductionException {

    DataReducer reducer = getSkeletonReducer(variable);

    TreeMap<Long, ColumnHeader> result = new TreeMap<Long, ColumnHeader>();

    List<ColumnHeader> headers = reducer.getColumnHeaders(exportOption.includeCalculationColumns());
    int i = -1;
    for (ColumnHeader header : headers) {
      i++;
      result.put(makeId(variable, i), header);
    }

    return result;
  }

  private static long makeId(InstrumentVariable variable, int sequence) {
    return variable.getId() * 10000 + sequence;
  }
}
