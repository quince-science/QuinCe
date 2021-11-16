package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroups;

/**
 * Holds information regarding time offsets between sensor groups.
 *
 * @author Steve Jones
 *
 */
public class SensorOffsets {

  /**
   * The offsets defined for each sensor group pair.
   */
  private Map<SensorGroupPair, List<SensorOffset>> offsets;

  public SensorOffsets(SensorGroups sensorGroups) {
    sensorGroups.getGroupPairs()
      .forEach(p -> offsets.put(p, new ArrayList<SensorOffset>()));
  }

  public List<SensorOffset> getOffsets(SensorGroupPair sensorGroupPair) {
    return offsets.get(sensorGroupPair);
  }

}
