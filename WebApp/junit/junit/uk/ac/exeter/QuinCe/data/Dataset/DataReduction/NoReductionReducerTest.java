package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.NoReductionReducer;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Test for the dummy {@link NoReductionReducer}.
 */
public class NoReductionReducerTest extends DataReducerTest {

  @FlywayTest
  @Test
  public void testReduction() throws Exception {

    initResourceManager();

    SensorType coreSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Intake Temperature");

    // Mock objects
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getId()).thenReturn(1L);

    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(1L);
    Mockito.when(variable.getCoreSensorType()).thenReturn(coreSensorType);

    NoReductionReducer reducer = new NoReductionReducer(variable,
      new HashMap<String, Properties>());

    MeasurementValue intakeTemp = makeMeasurementValue("Intake Temperature",
      9.889D);

    Measurement measurement = makeMeasurement(intakeTemp);

    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      reducer.getCalculationParameterNames());

    reducer.doCalculation(instrument, measurement, record,
      getDataSource().getConnection());

    assertEquals(9.889D, record.getCalculationValue("Intake Temperature"),
      0.0001);

    List<String> paramNames = Arrays.asList("Intake Temperature");
    assertTrue(listsEqual(paramNames, reducer.getCalculationParameterNames()));
  }
}
