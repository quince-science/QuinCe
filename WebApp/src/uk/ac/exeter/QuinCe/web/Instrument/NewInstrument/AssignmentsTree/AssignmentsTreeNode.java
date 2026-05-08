package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

@SuppressWarnings("serial")
public class AssignmentsTreeNode<T extends AssignmentsTreeNodeData>
  extends DefaultTreeNode<T> {

  protected AssignmentsTreeNode(AssignmentsTree tree, String type, T data,
    TreeNode<T> parent) {

    super(type, data, parent);
    setExpanded(tree.getExpandState(data));
  }

  protected AssignmentsTreeNode(AssignmentsTree tree, T data,
    TreeNode<T> parent) {

    super(data, parent);
    setExpanded(tree.getExpandState(data));
  }
}
