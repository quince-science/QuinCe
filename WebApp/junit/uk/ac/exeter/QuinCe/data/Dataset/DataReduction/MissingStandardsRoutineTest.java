package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.MissingStandardsRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionQCJob;

@TestInstance(Lifecycle.PER_CLASS)
public class MissingStandardsRoutineTest extends DataReductionQCRoutineTest {

  private Instrument getInstrument() throws Exception {
    return InstrumentDB.getInstrument(getConnection(), 1);
  }

  private DataSet getDataSet() throws Exception {
    return DataSetDB.getDataSet(getConnection(), 1L);
  }

  private DatasetSensorValues getSensorValues(Instrument instrument)
    throws Exception {
    return DataSetDataDB.getSensorValues(getConnection(), instrument, 1L, false,
      false);
  }

  private Map<Long, Map<Variable, ReadOnlyDataReductionRecord>> getDataReduction(
    Instrument instrument, DataSet dataSet) throws Exception {
    return DataSetDataDB.getDataReductionData(getConnection(), instrument,
      dataSet);
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset",
    "resources/sql/data/DataSet/DataReduction/MissingStandardsRoutineTest" })
  @Test
  public void noMissingStandardsTest() throws Exception {

    Instrument instrument = getInstrument();
    DataSet dataset = getDataSet();
    DatasetSensorValues sensorValues = getSensorValues(instrument);
    List<Measurement> measurements = DataSetDataDB
      .getMeasurements(getConnection(), 1L);
    Map<Long, Map<Variable, ReadOnlyDataReductionRecord>> dataReduction = getDataReduction(
      instrument, dataset);

    TreeMap<Measurement, ReadOnlyDataReductionRecord> variableRecords = DataReductionQCJob
      .makeVariableRecords(measurements, instrument.getVariable(1L),
        dataReduction);

    DataReductionQCRoutine routine = getRoutine(UnderwayMarinePco2Reducer.class,
      MissingStandardsRoutine.class);

    FlaggedItems flaggedItems = new FlaggedItems();

    routine.qc(getConnection(), instrument,
      DataSetDB.getDataSet(getConnection(), 1L), instrument.getVariable(1L),
      variableRecords, sensorValues, flaggedItems);

    assertTrue(flaggedItemsEmpty(flaggedItems));
  }
}
