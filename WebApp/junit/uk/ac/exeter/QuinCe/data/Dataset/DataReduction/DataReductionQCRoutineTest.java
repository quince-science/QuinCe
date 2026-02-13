package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Base class for tests that result in a {@link FlaggedItems} object that needs
 * to be checked.
 */
public class DataReductionQCRoutineTest extends BaseTest {

  /**
   * Initialise the Resource Manager.
   */
  @BeforeEach
  public void setup() throws Exception {
    initResourceManager();
  }

  /**
   * Destroy the Resource Manager.
   */
  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  /**
   * Check a {@link FlaggedItems} object to see if it is empty (i.e. there are
   * no flagged items).
   *
   * @param flaggedItems
   *          The {@link FlaggedItems} object.
   * @return {@code true} if there are no flagged items; {@code false} if there
   *         are.
   */
  protected boolean flaggedItemsEmpty(FlaggedItems flaggedItems) {
    return flaggedItems.getSensorValues().size() == 0
      && flaggedItems.getMeasurements().size() == 0
      && flaggedItems.getDataReductionRecords().size() == 0;
  }

  /**
   * Get the Data Reduction QC Routines configuration.
   *
   * @return The configuration.
   */
  protected DataReductionQCRoutinesConfiguration getDataReductionQCRoutinesConfiguration() {
    return ResourceManager.getInstance()
      .getDataReductionQCRoutinesConfiguration();
  }

  /**
   * Get a specified {@link DataReductionQCRoutine} for a specified
   * {@link DataReducer} from the system configuration.
   *
   * @param reducerClass
   *          The {@link DataReducer}.
   * @param routineClass
   *          The {@link DataReductionQCRoutine}.
   * @return The routine.
   */
  protected DataReductionQCRoutine getRoutine(
    Class<? extends DataReducer> reducerClass,
    Class<? extends DataReductionQCRoutine> routineClass) {

    return getDataReductionQCRoutinesConfiguration().getRoutines(reducerClass)
      .stream().filter(routine -> routine.getClass().equals(routineClass))
      .findAny().get();
  }
}
