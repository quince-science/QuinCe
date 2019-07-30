package uk.ac.exeter.QuinCe.web.datasets.data;

public class FieldSet implements Comparable<FieldSet> {

  private long id;

  private String name;

  public static final long BASE_ID = 0L;

  public static final String BASE_NAME = "Base";

  public static final FieldSet BASE_FIELD_SET;

  static {
    BASE_FIELD_SET = new FieldSet(BASE_ID, BASE_NAME);
  }

  public FieldSet(long id, String name) {
    this.id = id;
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public int compareTo(FieldSet o) {
    int result = 1;

    if (null != o) {
      result = (int) (this.id - o.id);
    }

    return result;
  }

  @Override
  public String toString() {
    return name;
  }
}
