package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Properties;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Test for the {@link NoReductionReducer}.
 */
public class NoReductionReducerTest extends DataReducerTest {

  @FlywayTest
  @Test
  public void testReduction() throws Exception {

    initResourceManager();

    SensorType coreSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Water Temperature");

    // Mock objects
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getId()).thenReturn(1L);

    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(1L);
    Mockito.when(variable.getCoreSensorType()).thenReturn(coreSensorType);

    NoReductionReducer reducer = new NoReductionReducer(variable,
      new HashMap<String, Properties>());

    MeasurementValue waterTemp = makeMeasurementValue("Water Temperature",
      9.889D);

    Measurement measurement = makeMeasurement(waterTemp);

    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      reducer.getCalculationParameterNames());

    reducer.doCalculation(instrument, measurement, record,
      getDataSource().getConnection());

    assertEquals(0, reducer.getCalculationParameterNames().size());
  }
}
