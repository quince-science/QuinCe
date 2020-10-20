package uk.ac.exeter.QuinCe.web.ui;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

public class EditableTreeNode extends DefaultTreeNode {

  /**
   * Serial version uid
   */
  private static final long serialVersionUID = 8010606720091278400L;
  private Object data;

  public EditableTreeNode(String type, Object data, TreeNode parent) {
    super(type, data, parent);
    this.data = data;
  }

  @Override
  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

}
