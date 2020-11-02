package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.MeasurementValues;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.UnderwayMarinePco2Reducer;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Test for the {@link UnderwayMarinePco2Reducer}.
 *
 * <p>
 * Note that this only tests the pCO₂/fCO₂ and intermediate calculations, and
 * not things like calibrating CO₂ and equilibrator pressure - these are
 * performed outside the reducer and therefore tested in the relevant places.
 * </p>
 *
 */
public class UnderwayMarinePco2ReducerTest extends BaseTest {

  @Test
  public void testReduction() throws Exception {

    // Mock objects
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getDatabaseId()).thenReturn(1L);

    Measurement measurement = Mockito.mock(Measurement.class);
    Mockito.when(measurement.getId()).thenReturn(1L);

    Variable variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getId()).thenReturn(1L);

    // Initialise the reducer
    UnderwayMarinePco2Reducer reducer = new UnderwayMarinePco2Reducer(variable,
      new HashMap<String, Properties>());

    // Make a record to work with
    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      reducer.getCalculationParameterNames());

    Map<String, Double> values = new HashMap<String, Double>();
    values.put("Intake Temperature", 11.912D);
    values.put("Salinity", 35.224D);
    values.put("Equilibrator Temperature", 12.37D);
    values.put("Equilibrator Pressure", 999.23D);
    values.put("xCO₂ (with standards)", 374.977D);

    // Mock object to give sensor values to the reducer
    MeasurementValues sensorValues = new FixedMeasurementValues(instrument,
      measurement, "Test", values);

    reducer.doCalculation(instrument, sensorValues, record, null, null, null);

    // Check the calculated values in the record
    assertEquals(999.23D, record.getCalculationValue("Equilibrator Pressure"),
      0.0001);
    assertEquals(0.458D, record.getCalculationValue("ΔT"), 0.0001);
    assertEquals(0.01389918297D, record.getCalculationValue("pH₂O"), 0.0001);
    assertEquals(374.977D, record.getCalculationValue("Calibrated CO₂"),
      0.0001);
    assertEquals(364.576695236D, record.getCalculationValue("pCO₂ TE Wet"),
      0.0001);
    assertEquals(363.233567921D, record.getCalculationValue("fCO₂ TE Wet"),
      0.0001);
    assertEquals(357.5815834D, record.getCalculationValue("pCO₂ SST"), 0.0001);
    assertEquals(356.2642266D, record.getCalculationValue("fCO₂"), 0.0001);
  }
}
