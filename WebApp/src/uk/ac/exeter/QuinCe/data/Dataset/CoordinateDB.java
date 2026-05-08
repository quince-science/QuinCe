package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for storing and accessing {@link Coordinate} objects in the database.
 */
public class CoordinateDB {

  private static final String STORE_ARGO_COORDINATE_STMT = "INSERT INTO coordinates "
    + "(dataset_id, date, cycle_number, nprof, direction, nlevel, pres, source_file) "
    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String STORE_TIME_COORDINATE_STMT = "INSERT INTO coordinates "
    + "(dataset_id, date) VALUES (?, ?)";

  private static final String GET_SENSOR_VALUE_COORDINATES_QUERY = "SELECT "
    + "sv.coordinate_id FROM sensor_values sv "
    + "INNER JOIN coordinates c ON c.id = sv.coordinate_id "
    + "WHERE c.dataset_id = ?";

  private static final String GET_MEASUREMENT_COORDINATES_QUERY = "SELECT "
    + "m.coordinate_id FROM measurements m "
    + "INNER JOIN coordinates c ON c.id = m.coordinate_id "
    + "WHERE c.dataset_id = ?";

  private static final String GET_COORDINATES_QUERY = "SELECT "
    + "id, dataset_id, date, depth, station, cast, bottle, replicate, "
    + "cycle_number, nprof, direction, nlevel, pres, source_file "
    + "FROM coordinates WHERE id IN " + DatabaseUtils.IN_PARAMS_TOKEN;

  /**
   * Store the provided {@link Coordinate} in the database.
   *
   * @param conn
   *          A database connection.
   * @param coordinate
   *          The coordinate to be stored.
   * @throws CoordinateException
   *           If the coordinate is invalid.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws RecordNotFoundException
   * @see #saveCoordinates(Connection, Collection)
   */
  protected static void saveCoordinate(Connection conn, Coordinate coordinate)
    throws CoordinateException, DatabaseException, RecordNotFoundException {
    saveCoordinates(conn, Collections.singleton(coordinate));
  }

  /**
   * Store the provided {@link Coordinate}s in the database.
   *
   * <p>
   * This is a wrapper around {@link #saveCoordinates(Connection, Collection)}.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param coordinates
   *          The coordinates.
   * @throws CoordinateException
   *           If any of the coordinates are invalid.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws RecordNotFoundException
   * @see #saveCoordinates(Connection, Collection)
   */
  protected static void saveCoordinates(Connection conn,
    Coordinate... coordinates)
    throws CoordinateException, DatabaseException, RecordNotFoundException {
    saveCoordinates(conn, Arrays.asList(coordinates));
  }

  /**
   * Store the provided {@link Coordinate}s in the database.
   *
   * <p>
   * A {@link Coordinate} will only be saved if it does not already have a
   * database ID; otherwise it will be ignored. {@link Coordinate}s without
   * database IDs will be stored, and their {@code id}s updated with the
   * generated database keys.
   * </p>
   *
   * <p>
   * <b>Note:</b> It is usually desirable that this method is called as part of
   * a larger transaction. It is up to the caller to work this out: this method
   * does not change the commit status of the supplied {@link Connection}, nor
   * does it perform any explicit commit action.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param coordinates
   *          The coordinates to be stored.
   * @throws CoordinateException
   *           If the coordinates are not all of the same type, or any
   *           coordinate is invalid.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws RecordNotFoundException
   */
  protected static void saveCoordinates(Connection conn,
    Collection<Coordinate> coordinates)
    throws CoordinateException, DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(coordinates, "coordinates", false);

