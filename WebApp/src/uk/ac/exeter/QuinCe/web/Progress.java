package uk.ac.exeter.QuinCe.web;

/**
 * Information holder for a progress bar.
 *
 * <p>
 * The maximum value defaults to 100, in which case the progress value is
 * assumed to be a percentage. Otherwise the value will be calculated as a
 * percentage of the maximum.
 * </p>
 */
public class Progress {

  /**
   * The label for the progress bar.
   */
  private String name = "Progress";

  /**
   * The maximum (target) value of the progress bar.
   *
   * <p>
   * Defaults to 100 so it acts as a percentage.
   * </p>
   */
  private float max = 100F;

  /**
   * The current value. Starts at zero.
   */
  private float value = 0F;

  /**
   * Set the name/label for the progress bar.
   *
   * @param name
   *          The name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Set the maximum value for the progress bar.
   *
   * @param max
   *          The maximum value.
   */
  public void setMax(float max) {
    this.max = max;
  }

  /**
   * Set the value for the progress bar.
   *
   * @param value
   *          The value.
   */
  public void setValue(float value) {
    synchronized (this) {
      this.value = value;
    }
  }

  public void increment() {
    synchronized (this) {
      this.value += 1;
    }
  }

  /**
   * Get the name/label for the progress bar.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the current progress as a percentage.
   *
   * @return The progress percentage.
   */
  public float getProgress() {
    return max == 100F ? value : (value / max) * 100;
  }
}
