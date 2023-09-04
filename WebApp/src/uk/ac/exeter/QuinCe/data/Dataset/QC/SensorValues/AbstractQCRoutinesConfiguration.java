package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.MissingParam;

/**
 * Base class for QC Routine configurations.
 *
 * <p>
 * QC Routines run at different stages of the data processing pipeline are
 * configured slightly differently, but they all need similar functionality at
 * their core. This class provides those common functions as a parent class of
 * the specific instances.
 * </p>
 *
 * <p>
 * Routines are configured in a CSV file with the columns listed below. The
 * column names must be included as the first line of the file.
 * </p>
 *
 * <p>
 * The same routine can be configured multiple times for different
 * {@link SensorType}s, which may need the same type of check but with different
 * limits (a range check is the obvious example).
 * </p>
 *
 * <table>
 * <caption>QC Routine configuration file columns</caption>
 * <tr>
 * <th>Column Name</th>
 * <th>Purpose</th>
 * </tr>
 * <tr>
 * <td>Class</td>
 * <td>The name of the routine, which is also the root of the
 * {@link AbstractAutoQCRoutine} class name for the routine. The
 * {@link #getRoutine(String)} method is responsible for retrieving concrete
 * routine instances from the name.</td>
 * </tr>
 * <tr>
 * <td>Sensor Type</td>
 * <td>The {@link SensorType} to which this routine will be applied.</td>
 * </tr>
 * <tr>
 * <td>Option...</td>
 * <td>The options for the routine. The number and type of options will vary
 * between Routines, so the validation of these options is left to the specific
 * Routine's {@link AbstractAutoQCRoutine#validateParameters()} method.</td>
 * </tr>
 * </table>
 */
public abstract class AbstractQCRoutinesConfiguration {

  /**
   * The configured QC routines grouped by their target {@link SensorType}s.
   */
  private Map<SensorType, List<AbstractAutoQCRoutine>> routines;

  /**
   * Build the configuration from a configuration file.
   *
   * @param sensorsConfig
   *          The sensor configuration for the
   *          {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument} whose
   *          values are being processed.
   * @param configFile
   *          The path of the configuration file.
   * @throws QCRoutinesConfigurationException
   *           If the configuration is invalid.
   */
  public AbstractQCRoutinesConfiguration(SensorsConfiguration sensorsConfig,
    String configFile) throws QCRoutinesConfigurationException {

    MissingParam.checkMissing(configFile, "configFile");
    routines = new HashMap<SensorType, List<AbstractAutoQCRoutine>>();
    init(sensorsConfig, configFile);
  }

  /**
   * Add a routine to the configuration.
   *
   * @param sensorType
   *          The routine's target {@link SensorType}. It will be run on all
   *          values for sensors of that type.
   * @param routineClass
   *          The routine's Java class.
   * @param parameters
   *          The routine parameters (specified in the Option columns in the
   *          configuration file).
   * @throws Exception
   *           If the {@code routineClass} is not of the correct type, or it
   *           cannot be instantiated.
   * @see #makeInstance(Class, List)
   */
  private void addRoutine(SensorType sensorType, Class<?> routineClass,
    List<String> parameters) throws Exception {

    if (!routines.containsKey(sensorType)) {
      routines.put(sensorType, new ArrayList<AbstractAutoQCRoutine>());
    }

    if (routineClass.isAssignableFrom(getRoutineSuperClass())) {
      throw new RoutineException("Routine class does not extend "
        + getRoutineSuperClass().getCanonicalName());
    }

    routines.get(sensorType).add(makeInstance(routineClass, parameters));
  }

  /**
   * Create a concrete instance of an Auto QC Routine.
   *
   * @param routineClass
   *          The routine class.
   * @param parameters
   *          The parameters for the routine (specified in the Option columns in
   *          the configuration file).
   * @return The routine instance.
   * @throws Exception
   *           If the routine class cannot be instantiated, or the supplied
   *           parameters are invalid.
   * @see Constructor#newInstance
   * @see AbstractAutoQCRoutine#validateParameters()
   */
  private AbstractAutoQCRoutine makeInstance(Class<?> routineClass,
    List<String> parameters) throws Exception {

    AbstractAutoQCRoutine instance = (AbstractAutoQCRoutine) routineClass
      .getDeclaredConstructor().newInstance();
    instance.setParameters(parameters);
    return instance;

  }

  /**
   * Get the super-class from which a specific Auto QC Routine must be
   * inherited.
   *
   * <p>
   * Each Auto QC Routine type has a specific super-class providing
   * functionality specific to that type, so all routines of that type must
   * inherit from that class. This method provides the super-class so the
   * necessary checks can be made when configuring the routines.
   * </p>
   *
   * @return The super-class for an Auto QC Routine.
   */
  protected abstract Class<? extends AbstractAutoQCRoutine> getRoutineSuperClass();

