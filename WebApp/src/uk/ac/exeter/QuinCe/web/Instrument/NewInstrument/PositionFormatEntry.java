package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

public class PositionFormatEntry {

  private final int id;

  private final String name;

  protected PositionFormatEntry(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

}
