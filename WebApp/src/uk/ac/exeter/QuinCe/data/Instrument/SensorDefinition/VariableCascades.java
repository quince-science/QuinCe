package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableCascades.Cascade;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@SuppressWarnings("serial")
public class VariableCascades
  extends HashMap<SensorType, HashMap<FlagScheme, List<Cascade>>> {

  protected void add(FlagScheme flagScheme, SensorType sensorType,
    int triggerFlagValue, int outcomeFlagValue)
    throws VariableCascadeException {

    Flag triggerFlag = flagScheme.getFlag(triggerFlagValue);
    Flag outcomeFlag = flagScheme.getFlag(outcomeFlagValue);

    Cascade cascade = new Cascade(triggerFlag, outcomeFlag);

    if (!containsKey(sensorType)) {
      put(sensorType, new HashMap<FlagScheme, List<Cascade>>());
    }

    HashMap<FlagScheme, List<Cascade>> sensorCascades = get(sensorType);
    if (!sensorCascades.containsKey(flagScheme)) {
      sensorCascades.put(flagScheme, new ArrayList<Cascade>());
    }

    List<Cascade> schemeCascades = sensorCascades.get(flagScheme);

    boolean cascadeExists = schemeCascades.stream()
      .anyMatch(c -> c.trigger().equals(triggerFlag));

    if (cascadeExists) {
      throw new VariableCascadeException(sensorType, flagScheme, triggerFlag,
        "Cascade already exists");
    } else {
      schemeCascades.add(cascade);
    }
  }

  protected Flag getCascadeFlag(FlagScheme flagScheme, SensorType sensorType,
    Flag flag) {

    Flag result = null;

    if (!flagScheme.isGood(flag, true)) {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      HashMap<FlagScheme, List<Cascade>> sensorCascades = get(sensorType);

      // If the exact SensorType isn't available,
      // try its parent.
      if (null == sensorCascades) {
        sensorCascades = get(sensorConfig.getParent(sensorType));
      }

      if (null != sensorCascades) {
        List<Cascade> schemeCascades = sensorCascades.get(flagScheme);

        if (null != schemeCascades) {
          for (Cascade cascade : schemeCascades) {
            if (cascade.trigger().equals(flag)) {
              result = cascade.outcome();
              break;
            }
          }
        }
      }
    }

    return result;
  }

  record Cascade(Flag trigger, Flag outcome) {

  }
}

@SuppressWarnings("serial")
class VariableCascadeException extends Exception {

  protected VariableCascadeException(SensorType sensorType,
    FlagScheme flagScheme, Flag flag, String message) {

    super(sensorType.getShortName() + ": " + flagScheme.getName() + ": "
      + flag.getName() + ": " + message);

  }

}
