package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for setting up diagnostics QC stuff
 *
 */
@ManagedBean
@SessionScoped
public class DiagnosticQCSetupBean extends BaseManagedBean {

  private static final String NAV_QC = "diagnostic_qc_setup";

  private static final String ASSIGNED = "assigned";

  private static final String NOT_ASSIGNED = "notAssigned";

  /**
   * The instrument being set up.
   */
  private Instrument instrument;

  /**
   * The diagnostic sensor currently being set up.
   */
  private SensorAssignment currentDiagnosticSensor = null;

  /**
   * The measurement sensor currently being set up.
   */
  private SensorAssignment currentMeasurementSensor = null;

  /**
   * The variables assigned for the current measurement sensor.
   */
  private List<Long> assignedVariables = null;

  /**
   * Get the instrument's database ID
   *
   * @return The instrument ID
   */
  public long getInstrumentId() {
    return (null == instrument ? -1 : instrument.getId());
  }

  /**
   * Set the database ID of the instrument. Also loads the corresponding
   * {@link Instrument} object.
   *
   * @param instrumentId
   *          The instrument ID
   * @throws Exception
   *           If the instrument cannot be retrieved
   */
  public void setInstrumentId(long instrumentId) throws Exception {
    if (instrumentId > 0) {
      try {
        this.instrument = InstrumentDB.getInstrument(getDataSource(),
          instrumentId);
      } catch (Exception e) {
        ExceptionUtils.printStackTrace(e);
        throw e;
      }
    }
  }

  /**
   * Get the {@link Instrument} object for the sensors being set up.
   * 
   * @return The Instrument.
   */
  public Instrument getInstrument() {
    return instrument;
  }

  /**
   * Start up the bean.
   *
   * @return The bean navigation.
   */
  public String start() {
    currentDiagnosticSensor = instrument.getSensorAssignments()
      .getDiagnosticSensors().get(0);
    currentMeasurementSensor = getMeasurementSensors().get(0);
    updateAssignedVariables();

    return NAV_QC;
  }

  public List<SensorAssignment> getDiagnosticSensors() {
    return instrument.getSensorAssignments().getDiagnosticSensors();
  }

  public Long getCurrentDiagnosticSensor() {
    return currentDiagnosticSensor.getDatabaseId();
  }

  public void setCurrentDiagnosticSensor(Long sensor) {
    this.currentDiagnosticSensor = instrument.getSensorAssignments()
      .getById(sensor);
  }

  public void diagnosticSensorSelected(AjaxBehaviorEvent event) {
    updateAssignedVariables();
  }

  public List<SensorAssignment> getMeasurementSensors() {
    return instrument.getSensorAssignments().getNonDiagnosticSensors(false);
  }

  public Long getCurrentMeasurementSensor() {
    return currentMeasurementSensor.getDatabaseId();
  }

  public void setCurrentMeasurementSensor(Long sensorId) {
    this.currentMeasurementSensor = instrument.getSensorAssignments()
      .getById(sensorId);
  }

  public void measurementSensorSelected(AjaxBehaviorEvent event) {
    updateAssignedVariables();
  }

  public List<Long> getAssignedVariables() {
    return assignedVariables;
  }

  public void setAssignedVariables(List<Long> assignedVariables) {
    this.assignedVariables = assignedVariables;
  }

  public void variablesUpdated() throws VariableNotFoundException {
    instrument.getDiagnosticQCConfig().setAssignedVariables(
      currentDiagnosticSensor, currentMeasurementSensor,
      instrument.getVariables(assignedVariables));
  }

  public String getVarsAssignedString(Long sensorId) {
    SensorAssignment assignment = instrument.getSensorAssignments()
      .getById(sensorId);
    boolean assigned = instrument.getDiagnosticQCConfig()
      .hasAssignedVariables(currentDiagnosticSensor, assignment);
    return assigned ? ASSIGNED : NOT_ASSIGNED;
  }

  public String getSensorName(Long sensorId) {
    return instrument.getSensorAssignments().getById(sensorId).getSensorName();
  }

  public void assignAllVariables() {
    instrument.getDiagnosticQCConfig().setAssignedVariables(
      currentDiagnosticSensor, currentMeasurementSensor,
      instrument.getVariables());
    updateAssignedVariables();
  }

  public void unassignAllVariables() {
    instrument.getDiagnosticQCConfig().setAssignedVariables(
      currentDiagnosticSensor, currentMeasurementSensor, null);
    updateAssignedVariables();
  }

  private void updateAssignedVariables() {
    assignedVariables = instrument.getDiagnosticQCConfig()
      .getAssignedVariables(currentDiagnosticSensor, currentMeasurementSensor)
      .stream().map(v -> v.getId()).toList();
  }
}
