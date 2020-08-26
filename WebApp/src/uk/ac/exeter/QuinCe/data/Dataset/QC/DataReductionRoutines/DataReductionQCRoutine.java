package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReductionRoutines;

/**
 * Base class for QC routines to be run during data reduction
 *
 * @author Steve Jones
 *
 */
public abstract class DataReductionQCRoutine {

  /**
   * The routine settings
   */
  protected final DataReductionQCRoutineSettings settings;

  /**
   * Basic constructor.
   *
   * @param settings
   *          The routine settings
   */
  protected DataReductionQCRoutine(DataReductionQCRoutineSettings settings) {
    this.settings = settings;
  }

}
