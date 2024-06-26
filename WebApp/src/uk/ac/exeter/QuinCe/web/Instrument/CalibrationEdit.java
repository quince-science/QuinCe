package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.LocalDateTime;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationCoefficient;

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

  private long calibrationId;

  private LocalDateTime deploymentDate;

  private String target;

  private List<CalibrationCoefficient> coefficients;

  protected CalibrationEdit(int action, Calibration editedCalibration) {
    this.action = action;
    this.calibrationId = editedCalibration.getId();
    this.deploymentDate = editedCalibration.getDeploymentDate();
    this.target = editedCalibration.getTarget();
    this.coefficients = editedCalibration.getCoefficients().stream()
      .map(c -> (CalibrationCoefficient) c.clone()).toList();
  }
}
