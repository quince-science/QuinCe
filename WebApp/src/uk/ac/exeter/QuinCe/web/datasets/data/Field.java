package uk.ac.exeter.QuinCe.web.datasets.data;

public class Field {

  public static final long ROWID_FIELD_ID = 0L;

  private FieldSet fieldSet;

  private long id;

  private String name;

  public Field(FieldSet fieldSet, long sensorId, String name) {
    this.fieldSet = fieldSet;
    this.id = sensorId;
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public String getFullName() {
    return name;
  }

  public String getBaseName() {
    return name;
  }

  public FieldSet getFieldSet() {
    return fieldSet;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof Field) {
      result = ((Field) o).id == id;
    } else if (o instanceof Long) {
      result = (Long) o == id;
    }

    return result;
  }
}
