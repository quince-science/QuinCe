package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
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
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public abstract class AbstractQCRoutinesConfiguration {

  /**
   * The QC routines
   */
  private Map<SensorType, List<AbstractAutoQCRoutine>> routines;

  /**
   * Main constructor - parses supplied config file and builds all Routine
   * objects.
   *
   * @param configFile
   *          The configuration file
   * @throws QCRoutinesConfigurationException
   *           If the configuration is invalid
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public AbstractQCRoutinesConfiguration(SensorsConfiguration sensorsConfig,
    String configFile)
    throws QCRoutinesConfigurationException, MissingParamException {

    MissingParam.checkMissing(configFile, "configFile");
    routines = new HashMap<SensorType, List<AbstractAutoQCRoutine>>();
    init(sensorsConfig, configFile);
  }

  /**
   * Add a Routine to the configuration
   *
   * @param sensorType
   *          The target sensor type
   * @param routine
   *          The routine
   * @throws RoutineException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  protected void addRoutine(SensorType sensorType, Class<?> routineClass,
    List<String> parameters) throws RoutineException, InstantiationException,
    IllegalAccessException, IllegalArgumentException, InvocationTargetException,
    NoSuchMethodException, SecurityException {

    if (!routines.containsKey(sensorType)) {
      routines.put(sensorType, new ArrayList<AbstractAutoQCRoutine>());
    }

    if (routineClass.isAssignableFrom(getRoutineSuperClass())) {
      throw new RoutineException("Routine class does not extend "
        + getRoutineSuperClass().getCanonicalName());
    }

    routines.get(sensorType).add(makeInstance(routineClass, parameters));
  }

  protected AbstractAutoQCRoutine makeInstance(Class<?> routineClass,
    List<String> parameters) throws InstantiationException,
    IllegalAccessException, IllegalArgumentException, InvocationTargetException,
    NoSuchMethodException, SecurityException, RoutineException {

    AbstractAutoQCRoutine instance = (AbstractAutoQCRoutine) routineClass
      .getDeclaredConstructor().newInstance();
    instance.setParameters(parameters);
    return instance;

  }

  protected abstract Class<? extends AbstractAutoQCRoutine> getRoutineSuperClass();

  /**
   * Initialise the configuration from the supplied file
   *
   * @param configFilename
   *          The filename
   * @throws QCRoutinesConfigurationException
   *           If the configuration is invalid
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
   * Get the full class name from a routine name
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
   * Get the shortcut name of a concrete Routine class, for storage in the
   * database.
   *
   * This is the class name without the package prefix, and with the word
   * 'Routine' stripped off the end
   *
   * @param clazz
   *          The class
   * @return The shortcut name.
   */
  public String getRoutineName(Class<? extends AbstractAutoQCRoutine> clazz) {
    return clazz.getSimpleName().replaceAll(getRoutineClassTail() + "$", "");
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
  public String getRoutineName(AbstractAutoQCRoutine routine) {
    return getRoutineClassPackage() + "." + routine.getClass().getSimpleName()
      .replaceAll(getRoutineClassTail() + "$", "");
  }

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
   * Get the root package containing QC routines.
   *
   * @return The QC routines root package.
   */
  protected String getRoutineClassRoot() {
    return "uk.ac.exeter.QuinCe.data.Dataset.QC";
  }

  /**
   * Get the name of the package in which all sensor value routine classes will
   * be.
   *
   * <p>
   * This must be a child of the package returned by
   * {@link #getRoutineClassRoot()}.
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
   * @return The standard routine class tail.
   */
  protected String getRoutineClassTail() {
    return "Routine";
  }

  /**
   * Get the QC routines for a given sensor type
   *
   * @param sensorType
   *          The sensor type
   * @return The routines to be run
   */
  public List<AbstractAutoQCRoutine> getRoutines(SensorType sensorType) {
    List<AbstractAutoQCRoutine> result = routines.get(sensorType);
    if (null == result) {
      result = new ArrayList<AbstractAutoQCRoutine>();
    }

    return result;
  }
}
