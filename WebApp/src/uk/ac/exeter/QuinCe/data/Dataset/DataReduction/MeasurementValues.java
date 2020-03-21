package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@SuppressWarnings("serial")
public class MeasurementValues
  extends HashMap<SensorType, ArrayList<MeasurementValue>> {

  private Instrument instrument;

  private Measurement measurement;

  public MeasurementValues(Instrument instrument, Measurement measurement) {
    super();
    this.instrument = instrument;
    this.measurement = measurement;
  }

  public Instrument getInstrument() {
    return instrument;
  }

  public void put(SensorType sensorType, MeasurementValue value) {
    if (!containsKey(sensorType)) {
      put(sensorType, new ArrayList<MeasurementValue>());
    }

    get(sensorType).add(value);
  }

  public Double getValue(String sensorType,
    Map<String, ArrayList<Measurement>> allMeasurements, Connection conn)
    throws Exception {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    return getValue(sensorConfig.getSensorType(sensorType), allMeasurements,
      conn);
  }

  public Double getValue(SensorType sensorType,
    Map<String, ArrayList<Measurement>> allMeasurements, Connection conn)
    throws Exception {

    return ValueCalculators.getInstance().calculateValue(this, sensorType,
      allMeasurements, conn);
  }

  public Measurement getMeasurement() {
    return measurement;
  }
}
