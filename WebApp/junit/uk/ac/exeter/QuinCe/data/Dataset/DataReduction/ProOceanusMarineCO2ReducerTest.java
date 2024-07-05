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
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class ProOceanusMarineCO2ReducerTest extends DataReducerTest {

  @FlywayTest
  @Test
  public void testReduction() throws Exception {

    // Mock objects
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getId()).thenReturn(1L);

    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(1L);

    // Initialise the reducer
    ProOceanusMarineCO2Reducer reducer = new ProOceanusMarineCO2Reducer(
      variable, new HashMap<String, Properties>());

    MeasurementValue waterTemp = makeMeasurementValue("Water Temperature",
      10.777D);

    MeasurementValue cellGasPressure = makeMeasurementValue("Cell Gas Pressure",
      1014.81D);

    MeasurementValue xco2 = makeMeasurementValue("xCO₂ (wet, no standards)",
      393.722D);

    Measurement measurement = makeMeasurement(waterTemp, cellGasPressure,
      xco2);

    // Make a record to work with
    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      reducer.getCalculationParameterNames());

    reducer.doCalculation(instrument, measurement, record,
      getDataSource().getConnection());

    assertEquals(394.32817D, record.getCalculationValue("pCO₂ SST"), 0.0001);
    assertEquals(394.32669D, record.getCalculationValue("fCO₂"), 0.0001);

  }

}
