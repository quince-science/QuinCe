package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;

import uk.ac.exeter.QuinCe.data.Instrument.DiagnosticSensorQCConfig;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
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
   * The Run Types assigned for the current measurement sensor.
   */
  private List<String> assignedRunTypes = null;

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
   * Start up the bean.
   *
   * @return The bean navigation.
   */
  public String start() {
    currentDiagnosticSensor = instrument.getSensorAssignments()
      .getDiagnosticSensors().get(0);
    currentMeasurementSensor = getMeasurementSensors().get(0);
    updateAssignedRunTypes();

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
    updateAssignedRunTypes();
  }

  public String getDiagnosticSensorAssignedString(Long sensorId) {
    SensorAssignment assignment = instrument.getSensorAssignments()
      .getById(sensorId);
    boolean assigned = instrument.getDiagnosticQCConfig()
      .hasAssignedRunTypes(assignment);
    return assigned ? ASSIGNED : NOT_ASSIGNED;
  }

  public Double getMin() {
    return instrument.getDiagnosticQCConfig()
      .getRangeMin(currentDiagnosticSensor);
  }

  public void setMin(Double min) {
    instrument.getDiagnosticQCConfig().setRangeMin(currentDiagnosticSensor,
      min);
  }

  public Double getMax() {
    return instrument.getDiagnosticQCConfig()
      .getRangeMax(currentDiagnosticSensor);
  }

  public void setMax(Double max) {
    instrument.getDiagnosticQCConfig().setRangeMax(currentDiagnosticSensor,
      max);
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
    updateAssignedRunTypes();
  }

  /**
   * Get the list of run type categories for which there are run types.
   *
   * @return The run type categories.
   */
  public List<RunTypeCategory> getInstrumentRunTypeCategories() {
    return new ArrayList<RunTypeCategory>(instrument.getAllRunTypes().keySet());
  }

  /**
   * Get all the run types from all run type categories.
   *
   * <p>
   * This is used for the Run Types input on the JSF page.
   * </p>
   *
   * @return All the run types.
   */
  public List<RunTypeAssignment> getAllRunTypes() {
    List<RunTypeAssignment> result = new ArrayList<RunTypeAssignment>();

    for (RunTypeCategory category : getInstrumentRunTypeCategories()) {
      result.addAll(instrument.getAllRunTypes().get(category));
    }

    return result;
  }

  public List<String> getAssignedRunTypes() {
    return assignedRunTypes;
  }

  public void setAssignedRunTypes(List<String> assignedRunTypes) {
    this.assignedRunTypes = assignedRunTypes;
  }

  public void runTypesUpdated() throws VariableNotFoundException {
    instrument.getDiagnosticQCConfig().setAssignedRunTypes(
      currentDiagnosticSensor, currentMeasurementSensor, assignedRunTypes);
  }

  public String getMeasurementSensorAssignedString(Long sensorId) {
    SensorAssignment assignment = instrument.getSensorAssignments()
      .getById(sensorId);
    boolean assigned = instrument.getDiagnosticQCConfig()
      .hasAssignedRunTypes(currentDiagnosticSensor, assignment);
    return assigned ? ASSIGNED : NOT_ASSIGNED;
  }

  public String getSensorName(Long sensorId) {
    return instrument.getSensorAssignments().getById(sensorId).getSensorName();
  }

  public void assignAllRunTypes() {
    instrument.getDiagnosticQCConfig().setAssignedRunTypes(
      currentDiagnosticSensor, currentMeasurementSensor,
      instrument.getAllRunTypeNames());
    updateAssignedRunTypes();
  }

  public void unassignAllRunTypes() {
    instrument.getDiagnosticQCConfig().setAssignedRunTypes(
      currentDiagnosticSensor, currentMeasurementSensor, null);
    updateAssignedRunTypes();
  }

  private void updateAssignedRunTypes() {
    assignedRunTypes = instrument.getDiagnosticQCConfig()
      .getAssignedRunTypes(currentDiagnosticSensor, currentMeasurementSensor);
  }

  private DiagnosticSensorQCConfig getCurrentSensorConfig() {
    return instrument.getDiagnosticQCConfig().get(currentDiagnosticSensor);
  }
}
