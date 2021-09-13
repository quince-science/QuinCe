package junit.uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.List;

import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public abstract class PositionQCTestBase extends BaseTest {

  protected abstract List<Long> makeDataColumnIds();

  protected Instrument makeInstrument() throws RecordNotFoundException {

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

  protected SearchableSensorValuesList makeEmptyRunTypes() {
    return new SearchableSensorValuesList(0);
  }

}