  /**
   * Initialise the QC routines from a supplied configuration file.
   *
   * @param sensorsConfig
   *          The sensor configuration for the
   *          {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument} whose
   *          values are being processed.
   * @param configFilename
   *          The path to the confiugration file.
   * @throws QCRoutinesConfigurationException
   *           If the configuration is invalid.
   */
  private void init(SensorsConfiguration sensorsConfig, String configFilename)
    throws QCRoutinesConfigurationException {

    try (Reader in = new FileReader(configFilename)) {
      Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);

      for (CSVRecord record : records) {
        String routineName = record.get("Class");
        String sensorTypeName = record.get("Sensor Type");
        List<String> parameters = new ArrayList<String>(record.size() - 2);
        for (int i = 2; i < record.size(); i++) {
          parameters.add(record.get(i));
        }

        String fullClassName = getFullClassName(routineName);

        if (routineName.equalsIgnoreCase("")) {
          throw new QCRoutinesConfigurationException(configFilename,
            record.getRecordNumber(), "Routine class name cannot be empty");
        } else if (sensorTypeName.equalsIgnoreCase("")) {
          throw new QCRoutinesConfigurationException(configFilename,
            record.getRecordNumber(), "Sensor Type name cannot be empty");
        } else {
          try {
            SensorType sensorType = sensorsConfig.getSensorType(sensorTypeName);
            addRoutine(sensorType, Class.forName(fullClassName), parameters);

          } catch (SensorTypeNotFoundException e) {
            throw new QCRoutinesConfigurationException(configFilename,
              record.getRecordNumber(),
              "Sensor Type '" + sensorTypeName + "' does not exist");
          } catch (ClassNotFoundException e) {
            throw new QCRoutinesConfigurationException(configFilename,
              record.getRecordNumber(),
              "Routine check class '" + fullClassName + "' does not exist");
          } catch (Exception e) {
            throw new QCRoutinesConfigurationException(configFilename,
              record.getRecordNumber(), "Error creating Routine check class",
              e);
          }
        }
      }
    } catch (IOException e) {
      throw new QCRoutinesConfigurationException(configFilename,
        "I/O Error while reading file", e);
    }
  }

  /**
   * Get the full class name from a routine name.
   *
   * @param routineName
   *          The routine name
   * @return The full class name
   */
  private String getFullClassName(String routineName) {

    String className = routineName;
    if (routineName.contains(".")) {
      String[] split = routineName.split("\\.");
      className = split[split.length - 1];
    }

    return getRoutineClassRoot() + "." + getRoutineClassPackage() + "."
      + className + getRoutineClassTail();
  }

  /**
   * Get the short name of a Routine from a {@link Class} object for storage in
   * the database.
   *
   * <p>
   * This is the class name without the package prefix, and with the word
   * 'Routine' stripped off the end. It will match the name stored in the
   * "Class" column of the configuration file.
   * </p>
   *
   * @param clazz
   *          The class
   * @return The shortcut name.
   */
  public String getRoutineName(Class<? extends AbstractAutoQCRoutine> clazz) {
    return clazz.getSimpleName().replaceAll(getRoutineClassTail() + "$", "");
  }

  /**
   * Get the short name of a Routine from a concrete instance object for storage
   * in the database.
   *
   * <p>
   * This is the object's class name without the package prefix, and with the
   * word 'Routine' stripped off the end. It will match the name stored in the
   * "Class" column of the configuration file.
   * </p>
   *
   * @param routine
   *          The concrete routine instance.
   * @return The short routine name.
   */
  public String getRoutineName(AbstractAutoQCRoutine routine) {
    return getRoutineClassPackage() + "." + routine.getClass().getSimpleName()
      .replaceAll(getRoutineClassTail() + "$", "");
  }

  /**
   * Get a concrete instance of a Routine given its short name (as recorded in
   * the configuration file).
   *
   * @param routineName
   *          The routine's short name.
   * @return The routine instance.
   * @throws RoutineException
   *           If the routine cannot be instantiated.
   */
  public AbstractAutoQCRoutine getRoutine(String routineName)
    throws RoutineException {

    try {
      return (AbstractAutoQCRoutine) Class
        .forName(getFullClassName(routineName)).getDeclaredConstructor()
        .newInstance();
    } catch (Exception e) {
      throw new RoutineException(
        "Cannot get routine instance for '" + routineName + "'", e);
    }
  }

  /**
   * Get the name of the Java package containing all Auto QC Routines.
   *
   * <p>
   * This package contains one sub-package for each type of Auto QC Routine.
   * </p>
   *
   * @return The QC routines root package.
   */
  protected String getRoutineClassRoot() {
    return "uk.ac.exeter.QuinCe.data.Dataset.QC";
  }

  /**
   * Get the name of the package that contains the Auto QC Routines of a given
   * type.
   *
   * <p>
   * This must be a child of the package returned by
   * {@link #getRoutineClassRoot()}. It must be the name of the package only,
   * and not its full path.
   * </p>
   *
   * @return The name of the package containing QC routines.
   */
  protected abstract String getRoutineClassPackage();

  /**
   * Get the standard tail of the class name for each routine.
   *
   * <p>
   * Each QC routine class must be named {@code <routine_name><tail>}. This
   * method returns the tail.
   * </p>
   *
   * @return The standard routine class name tail.
   */
  protected String getRoutineClassTail() {
    return "Routine";
  }

  /**
   * Get the QC Routines registered for a given {@link SensorType}.
   *
   * @param sensorType
   *          The required Sensor Type.
   * @return The Auto QC Routines.
   */
  public List<AbstractAutoQCRoutine> getRoutines(SensorType sensorType) {
    List<AbstractAutoQCRoutine> result = routines.get(sensorType);
    if (null == result) {
      result = new ArrayList<AbstractAutoQCRoutine>();
    }

    return result;
  }
}
