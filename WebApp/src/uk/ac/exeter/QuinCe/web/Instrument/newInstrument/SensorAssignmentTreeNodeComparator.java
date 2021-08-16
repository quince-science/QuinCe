package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.Comparator;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;

public class SensorAssignmentTreeNodeComparator
  implements Comparator<SensorAssignmentTreeNode> {

  @Override
  public int compare(SensorAssignmentTreeNode o1, SensorAssignmentTreeNode o2) {

    return ((SensorAssignment) o1.getData())
      .compareTo((SensorAssignment) o2.getData());
  }

}
