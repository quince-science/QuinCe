package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;

/**
 * Class to define a set of InstrumentFiles when creating an instrument
 * @author Steve Jones
 *
 */
public class NewInstrumentFileSet extends InstrumentFileSet {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 3353458532623379331L;

  /**
   * Simple constructor to create an empty set
   */
  protected NewInstrumentFileSet() {
    super();
  }

  @Override
  public boolean add(FileDefinition file) {
    boolean result = false;

    if (file instanceof FileDefinitionBuilder) {
      result = super.add(file);
    }

    return result;
  }

  /**
   * Get the time and position column assignments for all
   * files related to this instrument.
   *
   * <p>
   *   The assignments are encoded as a JSON string in
   *   the following format:
   * </p>
   * <pre>
   * [
   * ]
   * </pre>
   * @return The time and position assignments
   * @throws DateTimeSpecificationException If an error occurs while generating the date/time string
   */
  public String getFileSpecificAssignments(SensorAssignments sensorAssignments) throws DateTimeSpecificationException {
    JsonArray json = new JsonArray();

    for (FileDefinition file : this) {
      JsonObject object = new JsonObject();
      object.add("longitude", file.getLongitudeSpecification().getJsonObject());
      object.add("latitude", file.getLatitudeSpecification().getJsonObject());
      object.add("datetime", file.getDateTimeSpecification().getJsonArray());
      object.addProperty("runTypeColRequired",
          file.requiresRunTypeColumn(sensorAssignments));
      object.addProperty("runTypeCol", file.getRunTypeColumn());
      json.add(object);
    }

    Gson gson = new Gson();
    return gson.toJson(json);
  }

  /**
   * Get the list of registered file descriptions and their columns as a JSON string
   * @return The file names
   */
  public String getFilesAndColumns() {
    JsonArray json = new JsonArray();

    for (FileDefinition file : this) {
      FileDefinitionBuilder fileBuilder = (FileDefinitionBuilder) file;
      JsonObject object = new JsonObject();
      object.addProperty("description", fileBuilder.getFileDescription());
      object.addProperty("columns", fileBuilder.getFileColumns());
      json.add(object);
    }
    Gson gson = new Gson();
    return gson.toJson(json);
  }
}