    if (coordinates.size() > 0) {
      // Make sure all coordinates are of the same type
      if (coordinates.stream().map(c -> c.getType()).distinct().limit(2)
        .count() > 1) {
        throw new CoordinateException(
          "All coordinates must be of the same type");
      }

      if (coordinates.stream().map(c -> c.getDatasetId()).distinct().limit(2)
        .count() > 1) {
        throw new CoordinateException(
          "All coordinates must be for the same DataSet");
      }

      if (!DataSetDB.datasetExists(conn,
        coordinates.stream().findAny().get().getDatasetId())) {
        throw new RecordNotFoundException("DataSet does not exist");
      }

      switch (coordinates.stream().findAny().get().getType()) {
      case Instrument.BASIS_TIME: {
        storeTimeCoordinates(conn, coordinates);
        break;
      }
      case Instrument.BASIS_ARGO: {
        storeArgoCoordinates(conn, coordinates);
        break;
      }
      default: {
        throw new CoordinateException("Unrecognised coordinate type");
      }
      }
    }
  }

  /**
   * Store the provided surface coordinates in the database.
   *
   * @param conn
   * @param coordinates
   * @throws DatabaseException
   * @throws CoordinateException
   */
  private static void storeTimeCoordinates(Connection conn,
    Collection<Coordinate> coordinates)
    throws DatabaseException, CoordinateException {

    try (
      PreparedStatement stmt = conn.prepareStatement(STORE_TIME_COORDINATE_STMT,
        Statement.RETURN_GENERATED_KEYS)) {

      for (Coordinate coordinate : coordinates) {
        if (coordinate.getId() == DatabaseUtils.NO_DATABASE_RECORD) {
          stmt.setLong(1, coordinate.getDatasetId());
          stmt.setLong(2, DateTimeUtils.dateToLong(coordinate.getTime()));

          stmt.execute();

          try (ResultSet keys = stmt.getGeneratedKeys()) {
            keys.next();
            coordinate.setId(keys.getLong(1));
          }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing coordinates", e);
    }
  }

  /**
   * Store the provided surface coordinates in the database
   *
   * @param conn
   * @param coordinates
   * @throws DatabaseException
   * @throws CoordinateException
   */
  private static void storeArgoCoordinates(Connection conn,
    Collection<Coordinate> coordinates)
    throws DatabaseException, CoordinateException {

    try (
      PreparedStatement stmt = conn.prepareStatement(STORE_ARGO_COORDINATE_STMT,
        Statement.RETURN_GENERATED_KEYS)) {

      for (Coordinate coordinate : coordinates) {
        if (coordinate.getId() == DatabaseUtils.NO_DATABASE_RECORD) {

          ArgoCoordinate coord = (ArgoCoordinate) coordinate;

          stmt.setLong(1, coordinate.getDatasetId());

          if (null == coordinate.getTime()) {
            stmt.setNull(2, Types.BIGINT);
          } else {
            stmt.setLong(2, DateTimeUtils.dateToLong(coordinate.getTime()));
          }

          stmt.setLong(3, coord.getCycleNumber());
          stmt.setLong(4, coord.getNProf());
          stmt.setString(5, String.valueOf(coord.getDirection()));
          stmt.setLong(6, coord.getNLevel());
          stmt.setDouble(7, coord.getPres());
          stmt.setString(8, coord.getSourceFile());

          stmt.execute();

          try (ResultSet keys = stmt.getGeneratedKeys()) {
            keys.next();
            coordinate.setId(keys.getLong(1));
          }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing coordinates", e);
    }
  }

  /**
   * Retrieve the {@link Coordinates} of the {@link SensorValue}s in a
   * {@link DataSet}.
   *
   * @param conn
   *          A database connection.
   * @param dataset
   *          The DataSet
   * @return The retrieved coordinates.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws CoordinateException
   *           If any Coordinate object cannot be constructed.
   */
  public static Map<Long, Coordinate> getSensorValueCoordinates(Connection conn,
    DataSet dataset)
    throws DatabaseException, RecordNotFoundException, CoordinateException {

    Set<Long> coordinateIds = new HashSet<Long>();

    try {
      // SensorValue coordinates
      try (PreparedStatement sensorValuesStmt = conn
        .prepareStatement(GET_SENSOR_VALUE_COORDINATES_QUERY)) {

        sensorValuesStmt.setLong(1, dataset.getId());

        try (ResultSet records = sensorValuesStmt.executeQuery()) {
          while (records.next()) {
            coordinateIds.add(records.getLong(1));
          }
        }
      }

      return getCoordinates(conn, dataset.getInstrument().getBasis(),
        coordinateIds);
    } catch (SQLException e) {
      throw new DatabaseException("Error retrieving coordinates", e);
    }
  }

  /**
   * Retrieve the {@link Coordinates} of the {@link Measurements}
   * {@link DataSet}.
   *
   * <p>
   * This includes the DataSet, SensorValue and Measurement records.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param dataset
   *          The DataSet
   * @return The retrieved coordinates.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws CoordinateException
   *           If any Coordinate object cannot be constructed.
   */
  public static Map<Long, Coordinate> getMeasurementCoordinates(Connection conn,
    DataSet dataset)
    throws DatabaseException, RecordNotFoundException, CoordinateException {

    Set<Long> coordinateIds = new HashSet<Long>();

    try {
      try (PreparedStatement measurementsStmt = conn
        .prepareStatement(GET_MEASUREMENT_COORDINATES_QUERY)) {

        measurementsStmt.setLong(1, dataset.getId());

        try (ResultSet records = measurementsStmt.executeQuery()) {
          while (records.next()) {
            coordinateIds.add(records.getLong(1));
          }
        }
      }

      return getCoordinates(conn, dataset.getInstrument().getBasis(),
        coordinateIds);
    } catch (SQLException e) {
      throw new DatabaseException("Error retrieving coordinates", e);
    }
  }

  /**
   * Retrieve the specified {@link Coordinate}s from the database.
   *
   * <p>
   * The {@link Coordinate}s are returned as a {@link Map} of
   * {@code Coordinate ID -> Coordinate}.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param basis
   *          The measurement basis for the underlying instrument.
   * @param coordinateIds
   *          The coordinates' database IDs.
   * @return The coordinates.
   * @throws SQLException
   *           If a database error occurs.
   * @throws CoordinateException
   *           If any coordinate cannot be constructed.
   */
  private static Map<Long, Coordinate> getCoordinates(Connection conn,
    int basis, Collection<Long> coordinateIds)
    throws SQLException, CoordinateException {

    Map<Long, Coordinate> result = new HashMap<Long, Coordinate>();

    List<Long> uniqueIds = coordinateIds.stream().distinct().toList();

    String sql = DatabaseUtils.makeInStatementSql(GET_COORDINATES_QUERY,
      uniqueIds.size());

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

      int inIndex = 1;
      for (long id : coordinateIds.stream().distinct().toList()) {
        stmt.setLong(inIndex, id);
        inIndex++;
      }

      try (ResultSet records = stmt.executeQuery()) {
        while (records.next()) {
          Coordinate coordinate = coordinateFromRecord(records, basis);
          result.put(coordinate.getId(), coordinate);
        }
      }
    }
    return result;
  }

  /**
   * Retrieve a {@link Coordinate} from a {@link ResultSet}.
   *
   * <p>
   * If {@code loadedCoordinates} is supplied, this is presumed to be a cache of
   * already retrieved {@link Coordinate}s. If the coordinate ID in the record
   * is already in this Map, the value from the Map is returned without reading
   * the record.
   * </p>
   *
   * @param record
   *          The record to be read.
   * @param loadedCoordinates
   *          Optional cache of previously loaded {@link Coordinate}s.
   * @param basis
   *          The measurement basis for the instrument.
   * @param datasetId
   *          The ID of the dataset.
   * @return The retrieved coordinate.
   * @throws SQLException
   *           If a database error occurs.
   * @throws CoordinateException
   *           If the {@link Coordinate} object cannot be constructed.
   */
  private static Coordinate coordinateFromRecord(ResultSet record, int basis)
    throws SQLException, CoordinateException {

    Coordinate result;

    long coordinateId = record.getLong(1);
    long datasetId = record.getLong(2);

    switch (basis) {
    case Instrument.BASIS_TIME: {
      result = new TimeCoordinate(coordinateId, datasetId,
        DateTimeUtils.longToDate(record.getLong(3)));
      break;
    }
    case Instrument.BASIS_ARGO: {

      LocalDateTime timestamp = null;
      long millis = record.getLong(3);
      if (!record.wasNull()) {
        timestamp = DateTimeUtils.longToDate(millis);
      }

      result = new ArgoCoordinate(coordinateId, datasetId, record.getInt(9),
        record.getInt(10), record.getString(11).charAt(0), record.getInt(12),
        record.getDouble(13), record.getString(14), timestamp);

      break;
    }
    default: {
      throw new CoordinateException("Basis not recognised");
    }
    }

    return result;
  }
}
