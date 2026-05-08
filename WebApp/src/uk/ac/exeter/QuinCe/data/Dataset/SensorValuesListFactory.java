package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public class SensorValuesListFactory {

  public static SensorValuesList makeSensorValuesList(long columnId,
    DatasetSensorValues allSensorValues, boolean forceString)
    throws RecordNotFoundException {

    switch (allSensorValues.getInstrument().getBasis()) {
    case Instrument.BASIS_TIME: {
      return new TimestampSensorValuesList(columnId, allSensorValues,
        forceString);
    }
    default: {
      return new SimpleSensorValuesList(columnId, allSensorValues, forceString);
    }
    }
  }

  public static SensorValuesList makeSensorValuesList(
    Collection<Long> columnIds, DatasetSensorValues allSensorValues,
    boolean forceString) throws RecordNotFoundException {

    switch (allSensorValues.getInstrument().getBasis()) {
    case Instrument.BASIS_TIME: {
      return new TimestampSensorValuesList(columnIds, allSensorValues,
        forceString);
    }
    default: {
      return new SimpleSensorValuesList(columnIds, allSensorValues,
        forceString);
    }
    }
  }

}
