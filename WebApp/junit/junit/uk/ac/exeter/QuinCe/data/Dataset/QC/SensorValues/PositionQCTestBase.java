package junit.uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.List;

import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.PositionQCRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Base class for test classes related to the {@link PositionQCRoutine}.
 *
 * <p>
 * This provides a mock {@link Instrument} for tests to use.
 * </p>
 *
 * @author Steve Jones
 *
 */
public abstract class PositionQCTestBase extends BaseTest {

  /**
   * Generate the list of data column IDs for use by the mock instrument.
   *
   * @return
   */
  protected abstract List<Long> makeDataColumnIds();

  /**
   * Create the mock {@link Instrument} object.
   *
   * @return The {@link Instrument}.
   * @throws Exception
   *           If the instrument creation fails
   */
  protected Instrument makeInstrument() throws Exception {

    // SensorAssignemnts
    SensorAssignments sensorAssignments = Mockito.mock(SensorAssignments.class);
    Mockito.when(sensorAssignments.getSensorColumnIds())
      .thenReturn(makeDataColumnIds());

    // The mock will always return the same SensorType, with
    // hasInternalCalibrations() set as required in the method call.
    SensorType sensorType = Mockito.mock(SensorType.class);

    Mockito.when(sensorAssignments.getSensorTypeForDBColumn(Mockito.anyLong()))
      .thenReturn(sensorType);

    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getSensorAssignments())
      .thenReturn(sensorAssignments);

    return instrument;
  }

  /**
   * Creates an empty list for the Run Type parameters.
   *
   * <p>
   * A list of run types is required for some parts of the test setup, but the
   * contents of the list does not have any impact on this set of tests.
   * </p>
   *
   * @return An empty list of run types.
   */
  protected SearchableSensorValuesList makeEmptyRunTypes() {
    return new SearchableSensorValuesList(0);
  }

}
