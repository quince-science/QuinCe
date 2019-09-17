package uk.ac.exeter.QuinCe.web.datasets.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.primefaces.json.JSONArray;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;
import uk.ac.exeter.QuinCe.data.Dataset.Position;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public abstract class DatasetMeasurementData
  extends TreeMap<LocalDateTime, LinkedHashMap<Field, FieldValue>> {

  protected Instrument instrument;

  protected FieldSets fieldSets;

  protected DataSet dataSet;

  private boolean dirty = true;

  /**
   * A cache of the row IDs in the data
   */
  private List<LocalDateTime> rowIdsCache = new ArrayList<LocalDateTime>();

  /**
   * JSON representation of the row IDs in the data
   */
  private String rowIdsJson;

  /**
   * A log of which rows have been fully loaded into the data set
   */
  private HashMap<LocalDateTime, Boolean> rowsLoaded;

  /**
   * A log of which fields have been fully loaded into the data set
   */
  protected HashMap<Field, Boolean> fieldsLoaded;

  private TreeMap<LocalDateTime, Position> positions;

  private Map<Field, MapRecords> mapCache;

  private boolean filterInitialised = false;

  public DatasetMeasurementData(Instrument instrument, FieldSets fieldSets,
    DataSet dataSet) throws MeasurementDataException {
    super();
    this.instrument = instrument;
    this.dataSet = dataSet;
    this.fieldSets = fieldSets;
    this.rowsLoaded = new HashMap<LocalDateTime, Boolean>();
    this.fieldsLoaded = new HashMap<Field, Boolean>();
    for (Field field : fieldSets.getFields()) {
      fieldsLoaded.put(field, false);
    }

    mapCache = new HashMap<Field, MapRecords>();
    dirty = true;
  }

  /**
   * Add a value to the table
   *
   * @param rowId
   * @param field
   * @param value
   */
  public void addValue(LocalDateTime rowId, Field field, FieldValue value) {
    if (!containsKey(rowId)) {
      put(rowId, fieldSets.generateFieldValuesMap());
    }

    get(rowId).put(field, value);
    dirty = true;
  }

  /**
   * Add a value to the table
   *
   * @param rowId
   * @param fieldId
   * @param value
   */
  public void addValue(LocalDateTime rowId, long fieldId, FieldValue value) {
    addValue(rowId, fieldSets.getField(fieldId), value);
    // dirty flag is set in called method
  }

  /**
   * Add a set of values to the table
   *
   * @param rowId
   *          The table row
   * @param values
   *          The field values
   */
  protected void addValues(LocalDateTime rowId,
    Map<Field, ? extends FieldValue> values) {
    if (!containsKey(rowId)) {
      put(rowId, fieldSets.generateFieldValuesMap());
    }

    get(rowId).putAll(values);
  }

  /**
   * Get data for a plot
   *
   * @param xAxis
   *          X Axis
   * @param yAxis
   *          Y Axis
   * @return The plot data
   */
  public String getPlotData(Field xAxis, Field yAxis)
    throws MeasurementDataException {

    TreeSet<PlotRecord> records;

    if (xAxis.getId() == Field.ROWID_FIELD_ID) {
      records = getPlotDataWithIdAxis(yAxis);
    } else {
      records = getPlotDataWithNonIdAxis(xAxis, yAxis);
    }

    // TODO Convert to GSON
    JSONArray json = new JSONArray();

    for (PlotRecord record : records) {
      json.put(record.toJsonArray());
    }

    return json.toString();
  }

  public List<Double> getValueRange(Field field) {

    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    for (LinkedHashMap<Field, FieldValue> record : values()) {
      FieldValue value = record.get(field);
      if (null != value && !value.isNaN()) {
        if (value.getValue() < min) {
          min = value.getValue();
        }

        if (value.getValue() > max) {
          max = value.getValue();
        }
      }
    }

    Double[] result = { min, max };
    return Arrays.asList(result);
  }

  public String getMapData(Field field, GeoBounds bounds) {

    if (!mapCache.containsKey(field)) {
      buildMapCache(field);
    }

    return mapCache.get(field).getDisplayJson(bounds);
  }

  private void buildMapCache(Field field) {
    MapRecords records = new MapRecords(size());

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> entry : entrySet()) {

      if (entry.getValue().containsKey(field)) {
        FieldValue value = entry.getValue().get(field);

        if (null != value && !value.isNaN()) {
          Position position = getClosestPosition(entry.getKey());
          MapRecord record = new MapRecord(position,
            DateTimeUtils.dateToLong(entry.getKey()), value);
          records.add(record);
        }
      }
    }

    mapCache.put(field, records);
  }

  private TreeSet<PlotRecord> getPlotDataWithIdAxis(Field yAxis)
    throws MeasurementDataException {

    loadField(yAxis);

    TreeSet<PlotRecord> records = new TreeSet<PlotRecord>();

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> entry : entrySet()) {

      if (entry.getValue().containsKey(yAxis)) {
        FieldValue value = entry.getValue().get(yAxis);

        if (null != value && !value.isNaN()) {
          records.add(new PlotRecord(DateTimeUtils.dateToLong(entry.getKey()),
            DateTimeUtils.dateToLong(entry.getKey()), value, dataSet.isNrt()));
        }
      }
    }

    return records;
  }

  private TreeSet<PlotRecord> getPlotDataWithNonIdAxis(Field xAxis, Field yAxis)
    throws MeasurementDataException {

    loadField(xAxis, yAxis);

    TreeSet<PlotRecord> records = new TreeSet<PlotRecord>();

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> yEntry : entrySet()) {

      if (yEntry.getValue().containsKey(yAxis)) {

        long dateLong = DateTimeUtils.dateToLong(yEntry.getKey());
        FieldValue yValue = yEntry.getValue().get(yAxis);

        if (null != yValue && !yValue.isNaN()) {
          // If the current key also contains the x axis, use that
          FieldValue xValue = get(getClosestWithField(yEntry.getKey(), xAxis))
            .get(xAxis);

          records.add(new PlotRecord(xValue.getValue(), dateLong, yValue,
            dataSet.isNrt()));
        }
      }
    }

    return records;
  }

  private LocalDateTime getClosestWithField(LocalDateTime start, Field field) {
    LocalDateTime result = null;

    if (get(start).containsKey(field) && !get(start).get(field).isNaN()) {
      result = start;
    } else {

      long prevDiff = Long.MAX_VALUE;
      long nextDiff = Long.MAX_VALUE;

      LocalDateTime prevKey = lowerKeyWithField(start, field);
      if (null != prevKey) {
        prevDiff = Math.abs(ChronoUnit.SECONDS.between(prevKey, start));
      }

      LocalDateTime nextKey = higherKeyWithField(start, field);
      if (null != nextKey) {
        nextDiff = Math.abs(ChronoUnit.SECONDS.between(nextKey, start));
      }

      if (prevDiff <= nextDiff) {
        result = prevKey;
      } else {
        result = nextKey;
      }
    }

    return result;
  }

  private LocalDateTime lowerKeyWithField(LocalDateTime start, Field field) {

    LocalDateTime result = null;

    LocalDateTime searchKey = lowerKey(start);

    while (null == result && null != searchKey) {
      if (get(searchKey).containsKey(field)
        && !get(searchKey).get(field).isNaN()) {
        result = searchKey;
      } else {
        searchKey = lowerKey(searchKey);
      }
    }

    return result;
  }

  private LocalDateTime higherKeyWithField(LocalDateTime start, Field field) {

    LocalDateTime result = null;

    LocalDateTime searchKey = higherKey(start);

    while (null == result && null != searchKey) {
      if (get(searchKey).containsKey(field)
        && !get(searchKey).get(field).isNaN()) {
        result = searchKey;
      } else {
        searchKey = higherKey(searchKey);
      }
    }

    return result;
  }

  public List<LocalDateTime> getRowIds() {

    // If the cache size doesn't equal the data size, it needs rebuilding
    if (rowIdsCache.size() != size()) {
      rowIdsCache = new ArrayList<LocalDateTime>(keySet());
      makeRowIds();
    }

    return rowIdsCache;
  }

  public String getRowIdsJson() {
    if (dirty) {
      buildCaches();
    }

    return rowIdsJson;
  }

  private void buildCaches() {
    // makePositionLookup();
    dirty = false;
  }

  private void makeRowIds() {
    List<Long> jsonInput = new ArrayList<Long>(size());
    rowIdsCache.stream()
      .forEach(k -> jsonInput.add(DateTimeUtils.dateToLong(k)));

    Gson gson = new Gson();
    rowIdsJson = gson.toJson(jsonInput).toString();
  }

  private void makePositionLookup() {
    positions = new TreeMap<LocalDateTime, Position>();

    if (fieldSets.containsField(FileDefinition.LONGITUDE_COLUMN_ID)) {
      Field lonField = fieldSets.getField(FileDefinition.LONGITUDE_COLUMN_ID);
      Field latField = fieldSets.getField(FileDefinition.LATITUDE_COLUMN_ID);

      for (LocalDateTime time : keySet()) {
        LinkedHashMap<Field, FieldValue> data = get(time);
        if (data.containsKey(lonField)) {
          positions.put(time, new Position(data.get(lonField).getValue(),
            data.get(latField).getValue()));
        }
      }
    }
  }

  private Position getClosestPosition(LocalDateTime start) {

    Position result = null;

    if (positions.containsKey(start)) {
      result = positions.get(start);
    } else {
      long prevDiff = Long.MAX_VALUE;
      long nextDiff = Long.MAX_VALUE;

      LocalDateTime prevKey = lowerKey(start);
      if (null != prevKey) {
        prevDiff = Math.abs(ChronoUnit.SECONDS.between(prevKey, start));
      }

      LocalDateTime nextKey = higherKey(start);
      if (null != nextKey) {
        nextDiff = Math.abs(ChronoUnit.SECONDS.between(nextKey, start));
      }

      if (prevDiff <= nextDiff) {
        result = positions.get(prevKey);
      } else {
        result = positions.get(nextKey);
      }
    }

    return result;
  }

  /**
   * Get the field sets
   *
   * @return
   */
  public FieldSets getFieldSets() {
    return fieldSets;
  }

  public FieldValue getValue(LocalDateTime time, int fieldIndex) {
    return get(time).get(fieldSets.getField(fieldIndex));
  }

  public CommentSet getCommentSet(int fieldIndex, List<LocalDateTime> rows) {

    CommentSet result = new CommentSet();
    Field field = fieldSets.getField(fieldIndex);

    for (LocalDateTime id : rows) {
      FieldValue value = get(id).get(field);
      if (null != value) {
        if (!value.getQcFlag().isGood()) {
          result.addComment(value.getQcComment(), value.getQcFlag());
        }
      }
    }

    return result;

  }

  public List<FieldValue> setQC(List<LocalDateTime> rows, int fieldIndex,
    Flag flag, String comment) {

    List<FieldValue> updatedValues = new ArrayList<FieldValue>(rows.size());
    Field field = fieldSets.getField(fieldIndex);

    for (LocalDateTime id : rows) {
      FieldValue value = get(id).get(field);
      if (null != value) {
        value.setQcFlag(flag);
        value.setQcComment(comment);
        value.setNeedsFlag(false);
        updatedValues.add(value);
      }
    }

    return updatedValues;
  }

  public final void filterAndAddValues(String runType, LocalDateTime time,
    Map<Long, FieldValue> values) throws MeasurementDataException {

    if (!filterInitialised) {
      initFilter();
    }

    filterAndAddValuesAction(runType, time, values);
  }

  /**
   * Ensure that the specified times are present in the map. New times will be
   * added with no data, while existing times will be left as they are.
   *
   * @param times
   *          The times to be added
   */
  public final void addTimes(Collection<LocalDateTime> times) {
    times.stream().forEach(this::addTime);
  }

  /**
   * Add a set of values, filtering out unwanted values. The default filter
   * removes values for columns that are internally calibrated where the run
   * type is not a measurement. This has the effect of removing all values taken
   * during internal calibration. Ensure that the specified time is present in
   * the map. A new time will be added with no data, while an existing time will
   * be left as it is.
   *
   * @param time
   *          The time to be added
   */
  public final void addTime(LocalDateTime time) {
    if (!containsKey(time)) {
      put(time, fieldSets.generateFieldValuesMap());
      rowsLoaded.put(time, false);
    }
  }

  /**
   * Add a set of values, filtering out unwanted values. The default filter
   * removes values for columns that are internally calibrated where the run
   * type is not a measurement. This has the effect of removing all values taken
   * during internal calibration.
   *
   * Override this method to filter the supplied values according to need.
   *
   * @param currentRunType
   * @param time
   * @param values
   * @throws RecordNotFoundException
   */
  protected abstract void filterAndAddValuesAction(String runType,
    LocalDateTime time, Map<Long, FieldValue> values)
    throws MeasurementDataException;

  /**
   * Initialise information required for filterAndAddValues
   */
  protected abstract void initFilter() throws MeasurementDataException;

  /**
   * Load a range of data into the map by index
   *
   * @param start
   *          The first row to load
   * @param length
   *          The number of rows to load
   */
  public void loadRows(int start, int length) throws MeasurementDataException {
    List<LocalDateTime> datesToLoad = getRowIds()
      .subList(start, start + length - 1).stream()
      .filter(d -> !rowsLoaded.get(d)).collect(Collectors.toList());

    // Load those dates that haven't already been loaded
    load(datesToLoad);
    setRowLoaded(datesToLoad, true);
  }

  /**
   * Get the database ID of the dataset to which this data belongs
   *
   * @return The dataset ID
   */
  public long getDatasetId() {
    return dataSet.getId();
  }

  /**
   * Get the instrument that measured this data
   *
   * @return The instrument
   */
  public Instrument getInstrument() {
    return instrument;
  }

  private void setRowLoaded(Collection<LocalDateTime> times, boolean loaded) {
    times.stream().forEach(t -> this.rowsLoaded.put(t, loaded));
  }

  protected abstract void load(List<LocalDateTime> times)
    throws MeasurementDataException;

  private void loadField(Field... field) throws MeasurementDataException {
    loadFieldAction(Arrays.stream(field).filter(f -> !fieldsLoaded.get(f))
      .collect(Collectors.toList()));
    Arrays.stream(field).forEach(f -> fieldsLoaded.put(f, true));
  }

  protected abstract void loadFieldAction(List<Field> field)
    throws MeasurementDataException;
}
