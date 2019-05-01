package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.HashMap;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;

/**
 * Factory class for Data Reducers
 * @author Steve Jones
 *
 */
public class DataReducerFactory {

  private static HashMap<String, DataReducer> reducers;
  
  static {
    reducers = new HashMap<String, DataReducer>();
    reducers.put("Underway Marine pCO₂", new UnderwayMarinePco2Reducer());
  }
  
  /**
   * Get the Data Reducer for a given variable
   * @param variable The variable
   * @return The Data Reducer
   */
  public static DataReducer getReducer(InstrumentVariable variable) {
    return reducers.get(variable.getName());
  }
}
