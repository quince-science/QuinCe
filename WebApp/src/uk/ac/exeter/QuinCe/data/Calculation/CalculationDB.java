package uk.ac.exeter.QuinCe.data.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import uk.ac.exeter.QCRoutines.data.NoSuchColumnException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.InvalidFlagException;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.RebuildCode;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.VariableList;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class for dealing with database calls related to calculation data
 * @author Steve Jones
 *
 */
public abstract class CalculationDB {

  /**
   * Get the name of the database table where calculation data is stored
   * @return The table name
   */
  public abstract String getCalculationTable();

  /**
   * Create a calculation record for the given measurement record
   * @param conn A database connection
   * @param measurementId The measurement's ID
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public void createCalculationRecord(Connection conn, long measurementId) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");

    PreparedStatement statement = null;

    try {
      statement = getInsertStatement(conn);

      statement.setLong(1, measurementId);
      statement.setInt(2, Flag.VALUE_NOT_SET);
      statement.setNull(3, Types.VARCHAR);
      statement.setInt(4, Flag.VALUE_NOT_SET);
      statement.setNull(5, Types.VARCHAR);

      statement.execute();

    } catch (SQLException e) {
      throw new DatabaseException("Error while creating calculation record", e);
    } finally {
      DatabaseUtils.closeStatements(statement);
    }
  }

  /**
   * Generate the insert statement for a new calculation record
   * @param conn A database connection
   * @return The insert statement
   * @throws MissingParamException If any required parameters are missing
   * @throws SQLException If the statement cannot be created
   */
  private PreparedStatement getInsertStatement(Connection conn) throws MissingParamException, SQLException {

    List<String> fields = new ArrayList<String>();

    fields.add("measurement_id");
    fields.add("auto_flag");
    fields.add("auto_message");
    fields.add("user_flag");
    fields.add("user_message");

    return DatabaseUtils.createInsertStatement(conn, getCalculationTable(), fields);
  }

  /**
   * Delete the calculation data for a given data set
   * @param conn A database connection
   * @param dataSet The data set
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public void deleteDatasetCalculationData(Connection conn, DataSet dataSet) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataSet, "dataSet");

    PreparedStatement stmt = null;

    try {
      // TODO I think this could be done better. But maybe not.
      String deleteStatement = "DELETE c.* FROM " + getCalculationTable() + " AS c INNER JOIN dataset_data AS d ON c.measurement_id = d.id WHERE d.dataset_id = ?";

      stmt = conn.prepareStatement(deleteStatement);
      stmt.setLong(1, dataSet.getId());

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting dataset data", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Get the Automatic QC flag for a measurement
   * @param conn A database connection
   * @param measurementId The measurement ID
   * @return The automatic QC flag
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the measurement does not exist
   * @throws InvalidFlagException The the flag value is invalid
   */
  public Flag getAutoQCFlag(Connection conn, long measurementId) throws MissingParamException, DatabaseException, RecordNotFoundException, InvalidFlagException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");

    Flag result = null;
    PreparedStatement stmt = null;
    ResultSet record = null;

