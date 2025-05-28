package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Test for the {@link UnderwayMarinePco2Reducer}.
 *
 * <p>
 * Note that this only tests the pCO₂/fCO₂ and intermediate calculations, and
 * not things like calibrating CO₂ and equilibrator pressure - these are
 * performed outside the reducer and therefore tested in the relevant places.
 * </p>
 */
public class UnderwayMarinePco2ReducerTest extends DataReducerTest {

  @FlywayTest
  @Test
  public void testReduction() throws Exception {

    // Mock objects
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getId()).thenReturn(1L);

    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(1L);

    // Initialise the reducer
    UnderwayMarinePco2Reducer reducer = new UnderwayMarinePco2Reducer(variable,
      new HashMap<String, Properties>(), null);

    MeasurementValue waterTemp = makeMeasurementValue("Water Temperature",
      11.912D);

    MeasurementValue salinity = makeMeasurementValue("Salinity", 35.224D);

    MeasurementValue eqTemp = makeMeasurementValue("Equilibrator Temperature",
      12.37D);

    MeasurementValue eqPressure = makeMeasurementValue("Equilibrator Pressure",
      999.23D);

    MeasurementValue xco2 = makeMeasurementValue("xCO₂ (with standards)",
      374.977D);

    Measurement measurement = makeMeasurement(waterTemp, salinity, eqTemp,
      eqPressure, xco2);

    // Make a record to work with
    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      reducer.getCalculationParameterNames());

    reducer.doCalculation(instrument, measurement, record,
      getDataSource().getConnection());

    // Check the calculated values in the record
    assertEquals(0.458D, record.getCalculationValue("ΔT"), 0.0001);
    assertEquals(0.01389918297D, record.getCalculationValue("pH₂O"), 0.0001);
    assertEquals(364.576695236D, record.getCalculationValue("pCO₂ TE Wet"),
      0.0001);
    assertEquals(363.233567921D, record.getCalculationValue("fCO₂ TE Wet"),
      0.0001);
    assertEquals(357.5815834D, record.getCalculationValue("pCO₂ SST"), 0.0001);
    assertEquals(356.2642266D, record.getCalculationValue("fCO₂"), 0.0001);
  }

  @FlywayTest
  @Test
  public void largeDeltaTTest()
    throws SensorTypeNotFoundException, DataReductionException, SQLException {

    // Mock objects
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getId()).thenReturn(1L);

    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(1L);

    UnderwayMarinePco2Reducer reducer = new UnderwayMarinePco2Reducer(variable,
      new HashMap<String, Properties>(), null);

    MeasurementValue waterTemp = makeMeasurementValue("Water Temperature",
      11.912D);

    MeasurementValue salinity = makeMeasurementValue("Salinity", 35.224D);

    MeasurementValue eqTemp = makeMeasurementValue("Equilibrator Temperature",
      1000D);

    MeasurementValue eqPressure = makeMeasurementValue("Equilibrator Pressure",
      999.23D);

    MeasurementValue xco2 = makeMeasurementValue("xCO₂ (with standards)",
      374.977D);

    Measurement measurement = makeMeasurement(waterTemp, salinity, eqTemp,
      eqPressure, xco2);

    // Make a record to work with
    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      reducer.getCalculationParameterNames());

    reducer.doCalculation(instrument, measurement, record,
      getDataSource().getConnection());

    // Check the calculated values in the record
    assertEquals(988.088D, record.getCalculationValue("ΔT"), 0.0001);
    assertEquals(Double.NaN, record.getCalculationValue("pH₂O"));
    assertEquals(Double.NaN, record.getCalculationValue("pCO₂ TE Wet"));
    assertEquals(Double.NaN, record.getCalculationValue("fCO₂ TE Wet"));
    assertEquals(Double.NaN, record.getCalculationValue("pCO₂ SST"));
    assertEquals(Double.NaN, record.getCalculationValue("fCO₂"));
  }
}
