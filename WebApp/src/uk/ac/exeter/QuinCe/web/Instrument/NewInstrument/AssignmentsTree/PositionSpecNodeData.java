package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionSpecification;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.FileDefinitionBuilder;

public class PositionSpecNodeData extends AssignmentsTreeNodeData {

  private static final int INVALID_ASSIGNMENT_TYPE = -1;

  protected static final int VALUE = 0;

  protected static final int HEMISPHERE = 1;

  private final FileDefinitionBuilder file;

  private final PositionSpecification positionSpec;

  private final int assignmentType;

  private final int fixedFormat;

  protected PositionSpecNodeData(FileDefinitionBuilder file,
    PositionSpecification positionSpec, int assignmentType) {
    this.file = file;
    this.positionSpec = positionSpec;
    this.assignmentType = assignmentType;
    this.fixedFormat = PositionSpecification.NO_FORMAT;
  }

  protected PositionSpecNodeData(FileDefinitionBuilder file,
    PositionSpecification positionSpec, int assignmentType, int fixedFormat) {
    this.file = file;
    this.positionSpec = positionSpec;
    this.assignmentType = assignmentType;
    this.fixedFormat = fixedFormat;
  }

  public String getFile() {
    return file.getFileDescription();
  }

  public int getColumn() {
    int columnIndex;

    switch (assignmentType) {
    case VALUE: {
      columnIndex = positionSpec.getValueColumn();
      break;
    }
    case HEMISPHERE: {
      columnIndex = positionSpec.getHemisphereColumn();
      break;
    }
    default: {
      columnIndex = INVALID_ASSIGNMENT_TYPE;
    }
    }

    return columnIndex;
  }

  @Override
  public String getLabel() {

    String result;

    int columnIndex = getColumn();

    if (columnIndex == INVALID_ASSIGNMENT_TYPE) {
      result = "INVALID ASSIGNMENT TYPE " + assignmentType;
    } else {
      result = file.getFileDescription() + ": "
        + file.getColumnName(columnIndex);
    }

    return result;
  }

  public int getFixedFormat() {
    return fixedFormat;
  }

  @Override
  public String getId() {
    return "POSSPEC_" + getFile() + "_" + getLabel();
  }
}
