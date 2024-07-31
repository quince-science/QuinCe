package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;

/**
 * Represents a single edit of a calibration.
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

  protected CalibrationEdit(int action, Calibration editedCalibration) {
    this.action = action;
    this.calibration = editedCalibration.makeCopy();
  }

  public int getAction() {
    return action;
  }

  public long getCalibrationId() {
    return calibration.getId();
  }

  public boolean validate() {
    return calibration.validate();
  }

  public LocalDateTime getDeploymentDate() {
    return calibration.getDeploymentDate();
  }

  public Instrument getInstrument() {
    return calibration.getInstrument();
  }

  public String getTarget() {
    return calibration.getTarget();
  }

  public String getType() {
    return calibration.getType();
  }

  public String getCoefficientsJson() {
    return calibration.getCoefficientsJson();
  }
}
