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
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

@SuppressWarnings("serial")
public abstract class DatasetMeasurementData
  extends TreeMap<LocalDateTime, LinkedHashMap<Field, FieldValue>> {

  protected Instrument instrument;

  protected FieldSets fieldSets;

  protected DataSet dataSet;

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
  protected HashMap<Field, Boolean> loadedFields;

  private TreeMap<LocalDateTime, Position> positions;

  private boolean positionsLoaded = false;

  private Map<Field, MapRecords> mapCache;

  public DatasetMeasurementData(Instrument instrument, FieldSets fieldSets,
    DataSet dataSet) throws MeasurementDataException {
    super();
    this.instrument = instrument;
    this.dataSet = dataSet;
    this.fieldSets = fieldSets;
    this.rowsLoaded = new HashMap<LocalDateTime, Boolean>();
    this.loadedFields = new HashMap<Field, Boolean>();
    for (Field field : fieldSets.getFields()) {
      loadedFields.put(field, false);
    }

    mapCache = new HashMap<Field, MapRecords>();

    initFilter();
  }

  /**
   * Add a value to the table
   *
   * @param rowId
   * @param field
   * @param value
   * @throws MissingParamException
   */
  public void addValue(LocalDateTime rowId, Field field, FieldValue value)
    throws MissingParamException {

    MissingParam.checkMissing(rowId, "rowId");
    MissingParam.checkMissing(field, "field");
    MissingParam.checkMissing(value, "value");

    if (!fieldSets.isUnused(field)) {
      if (!containsKey(rowId)) {
        put(rowId, fieldSets.generateFieldValuesMap());
      }

      get(rowId).put(field, value);
    }
  }

  /**
   * Add a value to the table
   *
   * @param rowId
   * @param fieldId
   * @param value
   * @throws MissingParamException
   */
  public void addValue(LocalDateTime rowId, long fieldId, FieldValue value)
    throws MissingParamException {
    addValue(rowId, fieldSets.getField(fieldId), value);
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
    Map<Field, ? extends FieldValue> values) throws MissingParamException {
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

  public String getMapData(Field field, GeoBounds bounds)
    throws MeasurementDataException {

    if (!mapCache.containsKey(field)) {
      buildMapCache(field);
    }

    return mapCache.get(field).getDisplayJson(bounds);
  }

  private void buildMapCache(Field field) throws MeasurementDataException {

    loadField(field);

    MapRecords records = new MapRecords(size());

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> entry : entrySet()) {

      if (entry.getValue().containsKey(field)) {
        FieldValue value = entry.getValue().get(field);

        if (null != value && !value.isNaN() && !value.isGhost()) {
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
    updateRowIdsCache();
    return rowIdsCache;
  }

  public String getRowIdsJson() {
    updateRowIdsCache();
    return rowIdsJson;
  }

  /**
   * Determines whether or not this data contains position data
   *
   * @return {@code true} if the data contains positions; {@code false}
   *         otherwise.
   */
  public boolean containsPosition() {
    return fieldSets.containsField(FileDefinition.LONGITUDE_COLUMN_ID)
      || fieldSets.containsField(FileDefinition.LATITUDE_COLUMN_ID);
  }

  private void updateRowIdsCache() {
    // If the cache size doesn't equal the data size, it needs rebuilding
    if (rowIdsCache.size() != size()) {
      rowIdsCache = new ArrayList<LocalDateTime>(keySet());
      makeRowIds();
    }
  }

  private void makeRowIds() {
    List<Long> jsonInput = new ArrayList<Long>(size());
    rowIdsCache.stream()
      .forEach(k -> jsonInput.add(DateTimeUtils.dateToLong(k)));

    Gson gson = new Gson();
    rowIdsJson = gson.toJson(jsonInput).toString();
  }

  private void loadPositions() throws MeasurementDataException {

    if (fieldSets.containsField(FileDefinition.LONGITUDE_COLUMN_ID)) {
      loadField(fieldSets.getField(FileDefinition.LONGITUDE_COLUMN_ID),
        fieldSets.getField(FileDefinition.LATITUDE_COLUMN_ID));

      positions = new TreeMap<LocalDateTime, Position>();

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

    positionsLoaded = true;
  }

  private Position getClosestPosition(LocalDateTime start)
    throws MeasurementDataException {

    if (!positionsLoaded) {
      loadPositions();
    }

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

  public FieldValue getValue(LocalDateTime time, long fieldId) {
    return get(time).get(fieldSets.getField(fieldId));
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
    Flag flag, String comment) throws MeasurementDataException {

    return setQC(rows, fieldSets.getField(fieldIndex), flag, comment);
  }

  public List<FieldValue> setQC(List<LocalDateTime> rows, int fieldIndex,
    int flag, String comment)
    throws InvalidFlagException, MeasurementDataException {

    return setQC(rows, fieldSets.getField(fieldIndex), new Flag(flag), comment);
  }

  public List<FieldValue> setQC(List<LocalDateTime> rows, Field field, int flag,
    String comment) throws InvalidFlagException, MeasurementDataException {

    return setQC(rows, field, new Flag(flag), comment);
  }

  public List<FieldValue> setQC(List<LocalDateTime> rows, Field field,
    Flag flag, String comment) throws MeasurementDataException {

    List<FieldValue> updatedValues = new ArrayList<FieldValue>(rows.size());

    for (LocalDateTime time : rows) {
      FieldValue value = get(time).get(field);

      // Do not set QC on ghost data
      if (null != value && !value.isGhost()) {
        value.setQC(flag, comment);
        updatedValues.add(value);
      }
    }

    return updatedValues;
  }

  public abstract void filterAndAddValues(String runType, LocalDateTime time,
    Map<Field, FieldValue> values)
    throws MeasurementDataException, MissingParamException;

  /**
   * Ensure that the specified times are present in the map. New times will be
   * added with no data, while existing times will be left as they are.
   *
   * @param times
   *          The times to be added
   */
  public void addTimes(Collection<LocalDateTime> times)
    throws MeasurementDataException {

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
  protected final void addTime(LocalDateTime time) {
    if (!containsKey(time)) {
      put(time, fieldSets.generateFieldValuesMap());
      rowsLoaded.put(time, false);
    }
  }

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

    List<LocalDateTime> rowIds = getRowIds();

    int subListEnd = start + length;
    if (subListEnd > size()) {
      subListEnd = size();
    }

    List<LocalDateTime> datesToLoad = rowIds.subList(start, subListEnd).stream()
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

  protected void load(List<LocalDateTime> times)
    throws MeasurementDataException {
    loadAction(times.stream().filter(t -> !rowsLoaded.get(t))
      .collect(Collectors.toList()));
  }

  protected abstract void loadAction(List<LocalDateTime> times)
    throws MeasurementDataException;

  private void loadField(Field... field) throws MeasurementDataException {
    loadFieldAction(Arrays.stream(field).filter(f -> !loadedFields.get(f))
      .collect(Collectors.toList()));
    Arrays.stream(field).forEach(f -> loadedFields.put(f, true));
  }

  protected abstract void loadFieldAction(List<Field> field)
    throws MeasurementDataException;

  /**
   * Apply the specified QC flag to all the {@link FieldValue}s in a given
   * {@link FieldSet} at the specified times.
   *
   * <p>
   * This method takes the QC flag and comment from a given {@link Field} and
   * applies it to all the fields in the specified {@link FieldSet}. This is
   * useful where the quality of a given field impacts the quality of all the
   * others. For example, a bad position implies that all the sensors (SST,
   * Salinity etc.) must also be bad.
   * </p>
   * <p>
   * Depending on the instrument's configuration, the affected fields may not
   * have values at the exact times as the source field. Therefore, any
   * {@link FieldValue} in the {@link FieldSet} that occurs before the
   * <i>next</i> value from the source {@link Field} will be included in the
   * update.
   * </p>
   * <p>
   * Each {@link FieldValue} will be treated independently of all the others,
   * with the QC flag applied as follows:
   * </p>
   * <ul>
   * <li>If the field has no user QC set, the supplied QC is applied with the
   * specified prefix so it can be detected correctly in future updates.</li>
   * <li>If the field already has a user QC set with the supplied prefix, we
   * update the field's QC as follows:
   * <ul>
   * <li>If the supplied value is {@link Flag#GOOD}, the field's flag is
   * removed. If the field's auto QC is anything other than {@link Flag#GOOD},
   * the user QC flag is set to {@link Flag#NEEDED}.</li>
   * <li>If the supplied value is anything other than {@link Flag#GOOD}, the
   * supplied QC is applied with the specified prefix.</li>
   * </ul>
   * </li>
   * <li>If the field already has a user QC set without the supplied prefix, and
   * the supplied flag is worse than the existing flag, the supplied QC is
   * applied with the specified prefix.</li>
   * </ul>
   * <p>
   * Ghost data values are ignored by this method.
   * </p>
   *
   * @param times
   * @param fieldSet
   * @param commentPrefix
   * @param userFlag
   * @param userComment
   * @return
   * @throws MeasurementDataException
   */
  public List<FieldValue> applyQcToFieldSet(List<LocalDateTime> times,
    FieldSet fieldSet, Field sourceField, String commentPrefix, Flag flag,
    String comment) throws MeasurementDataException {

    String appliedComment = commentPrefix + " " + comment;

    List<FieldValue> updates = new ArrayList<FieldValue>();

    // Find all the times to be updated. This includes the times in the supplied
    // list, plus: For each time, all the times until the *next* time that a
    // value from the source field is recorded
    TreeSet<LocalDateTime> updateTimes = new TreeSet<LocalDateTime>();

    for (LocalDateTime time : times) {
      updateTimes.add(time);

      boolean nextTimeFound = false;
      LocalDateTime searchKey = higherKey(time);

      while (!nextTimeFound) {
        if (null == searchKey || null != get(searchKey).get(sourceField)) {
          nextTimeFound = true;
        } else {
          updateTimes.add(searchKey);
          searchKey = higherKey(searchKey);
        }
      }
    }

    // Make sure the data for all times are loaded
    load(new ArrayList<LocalDateTime>(updateTimes));

    // updateTimes now contains all the times we have to update. Loop through
    // each of these times
    for (LocalDateTime time : updateTimes) {

      // Loop through each field in the specified fieldset
      for (Field field : fieldSets.get(fieldSet)) {

        FieldValue value = get(time).get(field);
        if (null != value && !value.isGhost()) {

          // If the new flag is more significant than the value's existing flag,
          // or of the same significance but the flag has not been confirmed by
          // the user, set it.
          if (flag.moreSignificantThan(value.getQcFlag())
            || (flag.equalSignificance(value.getQcFlag()) && value.needsFlag)) {

            value.setQC(flag, appliedComment);
            updates.add(value);
          } else {

            // If the value has a field that doesn't have the supplied prefix,
            // see if the new QC value is worse than the existing one. If so,
            // replace it.
            if (!value.getQcComment().startsWith(commentPrefix)) {
              if (flag.moreSignificantThan(value.getQcFlag())) {
                value.setQC(flag, appliedComment);
                updates.add(value);
              }
            } else {
              if (!flag.isGood()) {
                value.setQC(flag, appliedComment);
                updates.add(value);
              } else {
                value.setQC(Flag.NEEDED, null);
                updates.add(value);
              }
            }
          }
        }
      }
    }

    return updates;
  }
}
