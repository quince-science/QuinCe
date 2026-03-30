package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * The configuration of QC routines to be run after data reduction.
 */
@SuppressWarnings("serial")
public class DataReductionQCRoutinesConfiguration
  extends HashMap<Class<? extends DataReducer>, List<DataReductionQCRoutine>> {

  /**
   * The name of the package in which all routine classes will be stored
   */
  private static final String ROUTINE_CLASS_ROOT = "uk.ac.exeter.QuinCe.data.Dataset.QC.";

  /**
   * The name of the package in which all data reduction routine classes will be
   * stored (child of {@link #ROUTINE_CLASS_ROOT})
   */
  private static final String ROUTINE_CLASS_PACKAGE = "DataReduction";

  /**
   * All routine class names must end with the same text
   */
  private static final String ROUTINE_CLASS_TAIL = "Routine";

  /**
   * The set of routines configured for each data reducer.
   */
  private Map<Class<? extends DataReducer>, List<DataReductionQCRoutine>> routines;

  /**
   * Initialise the configuration from the specific configuration file.
   *
   * @param sensorsConfig
   *          The application's sensors configuration.
   * @param configFile
   *          The path to the configuration file.
   * @throws MissingParamException
   *           If any required parameters are missing.
   * @throws DataReductionQCRoutinesConfigurationException
   *           If the configuration is invalid.
   */
  public DataReductionQCRoutinesConfiguration() {
  }

  /**
   * Get the routines for the specified {@link DataReducer}.
   *
   * @param reducer
   *          The reducer.
   * @return The routines for the reducer.
   */
  public List<DataReductionQCRoutine> getRoutines(
    Class<? extends DataReducer> reducerClass) {
    return routines.get(reducerClass);
  }

  /**
   * Get the shortcut name of a concrete Routine instance, for storage in the
   * database.
   *
   * This is the class name without the package prefix, and with the word
   * 'Routine' stripped off the end
   *
   * @param routine
   *          The instance
   * @return The shortcut name.
   */
  public static String getRoutineName(DataReductionQCRoutine routine) {
    return ROUTINE_CLASS_PACKAGE + "."
      + routine.getClass().getSimpleName().replaceAll("Routine$", "");
  }

  public static DataReductionQCRoutine getRoutine(String routineName)
    throws RoutineException {

    try {
      return (DataReductionQCRoutine) Class
        .forName(getFullClassName(routineName)).getDeclaredConstructor()
        .newInstance();
    } catch (Exception e) {
      throw new RoutineException(
        "Cannot get routine instance for '" + routineName + "'", e);
    }
  }

  /**
   * Get the full class name from a routine name
   *
   * @param routineName
   *          The routine name
   * @return The full class name
   */
  protected static String getFullClassName(String routineName) {
    String className = routineName;
    if (routineName.contains(".")) {
      String[] split = routineName.split("\\.");
      className = split[split.length - 1];
    }

    return ROUTINE_CLASS_ROOT + ROUTINE_CLASS_PACKAGE + "." + className
      + ROUTINE_CLASS_TAIL;
  }
}