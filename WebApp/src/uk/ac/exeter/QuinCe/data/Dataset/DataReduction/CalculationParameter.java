package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;

public class CalculationParameter {

  private String name;

  private ColumnHeader columnHeader;

  private boolean result;

  public CalculationParameter(String name, String columnName, String columnCode,
    String units, boolean result) {

    this.name = name;
    this.columnHeader = new ColumnHeader(columnName, columnCode, units);
    this.result = result;
  }

  public String getName() {
    return name;
  }

  public ColumnHeader getColumnHeader() {
    return columnHeader;
  }

  public boolean isResult() {
    return result;
  }

}
