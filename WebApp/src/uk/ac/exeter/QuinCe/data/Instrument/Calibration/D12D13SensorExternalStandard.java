package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.UnderwayMarine12_13Pco2Reducer;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.ParameterException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class D12D13SensorExternalStandard extends DefaultExternalStandard {

  boolean useTotal = false;

  public D12D13SensorExternalStandard(Instrument instrument, long id,
    LocalDateTime date) throws CalibrationException {
    super(instrument, id, date);
    calculateUseTotal(instrument);
  }

  public D12D13SensorExternalStandard(long id, Instrument instrument,
    String target, LocalDateTime deploymentDate,
    Map<String, String> coefficients)
    throws ParameterException, CalibrationException {
    super(id, instrument, target, deploymentDate, coefficients);
    calculateUseTotal(instrument);
  }

  private void calculateUseTotal(Instrument instrument)
    throws CalibrationException {

    Map<Variable, Properties> props = instrument.getAllVariableProperties();

    String marineProp = getCalGasProperty(props,
      "Underway Marine pCO₂ from ¹²CO₂/¹³CO₂");
    String atmosProp = getCalGasProperty(props,
      "Underway Atmospheric pCO₂ from ¹²CO₂/¹³CO₂");

    if (null != marineProp && null != atmosProp
      && !marineProp.equals(atmosProp)) {
      throw new CalibrationException(
        UnderwayMarine12_13Pco2Reducer.CAL_GAS_TYPE_ATTR
          + " property must be the same for both marine and atmospheric variables");
    } else if (null != marineProp) {
      useTotal = marineProp
        .equals(UnderwayMarine12_13Pco2Reducer.TOTAL_CO2_GAS_CAL_TYPE);
    } else if (null != atmosProp) {
      useTotal = atmosProp
        .equals(UnderwayMarine12_13Pco2Reducer.TOTAL_CO2_GAS_CAL_TYPE);
    } else {
      throw new CalibrationException("Missing "
        + UnderwayMarine12_13Pco2Reducer.CAL_GAS_TYPE_ATTR + " property");
    }
  }

  private String getCalGasProperty(Map<Variable, Properties> props,
    String variableName) throws CalibrationException {

    String result = null;

    try {
      Properties marineProps = props.get(ResourceManager.getInstance()
        .getSensorsConfiguration().getInstrumentVariable(variableName));

      if (null != marineProps) {
        result = marineProps
          .getProperty(UnderwayMarine12_13Pco2Reducer.CAL_GAS_TYPE_ATTR);
      }
    } catch (VariableNotFoundException e) {
      throw new CalibrationException(e);
    }

    return result;
  }

  @Override
  protected List<String> getHiddenSensorTypes() {

    ArrayList<String> result = new ArrayList<String>(
      super.getHiddenSensorTypes());

    if (useTotal) {
      result.add("x¹²CO₂ (with standards)");
      result.add("x¹³CO₂ (with standards)");
    } else {
      result.add("x¹²CO₂ + x¹³CO₂ (with standards)");
    }

    return result;
  }
}
