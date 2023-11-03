package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class DataReducerFactoryTest extends BaseTest {

  private Variable makeVariable() {
    Variable var = Mockito.mock(Variable.class);
    Mockito.when(var.getName()).thenReturn("Underway Atmospheric pCO₂");
    return var;
  }

  private Variable makeInvalidVariable() {
    Variable var = Mockito.mock(Variable.class);
    Mockito.when(var.getName()).thenReturn("Ceci n'est pas une variable");
    return var;
  }

  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  @Test
  public void getExistingReducerTest() throws Exception {
    DataReducer reducer = DataReducerFactory.getReducer(makeVariable(), null);
    assertNotNull(reducer);
  }

  @Test
  public void getNonExistantReducerTest() throws DataReductionException {
    assertThrows(DataReductionException.class, () -> {
      DataReducerFactory.getReducer(makeInvalidVariable(), null);
    });
  }

  @Test
  public void testPropertySet() throws Exception {
    Variable var = makeVariable();
    HashMap<String, Properties> props = new HashMap<String, Properties>();
    Properties properties = new Properties();
    properties.put("Prop1", "prop1");
    props.put(var.getName(), properties);

    DataReducer reducer = DataReducerFactory.getReducer(var, props);
    assertEquals("prop1", reducer.getStringProperty("Prop1"));
  }

  @ParameterizedTest
  @CsvSource({ "true,5", "false,3" })
  public void getCalculationParametersWithCalcColumnsTest(
    boolean includeCalculationColumns, int expectedSize) throws Exception {
    List<CalculationParameter> params = DataReducerFactory
      .getCalculationParameters(makeVariable(), includeCalculationColumns);

    /*
     * We just check the number of parameters - testing the ins and outs of the
     * CalculationParameter objects happens elsewhere
     */
    assertEquals(expectedSize, params.size());
  }

  @Test
  public void getCalculationParametersMultiVarTest() throws Exception {
    Variable var1 = makeVariable();

    Variable var2 = Mockito.mock(Variable.class);
    Mockito.when(var2.getName()).thenReturn("Underway Marine pCO₂");

    List<Variable> vars = Arrays.asList(var1, var2);
    Map<Variable, List<CalculationParameter>> params = DataReducerFactory
      .getCalculationParameters(vars);

    assertEquals(5, params.get(var1).size());
    assertEquals(6, params.get(var2).size());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getVariableUsedVariableTest() throws Exception {
    initResourceManager();
    Instrument instrument = InstrumentDB.getInstrument(getConnection(), 1);

    // Variable ID = 1 (multiplied by ID_MULTIPLIER; Second parameter
    long parameterId = 10000L + 2;

    Variable variable = DataReducerFactory.getVariable(instrument, parameterId);
    assertEquals(1, variable.getId());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getVariableUnusedVariableTest() throws Exception {
    initResourceManager();
    Instrument instrument = InstrumentDB.getInstrument(getConnection(), 1);

    // Variable ID = 17 (multiplied by ID_MULTIPLIER; Second parameter
    long parameterId = 170000L + 2;

    assertThrows(InstrumentException.class, () -> {
      DataReducerFactory.getVariable(instrument, parameterId);
    });
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getVariableInvalidParameterTest() throws Exception {
    initResourceManager();
    Instrument instrument = InstrumentDB.getInstrument(getConnection(), 1);

    // ID_MULTIPLIER not applied
    long parameterId = 2;

    assertThrows(InstrumentException.class, () -> {
      DataReducerFactory.getVariable(instrument, parameterId);
    });
  }

  @Test
  public void getCalculationParameterPositiveIDTest() throws Exception {
    CalculationParameter param = DataReducerFactory
      .getVariableParameter(makeVariable(), 2);

    assertEquals("xCO₂ In Atmosphere", param.getLongName());
  }

  @Test
  public void getCalculationParameterZeroIDTest() throws Exception {
    CalculationParameter param = DataReducerFactory
      .getVariableParameter(makeVariable(), 0);

    assertEquals("Sea Level Pressure", param.getLongName());
  }

  @Test
  public void getCalculationParameterInvalidIDTest() throws Exception {
    assertThrows(DataReductionException.class, () -> {
      DataReducerFactory.getVariableParameter(makeVariable(), 20);
    });
  }

  @Test
  public void getCalculationParameterNegativeIDTest() throws Exception {
    assertThrows(DataReductionException.class, () -> {
      DataReducerFactory.getVariableParameter(makeVariable(), -12);
    });
  }

  @Test
  public void getCalculationParameterInvalidVariableTest() throws Exception {
    assertThrows(DataReductionException.class, () -> {
      DataReducerFactory.getVariableParameter(makeInvalidVariable(), 0);
    });
  }
}
