package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import java.util.Objects;

/**
 * Base class for node data types in the
 * {@link uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree.AssignmentsTree}.
 */
public abstract class AssignmentsTreeNodeData {

  /**
   * Get the node's text label.
   *
   * @return The node's label.
   */
  public abstract String getLabel();

  /**
   * Get a unique identifier for this node.
   *
   * @return The node ID.
   */
  public String getId() {
    return getClass().getName() + "_" + getLabel();
  }

  @Override
  public String toString() {
    return getLabel();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AssignmentsTreeNodeData other = (AssignmentsTreeNodeData) obj;
    return Objects.equals(getId(), other.getId());
  }
}
