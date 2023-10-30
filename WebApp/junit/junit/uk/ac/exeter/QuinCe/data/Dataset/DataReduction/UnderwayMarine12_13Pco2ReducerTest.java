package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.UnderwayMarine12_13Pco2Reducer;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class UnderwayMarine12_13Pco2ReducerTest extends DataReducerTest {

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  @FlywayTest
  @Test
  public void testSplitReduction() throws Exception {
    List<MeasurementValue> co2MeasurementValues = new ArrayList<MeasurementValue>();
    co2MeasurementValues
      .add(makeMeasurementValue("x¹²CO₂ (with standards)", 395.96D));

    co2MeasurementValues
      .add(makeMeasurementValue("x¹³CO₂ (with standards)", 3.314D));

    runTest(UnderwayMarine12_13Pco2Reducer.SPLIT_CO2_GAS_CAL_TYPE,
      co2MeasurementValues);
  }

  @FlywayTest
  @Test
  public void testTotalReduction() throws Exception {
    List<MeasurementValue> co2MeasurementValues = new ArrayList<MeasurementValue>();
    co2MeasurementValues
      .add(makeMeasurementValue("x¹²CO₂ + x¹³CO₂ (with standards)", 399.274D));

    runTest(UnderwayMarine12_13Pco2Reducer.TOTAL_CO2_GAS_CAL_TYPE,
      co2MeasurementValues);
  }

  private void runTest(String calType,
    List<MeasurementValue> co2MeasurementValues) throws Exception {

    Properties varProps = new Properties();
    varProps.put(UnderwayMarine12_13Pco2Reducer.CAL_GAS_TYPE_ATTR, calType);
    HashMap<String, Properties> props = new HashMap<String, Properties>();
    props.put("Underway Marine pCO₂ from ¹²CO₂/¹³CO₂", varProps);

    // Mock objects
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getId()).thenReturn(1L);

    Variable variable = ResourceManager.getInstance().getSensorsConfiguration()
      .getInstrumentVariable("Underway Marine pCO₂ from ¹²CO₂/¹³CO₂");

    // Initialise the reducer
    UnderwayMarine12_13Pco2Reducer reducer = new UnderwayMarine12_13Pco2Reducer(
      variable, props);

    List<MeasurementValue> allMeasurementValues = new ArrayList<MeasurementValue>();
    allMeasurementValues
      .add(makeMeasurementValue("Intake Temperature", 6.061D));
    allMeasurementValues.add(makeMeasurementValue("Salinity", 34.441D));
    allMeasurementValues
      .add(makeMeasurementValue("Equilibrator Temperature", 7.513D));
    allMeasurementValues
      .add(makeMeasurementValue("Equilibrator Pressure", 1020.33D));

    allMeasurementValues.addAll(co2MeasurementValues);

    Measurement measurement = makeMeasurement(
      allMeasurementValues.toArray(MeasurementValue[]::new));

    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      reducer.getCalculationParameterNames());

    reducer.doCalculation(instrument, measurement, record,
      getDataSource().getConnection());

    assertEquals(1.452D, record.getCalculationValue("ΔT"), 0.0001);
    assertEquals(0.01D, record.getCalculationValue("pH₂O"), 0.0001);
    assertEquals(398.0550D, record.getCalculationValue("pCO₂ TE Wet"), 0.0001);
    assertEquals(396.4604D, record.getCalculationValue("fCO₂ TE Wet"), 0.0001);
    assertEquals(374.3423D, record.getCalculationValue("pCO₂ SST"), 0.0001);
    assertEquals(372.8427D, record.getCalculationValue("fCO₂"), 0.0001);
  }
}
