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

public class ProOceanusAtmosphericCO2ReducerTest extends DataReducerTest {

  @FlywayTest
  @Test
  public void testReduction() throws Exception {

    // Mock objects
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getId()).thenReturn(1L);

    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(1L);

    // Initialise the reducer
    ProOceanusAtmosphericCO2Reducer reducer = new ProOceanusAtmosphericCO2Reducer(
      variable, new HashMap<String, Properties>());

    MeasurementValue airTemp = makeMeasurementValue("Air Temperature", 14.755D);

    MeasurementValue cellGasPressure = makeMeasurementValue("Cell Gas Pressure",
      1022.55D);

    MeasurementValue humidityPressure = makeMeasurementValue(
      "Humidity Pressure", 14.67D);

    MeasurementValue xco2 = makeMeasurementValue("xCO₂ (wet, no standards)",
      398.419D);

    Measurement measurement = makeMeasurement(airTemp, cellGasPressure,
      humidityPressure, xco2);

    // Make a record to work with
    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      reducer.getCalculationParameterNames());

    reducer.doCalculation(instrument, measurement, record,
      getDataSource().getConnection());

    assertEquals(404.21811D, record.getCalculationValue("xCO₂"), 0.0001);
    assertEquals(402.07584D, record.getCalculationValue("pCO₂"), 0.0001);
    assertEquals(402.07439D, record.getCalculationValue("fCO₂"), 0.0001);
  }
}
