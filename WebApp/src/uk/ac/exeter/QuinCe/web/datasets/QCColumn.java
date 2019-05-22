package uk.ac.exeter.QuinCe.web.datasets;

/**
 * Represents a column in the QC table
 * @author Steve Jones
 *
 */
public class QCColumn {

  private final long id;

  private final String name;

  private final long fieldSet;

  protected QCColumn(long id, String name, long fieldSet) {
    this.id = id;
    this.name = name;
    this.fieldSet = fieldSet;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public long getFieldSet() {
    return fieldSet;
  }
}
