package uk.ac.exeter.QuinCe.data.Dataset.QC;

import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class RoutineFlag extends Flag {

  /**
   * The routine that generated this flag
   */
  protected final String routineName;

  /**
   * The value required by the routine
   */
  private final String requiredValue;

  /**
   * The actual value
   */
  private final String actualValue;

  public RoutineFlag(Routine routine, Flag flag, String requiredValue,
    String actualValue) {
    super(flag);
    this.routineName = routine.getName();
    this.requiredValue = requiredValue;
    this.actualValue = actualValue;
  }

  /**
   * Get a concrete instance of the Routine that generated this flag.
   *
   * @return The Routine instance.
   */
  protected Routine getRoutineInstance() throws RoutineException {

    Routine result;

    String[] routineNameParts = routineName.split("\\.");

    switch (routineNameParts[0]) {
    case "SensorValues": {
      result = ResourceManager.getInstance().getQCRoutinesConfiguration()
        .getRoutine(routineName);
      break;
    }
    case "ExternalStandards": {
      result = ResourceManager.getInstance()
        .getExternalStandardsRoutinesConfiguration().getRoutine(routineName);
      break;
    }
    case "DataReduction": {
      result = DataReductionQCRoutinesConfiguration.getRoutine(routineName);
      break;
    }
    default: {
      throw new RoutineException(
        "Cannot determine routine type " + routineNameParts[0]);
    }
    }

    return result;
  }

  public String getRoutineName() {
    return routineName;
  }

  /**
   * Get the short message for the routine attached to this flag
   *
   * @return The message
   * @throws RoutineException
   *           If the message cannot be retrieved
   */
  public String getShortMessage() throws RoutineException {
    return getRoutineInstance().getShortMessage();
  }

  /**
   * Get the short message for the routine attached to this flag
   *
   * @return The message
   * @throws RoutineException
   *           If the message cannot be retrieved
   */
  public String getLongMessage() throws RoutineException {
    return getRoutineInstance().getLongMessage(this);
  }

  public String getRequiredValue() {
    return requiredValue;
  }

  public String getActualValue() {
    return actualValue;
  }
}
