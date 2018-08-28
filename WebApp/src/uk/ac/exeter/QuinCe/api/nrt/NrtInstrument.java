package uk.ac.exeter.QuinCe.api.nrt;

/**
 * POJO bean for an NRT instrument. Used by GetNrtInstruments
 * @author zuj007
 *
 */
public class NrtInstrument {

  /**
   * Instrument ID
   */
  private long id;

  /**
   * Instrument name
   */
  private String name;

  /**
   * Instrument owner's name
   */
  private String owner;

  /**
   * Blank constructor - required for MOXy JSON
   */
  public NrtInstrument() {
  }

  /**
   * Complete constructor
   * @param id instrument ID
   * @param name instrument name
   * @param owner instrument owner's name
   */
  public NrtInstrument(long id, String name, String owner) {
    this.id = id;
    this.name = name;
    this.owner = owner;
  }

  /**
   * Get the instrument ID
   * @return instrument ID
   */
  public long getId() {
    return id;
  }

  /**
   * Set the instrument ID
   * @param id instrument ID
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Get the instrument name
   * @return instrument name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the instrument name
   * @param name instrument name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the instrument owner's name
   * @return instrument owner's name
   */
  public String getOwner() {
    return owner;
  }

  /**
   * Set the instrument owner's name
   * @param owner instrument owner's name
   */
  public void setOwner(String owner) {
    this.owner = owner;
  }
}
