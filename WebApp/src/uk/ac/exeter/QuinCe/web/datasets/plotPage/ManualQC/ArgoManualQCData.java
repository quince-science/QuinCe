package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import uk.ac.exeter.QuinCe.data.Dataset.ArgoCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.ArgoProfile;
import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.Progress;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ArgoPlot;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ArgoPlotPageTableRecord;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ArgoQCMap;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.CoordinateIdSerializer;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.DataLatLng;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.MapRecords;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.Plot;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecord;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecordSerializer;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageValueMapRecord;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.QCMap;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SimplePlotPageTableValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ArgoManualQCData extends ManualQCData {

  /**
   * A Gson object.
   */
  private static Gson gson = new Gson();

  /**
   * Column heading for the Cycle Number, which is the basis for the map scale.
   */
  private PlotPageColumnHeading cycleNumberHeading;

  /**
   * The profiles which are the basis for user selection of the data view.
   */
  private List<ArgoProfile> profiles = null;

  /**
   * The contents of the Profiles table as a JSON String.
   */
  private String profileTableData = null;

  /**
   * The number of records in the currently selected profile.
   */
  private int profileRecordCount = -1;

  /**
   * All plots use the Depth column as the Y axis. We cache it here.
   */
  private PlotPageColumnHeading depthColumn = null;

  /**
   * Details for the first plot. We use a special plot implementation.
   */
  private ArgoPlot argoPlot1;

  /**
   * Details for the second plot. We use a special plot implementation.
   */
  private ArgoPlot argoPlot2;

  /**
   * Details for the profiles map.
   *
   * <p>
   * This overrides {@link PlotPageData#map2} using a special QC Map
   * implementation.
   */
  private ArgoQCMap profilesMap;

  /**
   * The index of the currently selected profile.
   *
   * <p>
   * Defaults to the first profile.
   * </p>
   */
  private int selectedProfile = 0;

  /**
   * Extra details for the currently selected profile.
   *
   * <p>
   * Stored as a JSON string ready for sending to the front end.
   * </p>
   */
  private String selectedProfileDetails;

  /**
   * The data table row IDs for the currently selected profile.
   */
  private List<Long> profileRowIDs;

  /**
   * Construct the data object.
   *
   * <p>
   * Initially the object is empty. The data will be loaded by the
   * {@link #load(DataSource)} method.
   * </p>
   *
   * @param instrument
   *          The instrument that the dataset belongs to.
   * @param dataset
   *          The dataset.
   * @param dataSource
   *          A data source.
   * @throws SQLException
   * @throws Exception
   *           If the data cannot be loaded.
   */
  protected ArgoManualQCData(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws SensorTypeNotFoundException, SQLException {
    super(dataSource, instrument, dataset);

    SensorType cycleNumberSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Cycle Number");

    cycleNumberHeading = new PlotPageColumnHeading(
      instrument.getSensorAssignments().get(cycleNumberSensorType).first()
        .getColumnHeading(),
      true, false, true);
  }

  @Override
  protected PlotPageColumnHeading getDefaultMap1Column() throws Exception {
    return cycleNumberHeading;
  }

  @Override
  protected List<PlotPageColumnHeading> buildRootColumns()
    throws SensorTypeNotFoundException {
    List<PlotPageColumnHeading> rootColumns = new ArrayList<PlotPageColumnHeading>(
      7);

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    /*
     * We don't include the cycle number, direction or profile because they are
     * specified as part of the selected profile.
     *
     * Source File and Time are shown in the Profile Details and not as part of
     * the table.
     */

    rootColumns
      .add(new PlotPageColumnHeading(
        instrument.getSensorAssignments()
          .get(sensorConfig.getSensorType("Level")).first().getColumnHeading(),
        true, false, true));

    depthColumn = new PlotPageColumnHeading(instrument.getSensorAssignments()
      .get(sensorConfig.getSensorType("Pressure (Depth)")).first()
      .getColumnHeading(), true, false, true);

    rootColumns.add(depthColumn);

    return rootColumns;
  }

  @Override
  protected List<PlotPageColumnHeading> buildExtendedRootColumns()
    throws SensorTypeNotFoundException {

    return buildRootColumns();
  }

  public String getProfileTableColumns() {
    return gson.toJson(
      Arrays.asList(new String[] { "Cycle Number", "Direction", "Profile" }));
  }

  public String getProfileTableData() throws Exception {
    return profileTableData;
  }

  /**
   * Generate the main table data for a specified {@link ArgoProfile}.
   *
   * <p>
   * This is used in place of the normal implementation of
   * {@link ManualQCData#generateTableData(int, int). The method always loads
   * the complete set of data for the currently selected profile, rather than
   * loading a portion of the complete dataset's data.
   * </p>
   *
   * <p>
   * The format of this method's output is the same as for that method.
   * </p>
   */
  public String generateTableData() {

    ArgoProfile profile = profiles.get(selectedProfile);

    // Get the Coordinates matching the profile
    List<Coordinate> cycleCoordinates = getCoordinates().stream()
      .filter(c -> profile.matches((ArgoCoordinate) c)).toList();

    profileRecordCount = cycleCoordinates.size();

    List<PlotPageTableRecord> records = new ArrayList<PlotPageTableRecord>(
      cycleCoordinates.size());

    try {
      for (Coordinate baseCoordinate : cycleCoordinates) {
        ArgoCoordinate coordinate = (ArgoCoordinate) baseCoordinate;

        PlotPageTableRecord record = new ArgoPlotPageTableRecord(coordinate,
          sensorValues.getFlagScheme());
        record.addCoordinate(coordinate);

        Map<Long, SensorValue> recordSensorValues = sensorValues
          .get(coordinate);

        Long measurementId = null;
        Measurement measurement = measurements.get(coordinate);
        if (null != measurement) {
          measurementId = measurement.getId();
        }

        Map<Variable, ReadOnlyDataReductionRecord> dataReductionData = null;

        if (null != measurementId) {
          // Retrieve the data reduction data
          dataReductionData = dataReduction.get(measurementId);
        }

        for (long columnId : sensorColumnIds) {
          record.addColumn(recordSensorValues.get(columnId));
        }

        addDiagnosticColumns(record, recordSensorValues);
        addMeasurementColumns(record, measurement);
        addDataReductionColumns(record, dataReductionData);

        records.add(record);
      }

    } catch (Exception e) {
      error("Error building table data", e);
    }

    return tableDataGson.toJson(records);
  }

  @Override
  public int size() {
    return profileRecordCount;
  }

  /**
   * Build map records based on cycle numbers, with one record per cycle.
   */
  @Override
  protected void buildMapCache(PlotPageColumnHeading column) throws Exception {

    MapRecords records = new MapRecords(0, getAllSensorValues(),
      StringUtils::formatNumberToInt);

    HashSet<Integer> usedCycleNumbers = new HashSet<Integer>();

    for (int i = 0; i < profiles.size(); i++) {

      ArgoProfile profile = profiles.get(i);

      if (!usedCycleNumbers.contains(profile.getCycleNumber())) {

        if (null != profile.getPosition()) {
          records.add(new PlotPageValueMapRecord(profile.getPosition(), i,
            new SimplePlotPageTableValue(
              String.valueOf(profile.getCycleNumber()),
              sensorValues.getFlagScheme())));
          usedCycleNumbers.add(profile.getCycleNumber());
        }
      }
    }

    mapCache.put(column, records);
  }

  @Override
  protected List<Long> getMapSelection() {
    long mapSelection = -1L;

    for (int i = 0; i < profiles.size(); i++) {
      if (profiles.get(i).getCycleNumber() == profiles.get(selectedProfile)
        .getCycleNumber()) {
        mapSelection = i;
        break;
      }
    }

    return List.of(mapSelection);
  }

  @Override
  protected void initTableDataGson() {
    tableDataGson = new GsonBuilder()
      .registerTypeHierarchyAdapter(PlotPageTableRecord.class,
        new PlotPageTableRecordSerializer(getAllSensorValues()))
      .registerTypeHierarchyAdapter(Coordinate.class,
        new CoordinateIdSerializer())
      .create();
  }

  @Override
  protected PlotPageColumnHeading getDefaultXAxis1() throws Exception {
    return getColumnHeadings().get(SENSORS_FIELD_GROUP).get(0);
  }

  @Override
  protected PlotPageColumnHeading getDefaultXAxis2() throws Exception {
    return getColumnHeadings().get(SENSORS_FIELD_GROUP).get(1);
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis1() throws Exception {
    return depthColumn;
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis2() throws Exception {
    return depthColumn;
  }

  @Override
  public Plot getPlot1() {
    return argoPlot1;
  }

  @Override
  public Plot getPlot2() {
    return argoPlot2;
  }

  @Override
  protected void createPlot1() throws Exception {
    argoPlot1 = new ArgoPlot(this, getDefaultXAxis1(), getDefaultYAxis1(),
      !dataset.isNrt());
  }

  @Override
  protected void createPlot2() throws Exception {
    argoPlot2 = new ArgoPlot(this, getDefaultXAxis2(), getDefaultYAxis2(),
      !dataset.isNrt());
  }

  public int getSelectedProfile() {
    return selectedProfile;
  }

  public void setSelectedProfile(int profileIndex) {
    this.selectedProfile = profileIndex;
    buildProfileDetails();
  }

  public String getSelectedProfileDetails() {
    return selectedProfileDetails;
  }

  public List<ArgoProfile> getProfiles() {
    return profiles;
  }

  private void buildProfileDetails() {

    List<List<String>> profileDetails = new ArrayList<List<String>>();

    ArgoProfile profile = profiles.get(selectedProfile);

    profileDetails.add(
      Arrays.asList("Time", DateTimeUtils.formatDateTime(profile.getTime())));

    DataLatLng position = profile.getPosition();

    profileDetails.add(Arrays.asList("Latitude",
      StringUtils.formatNumber(position.getLatitude())));

    profileDetails.add(Arrays.asList("Longitude",
      StringUtils.formatNumber(position.getLongitude())));

    selectedProfileDetails = gson.toJson(profileDetails);

    /**
     * Set up the row IDs for the data table
     */
    profileRowIDs = getCoordinates().stream()
      .filter(c -> profile.matches((ArgoCoordinate) c)).map(c -> c.getId())
      .toList();
  }

  @Override
  public List<Long> getRowIDs() {
    return profileRowIDs;
  }

  @Override
  public void loadDataAction(Progress progress) throws Exception {
    super.loadDataAction(progress);

    // Build profile details
    profiles = new ArrayList<ArgoProfile>();

    ArgoProfile currentProfile = null;

    for (Coordinate c : getCoordinates()) {
      ArgoCoordinate coordinate = (ArgoCoordinate) c;

      // See if we've started a new profile
      if (null != currentProfile && !currentProfile.matches(coordinate)) {
        profiles.add(currentProfile);
        currentProfile = null;
      }

      if (null == currentProfile) {
        currentProfile = new ArgoProfile(coordinate);
      }

      if (null == currentProfile.getTime() && null != coordinate.getTime()) {
        currentProfile.setTime(coordinate.getTime());
      }

      if (null == currentProfile.getPosition()
        && null != getMapPosition(coordinate)) {
        currentProfile.setPosition(getMapPosition(coordinate));
      }
    }

    // Store the last profile
    profiles.add(currentProfile);

    List<List<String>> profileData = new ArrayList<List<String>>(
      profiles.size());
    IntStream.range(0, profiles.size()).forEach(i -> {

      ArgoProfile profile = profiles.get(i);

      profileData.add(
        Arrays.asList(new String[] { String.valueOf(profile.getCycleNumber()),
          String.valueOf(profile.getDirection()),
          String.valueOf(profile.getNProf()) }));
    });

    profileTableData = gson.toJson(profileData);

    // Initialise the profile details now we have loaded them
    buildProfileDetails();
  }

  @Override
  protected void createMap1() throws Exception {
    profilesMap = new ArgoQCMap(this, getDefaultMap1Column(), !dataset.isNrt());
  }

  @Override
  public QCMap getMap1() {
    return profilesMap;
  }

}