    try {
      // TODO I think this could be done better. But maybe not.
      String flagStatement = "SELECT auto_flag FROM " + getCalculationTable() + " WHERE measurement_id = ?";

      stmt = conn.prepareStatement(flagStatement);
      stmt.setLong(1, measurementId);

      record = stmt.executeQuery();
      if (!record.next()) {
        throw new RecordNotFoundException("Cannot find calculation record", getCalculationTable(), measurementId);
      } else {
        result = new Flag(record.getInt(1));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving QC flag", e);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Get the automatic QC messages for a given measurement
   * @param conn A datbase connection
   * @param measurementId The measurement ID
   * @return The QC messages
   * @throws MessageException If the messages cannot be parsed
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the measurement cannot be found
   */
  public List<Message> getQCMessages(Connection conn, long measurementId) throws MessageException, DatabaseException, RecordNotFoundException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");

    PreparedStatement stmt = null;
    ResultSet record = null;
    List<Message> result = null;

    try {
      // TODO I think this could be done better. But maybe not.
      String query = "SELECT auto_message FROM " + getCalculationTable() + " WHERE measurement_id = ?";

      stmt = conn.prepareStatement(query);

      stmt.setLong(1, measurementId);
      record = stmt.executeQuery();

      if (!record.next()) {
        throw new RecordNotFoundException("Cannot find calculation record", getCalculationTable(), measurementId);
      } else {
        result = RebuildCode.getMessagesFromRebuildCodes(record.getString(1));
      }

      return result;
    } catch (SQLException e) {
      throw new DatabaseException("An error occurred while retrieving QC messages", e);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Store the QC information for a given record
   * @param conn A database connection
   * @param record The record
   * @throws MessageException If the messages cannot be serialized for storage
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public void storeQC(Connection conn, CalculationRecord record) throws MissingParamException, DatabaseException, MessageException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(record, "record");

    PreparedStatement stmt = null;

    try {
      // TODO I think this could be done better. But maybe not.
      String sql = "UPDATE " + getCalculationTable() + " SET auto_flag = ?, "
          + "auto_message = ?, user_flag = ?, user_message = ? "
          + "WHERE measurement_id = ?";

      stmt = conn.prepareStatement(sql);

      stmt.setInt(1, record.getAutoFlag().getFlagValue());
      String rebuildCodes = RebuildCode.getRebuildCodes(record.getAutoQCMessages());
      if (null == rebuildCodes || rebuildCodes.length() == 0) {
        stmt.setNull(2, Types.VARCHAR);
      } else {
        stmt.setString(2, RebuildCode.getRebuildCodes(record.getAutoQCMessages()));
      }

      stmt.setInt(3, record.getUserFlag().getFlagValue());
      String userMessage = record.getUserMessage();
      if (null == userMessage || userMessage.length() == 0) {
        stmt.setNull(4, Types.VARCHAR);
      } else {
        stmt.setString(4, record.getUserMessage());
      }
      stmt.setLong(5, record.getLineNumber());

      stmt.execute();

    } catch (SQLException e) {
      throw new DatabaseException("Error while storing QC info", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Store the calculation values for a given measurement. This method
   * must only update an existing record in the database.
   * @param conn A database connection
   * @param measurementId The measurement's database ID
   * @param values The values to be stored
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public abstract void storeCalculationValues(Connection conn, long measurementId, Map<String, Double> values) throws MissingParamException, DatabaseException;

  /**
   * Add the calculation values to a {@link CalculationRecord}
   * @param dataSource A data source
   * @param record The record for which values should be retrieved
   * @return The calculation values
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the record does not exist
   * @throws MessageException If the automatic QC messages cannot be parsed
   * @throws NoSuchColumnException If the automatic QC messages cannot be parsed
   */
  public Map<String, Double> getCalculationValues(DataSource dataSource, CalculationRecord record) throws MissingParamException, DatabaseException, RecordNotFoundException, NoSuchColumnException, MessageException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(record, "record");

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      return getCalculationValues(conn, record);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting calculation values", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Add the calculation values to a {@link CalculationRecord}
   * @param conn A database connection
   * @param record The record for which values should be retrieved
   * @return The calculation values
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the record does not exist
   * @throws MessageException If the automatic QC messages cannot be parsed
   * @throws NoSuchColumnException If the automatic QC messages cannot be parsed
   */
  public abstract Map<String, Double> getCalculationValues(Connection conn, CalculationRecord record) throws MissingParamException, DatabaseException, RecordNotFoundException, NoSuchColumnException, MessageException;

  /**
   * Clear the calculation values for a given measurement. This method
   * must only update an existing record in the database.
   * @param conn A database connection
   * @param measurementId The measurement's database ID
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public abstract void clearCalculationValues(Connection conn, long measurementId) throws MissingParamException, DatabaseException;

  /**
   * Get the list of column headings for calculation fields
   * @return The column headings
   */
  public abstract List<String> getCalculationColumnHeadings();

  /**
   * Add the calculation variables to a variables list used
   * for selecting plots and maps
   * @param variables The variables list to be populated
   * @throws MissingParamException If the variable list is null
   */
  public abstract void populateVariableList(VariableList variables) throws MissingParamException;

  /**
   * Get the list of measurement IDs for a dataset that can be manipulated by the user.
   * This is basically all the IDs that have not been flagged as FATAL or INGNORED.
   * @param dataSource A data source
   * @param datasetId The dataset ID
   * @return The selectable measurement IDs
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
     */
  public List<Long> getSelectableMeasurementIds(DataSource dataSource, long datasetId) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    List<Long> ids = new ArrayList<Long>();

    try {
      conn = dataSource.getConnection();

      String sql = "SELECT c.measurement_id FROM "
        + getCalculationTable()
        + " c INNER JOIN dataset_data d ON c.measurement_id = d.id "
        + " WHERE d.dataset_id = ? AND c.user_flag NOT IN ("
        + Flag.VALUE_FATAL
        + ","
        + Flag.VALUE_IGNORED
        + ") ORDER BY c.measurement_id ASC";

      stmt = conn.prepareStatement(sql);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();

      while (records.next()) {
        ids.add(records.getLong(1));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting measurement IDs", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return ids;
  }

  /**
   * Accept the automatic QC flag as the final QC result for a set of rows
   * @param dataSource A data source
   * @param rows The rows' database IDs
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
     * @throws MessageException If the automatic QC messages cannot be extracted
   */
  public void acceptAutoQc(DataSource dataSource, List<Long> rows) throws MissingParamException, DatabaseException, MessageException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(rows, "rows", true);

    Connection conn = null;
    List<PreparedStatement> statements = new ArrayList<PreparedStatement>(rows.size() + 1);
    ResultSet records = null;

    String readSql = "SELECT measurement_id, auto_flag, auto_message FROM "
        + getCalculationTable()
        + " WHERE measurement_id IN ("
        + StringUtils.listToDelimited(rows, ",")
        + ")";

    String writeSql = "UPDATE "
        + getCalculationTable()
        + " SET user_flag = ?, user_message = ?"
        + " WHERE measurement_id = ?";

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      PreparedStatement readStatement = conn.prepareStatement(readSql);
      statements.add(readStatement);

      records = readStatement.executeQuery();

      while (records.next()) {

        long id = records.getLong(1);
        int flag = records.getInt(2);
        List<Message> messages = RebuildCode.getMessagesFromRebuildCodes(records.getString(3));

        StringBuilder outputMessages = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
          outputMessages.append(messages.get(i).getShortMessage());
          if (i < messages.size() - 1) {
            outputMessages.append(';');
          }
        }

        PreparedStatement writeStatement = conn.prepareStatement(writeSql);
        statements.add(writeStatement);

        writeStatement.setInt(1, flag);
        writeStatement.setString(2, outputMessages.toString());
        writeStatement.setLong(3, id);

        writeStatement.execute();
      }

      conn.commit();

    } catch (SQLException e) {
      DatabaseUtils.rollBack(conn);
      throw new DatabaseException("Error while accepting auto QC", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(statements);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Retrieve a set of comments for a specified set of rows in a data file.
   * The comments are grouped by their comment string. Each string also has the number
   * of times that comment appeared, along with the 'worst' flag assigned to that comment.
   *
   * Both QC comments and user comments are included in the list.
   *
   * @param dataSource A data source
   * @param rows The rows for which comments must be retrieved.
   * @return The comments
   * @throws DatabaseException If a database error occurs
   * @throws InvalidFlagException If a flag retrieved from the database is invalid
   * @throws MessageException If a QC message cannot be reconstructed from its rebuild code
   * @throws MissingParamException If any required parameters are missing
   */
  public CommentSet getCommentsForRows(DataSource dataSource, List<Long> rows) throws DatabaseException, InvalidFlagException, MessageException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(rows, "rows");

    CommentSet result = new CommentSet();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();

      String sql = "SELECT auto_message, user_message, user_flag FROM "
          + getCalculationTable()
          + " WHERE measurement_id IN ("
          + StringUtils.listToDelimited(rows, ",")
          + ")";

      stmt = conn.prepareStatement(sql);
      records = stmt.executeQuery();
      while (records.next()) {

        String autoMessages = records.getString(1);
        String userMessage = records.getString(2);
        Flag userFlag = new Flag(records.getInt(3));

        for (Message message : RebuildCode.getMessagesFromRebuildCodes(autoMessages)) {
          if (userFlag.equals(Flag.NEEDED) || !message.getShortMessage().equalsIgnoreCase(userMessage)) {
            result.addComment(message.getShortMessage(), message.getFlag());
          }
        }

        if (!userFlag.equals(Flag.NEEDED) && null != userMessage && userMessage.length() > 0) {
          result.addComment(userMessage, userFlag);
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving comments", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Apply a manual flag and comment to a set of rows
   * @param dataSource A data source
   * @param rows The rows to be updated
   * @param flag The flag
   * @param comment The comment
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
     * @throws InvalidFlagException If the flag value is invalid
   */
  public void applyManualFlag(DataSource dataSource, List<Long> rows, int flag, String comment) throws DatabaseException, MissingParamException, InvalidFlagException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(rows, "rows");
    if (!Flag.isValidFlagValue(flag)) {
      throw new InvalidFlagException(flag);
    }
    if (flag != Flag.VALUE_GOOD) {
      MissingParam.checkMissing(comment, "comment");
    }

    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      conn = dataSource.getConnection();

      String sql = "UPDATE "
          + getCalculationTable()
          + " SET user_flag = ?, user_message = ? WHERE measurement_id IN ("
          + StringUtils.listToDelimited(rows, ",")
          + ")";

      stmt = conn.prepareStatement(sql);
      stmt.setInt(1, flag);
      stmt.setString(2, comment);

      stmt.execute();

    } catch (SQLException e) {
      throw new DatabaseException("Error while setting user flags", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }


  /**
   * Get a JSON data array for a dataset with the given fields
   * @param dataSource A data source
   * @param dataset The dataset
   * @param fields The fields to retrieve
   * @return The JSON array
   * @throws InstrumentException If the dataset's instrument cannot be retrieved
   * @throws RecordNotFoundException If the dataset does not exist
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public String getJsonData(DataSource dataSource, DataSet dataset, List<String> fields, String sortField) throws DatabaseException, MissingParamException, RecordNotFoundException, InstrumentException {
    return getJsonData(dataSource, dataset, fields, sortField, null, false);
  }

  /**
   * Get a JSON data array for a dataset with the given fields
   * @param dataSource A data source
   * @param dataset The dataset
   * @param fields The fields to retrieve
   * @param sortField The field used to sort the data. If {@code null}, default ordering will be used
   * @param bounds The geographical limits of the query
   * @param limitPoints Indicates whether the number of points returned should be limited
   * @return The JSON array
   * @throws InstrumentException If the dataset's instrument cannot be retrieved
   * @throws RecordNotFoundException If the dataset does not exist
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public String getJsonData(DataSource dataSource, DataSet dataset, List<String> fields, String sortField, List<Double> bounds, boolean limitPoints) throws DatabaseException, MissingParamException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataset, "dataset");
    MissingParam.checkMissing(fields, "fields");

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;


    StringBuilder json = new StringBuilder();

    try {
      conn = dataSource.getConnection();

      List<String> datasetFields = DataSetDataDB.extractDatasetFields(conn, dataset, fields);
      List<String> calculationFields = new ArrayList<String>(fields);
      calculationFields.removeAll(datasetFields);

      StringBuilder sql = new StringBuilder();

      sql.append("SELECT ");
      for (int i = 0; i < fields.size(); i++) {
        String field = fields.get(i);

        if (datasetFields.contains(field)) {
          sql.append('d');
        } else {
          sql.append('c');
        }

        sql.append('.');
        sql.append(field);

        if (i < fields.size() - 1) {
          sql.append(',');
        }
      }

      sql.append(" FROM dataset_data d INNER JOIN ");
      sql.append(getCalculationTable());
      sql.append(" c ON d.id = c.measurement_id WHERE d.dataset_id = ?");

      if (null != bounds) {
        sql.append(" AND d.longitude >= ");
        sql.append(bounds.get(0));
        sql.append(" AND d.longitude <= ");
        sql.append(bounds.get(2));
        sql.append(" AND d.latitude >= ");
        sql.append(bounds.get(1));
        sql.append(" AND d.latitude <= ");
        sql.append(bounds.get(3));
      }

      if (null != sortField) {
        sql.append(" ORDER BY ");
        if (datasetFields.contains(sortField)) {
          sql.append('d');
        } else {
          sql.append('c');
        }

        sql.append('.');
        sql.append(sortField);
      }

      stmt = conn.prepareStatement(sql.toString());
      stmt.setLong(1, dataset.getId());

      records = stmt.executeQuery();
      int recordCount = 0;
      try {
          records.last();
          recordCount = records.getRow();
          records.beforeFirst();
      } catch (SQLException e) {
        recordCount = 0;
      }

      int everyNthRecord = 1;
      if (limitPoints) {
        int maxPoints = Integer.parseInt(ResourceManager.getInstance().getConfig().getProperty("map.max_points"));
        everyNthRecord = recordCount / maxPoints;
        if (everyNthRecord < 1) {
          everyNthRecord = 1;
        }
      }

      json.append('[');

      boolean hasRecords = false;
      while (records.relative(everyNthRecord)) {
        hasRecords = true;

        json.append('[');

        for (int i = 0; i < fields.size(); i++) {

          if (fields.get(i).equals("id") || fields.get(i).equals("date")) {
            json.append(records.getLong(i + 1));
          } else {
            json.append(records.getDouble(i + 1));
          }

          if (i < fields.size() - 1) {
            json.append(',');
          }
        }

        json.append("],");
      }

      // Remove the trailing comma
      if (hasRecords) {
        json.deleteCharAt(json.length() - 1);
      }
      json.append(']');

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting dataset records", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return json.toString();
  }

  /**
   * Get the range of values from a specific field in a dataset.
   * The returned range covers the 5th to 95th percentiles of the true range,
   * to prevent outliers from skewing color scales based on the range.
   *
   * By default, only records with flags of GOOD or QUESTIONABLE are included in
   * the range; if there are no records with these flags, all records will be included.
   *
   * @param dataSource A data source
   * @param dataset The dataset
   * @param field The field
   * @return The value range
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If one or more parameters are missing
   * @throws RecordNotFoundException If the dataset does not exist
   * @throws InstrumentException If the instrument details cannot be retrieved
   */
  public List<Double> getValueRange(DataSource dataSource, DataSet dataset, String field) throws DatabaseException, MissingParamException, RecordNotFoundException, InstrumentException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataset, "dataset");
    MissingParam.checkMissing(field, "field");

    List<Double> result = null;
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = getValueRange(conn, dataset, field, true);
    } catch (SQLException e) {
      throw new DatabaseException("Excpeption while getting scale bounds", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }


    return result;
  }

  /**
   * The real calculation method for getting the range of values for a field
   * in a dataset (see {@link #getValueRange(DataSource, DataSet, String)}).
   *
   * This method should be called with {@code filterFlags} set to {@code true},
   * which will force it to only check records with GOOD or QUESTIONABLE flags.
   * If no records are found, it will call itself again with {@code filterFlags == false}
   * to collect all records regardless of the flag.
   *
   * @param conn A database connection
   * @param dataset The dataset
   * @param field The field
   * @param filterFlags Indicates whether or not on GOOD or QUESTIONABLE flags should be checked
   * @return The value range
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If one or more parameters are missing
   * @throws RecordNotFoundException If the dataset does not exist
   * @throws InstrumentException If the instrument details cannot be retrieved
   */
  private List<Double> getValueRange(Connection conn, DataSet dataset, String field, boolean filterFlags) throws DatabaseException, MissingParamException, RecordNotFoundException, InstrumentException {
    PreparedStatement stmt = null;
    ResultSet results = null;

    List<Double> result = new ArrayList<Double>(2);

    try {
      List<String> fieldList = new ArrayList<String>(1);
      fieldList.add(field);
      List<String> datasetFields = DataSetDataDB.extractDatasetFields(conn, dataset, fieldList);

      StringBuilder sql = new StringBuilder();

      sql.append("SELECT ");
      if (datasetFields.size() > 0) {
        sql.append('d');
      } else {
        sql.append('c');
      }

      sql.append('.');
      sql.append(field);

      sql.append(" FROM dataset_data d INNER JOIN ");
      sql.append(getCalculationTable());
      sql.append(" c ON d.id = c.measurement_id WHERE d.dataset_id = ?");

      if (filterFlags) {
        sql.append(" AND c.user_flag IN (");
        sql.append(Flag.VALUE_ASSUMED_GOOD);
        sql.append(',');
        sql.append(Flag.VALUE_GOOD);
        sql.append(',');
        sql.append(Flag.VALUE_QUESTIONABLE);
        sql.append(")");
      }

      stmt = conn.prepareStatement(sql.toString());
      stmt.setLong(1, dataset.getId());
      results = stmt.executeQuery();

      List<Double> values = new ArrayList<Double>();

      boolean valuesFound = false;
      while(results.next()) {
        valuesFound = true;
        values.add(results.getDouble(1));
      }

      if (valuesFound) {
        Percentile percentile = new Percentile();

        double[] array = values.stream().mapToDouble(d -> d).toArray();

        // The minimum and maximum are the 5th and 95th percentile points
        result.add(percentile.evaluate(array, 5));
        result.add(percentile.evaluate(array, 95));
      } else {
        // Get the values including all flags, because there are no
        // records with good/questionable flags
        result = getValueRange(conn, dataset, field, false);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Excpeption while getting scale bounds", e);
    } finally {
      DatabaseUtils.closeResultSets(results);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Check a list of column headings to ensure that they are all recognised
   * as part of this calculation DB.
   * @param headings The headings to be checked
   * @throws CalculatorException If the heading is not recognised
   */
  public void validateColumnHeadings(List<String> headings) throws CalculatorException {
    for (String heading : headings) {
      if (!getCalculationColumnHeadings().contains(heading)) {
        throw new CalculatorException("Unrecognised heading '" + heading + "'");
      }
    }
  }
}
