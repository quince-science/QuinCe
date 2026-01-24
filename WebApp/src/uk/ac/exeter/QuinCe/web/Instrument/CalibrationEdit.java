package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Represents a single edit of a {@link Calibration}.
 */
public class CalibrationEdit {

  /**
   * Code for an Add action.
   */
  public static final int ADD = 1;

  /**
   * Code for an Edit action.
   */
  public static final int EDIT = 0;

  /**
   * Code for a Delete action.
   */
  public static final int DELETE = -1;

  /**
   * The action performed in this edit.
   */
  private int action;

  /**
   * The edited state of the calibration.
   */
  private Calibration calibration;

  /**
   * Create an instance of a calibration edit.
   *
   * @param action
   *          The edit action.
   * @param editedCalibration
   *          The {@link Calibration} that has been edited.
   */
  protected CalibrationEdit(int action, Calibration editedCalibration) {
    this.action = action;
    this.calibration = editedCalibration.makeCopy();
  }

  /**
   * Get the edit action.
   *
   * @return The edit action.
   */
  public int getAction() {
    return action;
  }

  /**
   * Get the database ID of the edited {@link Calibration}.
   *
   * <p>
   * For an {@link #ADD} edit, this will be
   * {@link DatabaseUtils#NO_DATABASE_RECORD}.
   * </p>
   *
   * @return The calibration ID.
   */
  public long getCalibrationId() {
    return calibration.getId();
  }

  /**
   * Validate the edited {@link Calibration}.
   *
   * @return {@code true} if the edited {@link Calibration} is valid;
   *         {@code false} if it is not.
   */
  public boolean validate() {
    return calibration.validate();
  }

  /**
   * Get the {@link Calibration}'s deployment date.
   *
   * @return The deployment date.
   */
  public LocalDateTime getDeploymentDate() {
    return calibration.getDeploymentDate();
  }

  /**
   * Get the specific {@link Calibration} class.
   *
   * @return The calibration class.
   */
  public Class<? extends Calibration> getCalibrationClass() {
    return calibration.getClass();
  }

  /**
   * Get the {@link Instrument} to which the {@link Calibration} applies.
   *
   * @return The instrument to which the calibration applies.
   */
  public Instrument getInstrument() {
    return calibration.getInstrument();
  }

  /**
   * Get the calibration target.
   *
   * @return The calibration target.
   */
  public String getTarget() {
    return calibration.getTarget();
  }

  /**
   * Get the {@link String} representation of the calibration type.
   *
   * @return The calibration type.
   */
  public String getType() {
    return calibration.getType();
  }

  /**
   * Get the {@link Calibration}'s coefficients as a JSON string.
   *
   * @return The calibration coefficients.
   */
  public String getCoefficientsJson() {
    return calibration.getCoefficientsJson();
  }
}
