package db_migrations;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LatitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Migration to rationalise the many fields in the file_definition table into
 * fewer fields containing JSON
 * 
 * @author stevej
 *
 */
public class V16__file_definition_json_1900 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Connection conn = context.getConnection();

    // Add the new columns for the JSON properties
    addColumns(conn);

    // Make the JSON versions of the properties
    makeJson(conn);

    // Remove the unused columns;
    removeColumns(conn);
  }

  /**
   * Add the new columns for the JSON strings
   * 
   * @param conn
   *          A database connection
   * @throws SQLException
   */
  private void addColumns(Connection conn) throws SQLException {

    PreparedStatement lonSpecColStmt = conn.prepareStatement(
      "ALTER TABLE file_definition ADD COLUMN lon_spec MEDIUMTEXT AFTER unix_col");
    lonSpecColStmt.execute();
    lonSpecColStmt.close();

    PreparedStatement latSpecColStmt = conn.prepareStatement(
      "ALTER TABLE file_definition ADD COLUMN lat_spec MEDIUMTEXT AFTER lon_spec");
    latSpecColStmt.execute();
    latSpecColStmt.close();

    PreparedStatement dateTimeSpecColStmt = conn.prepareStatement(
      "ALTER TABLE file_definition ADD COLUMN datetime_spec MEDIUMTEXT AFTER lat_spec");
    dateTimeSpecColStmt.execute();
    dateTimeSpecColStmt.close();
  }

  private void makeJson(Connection conn) throws SQLException, PositionException,
    IOException, DateTimeSpecificationException, ClassNotFoundException {
    Gson gson = new Gson();
    PreparedStatement storeJsonStmt = conn
      .prepareStatement("UPDATE file_definition "
        + "SET lon_spec = ?, lat_spec = ?, datetime_spec = ? WHERE id = ?");

    for (FileDefinition def : getFileDefinitions(conn)) {
      String lonJson = gson.toJson(def.getLongitudeSpecification());
      String latJson = gson.toJson(def.getLatitudeSpecification());
      String dateTimeJson = gson.toJson(def.getDateTimeSpecification());

      storeJsonStmt.setString(1, lonJson);
      storeJsonStmt.setString(2, latJson);
      storeJsonStmt.setString(3, dateTimeJson);
      storeJsonStmt.setLong(4, def.getDatabaseId());
      storeJsonStmt.execute();
    }

    storeJsonStmt.close();
  }

  private void removeColumns(Connection conn) throws SQLException {

    String[] deleteColumns = new String[] { "lon_format", "lon_value_col",
      "lon_hemisphere_col", "lat_format", "lat_value_col", "lat_hemisphere_col",
      "date_time_col", "date_time_props", "date_col", "date_props",
      "hours_from_start_col", "hours_from_start_props", "jday_time_col",
      "jday_col", "year_col", "month_col", "day_col", "time_col", "time_props",
      "hour_col", "minute_col", "second_col", "unix_col" };

    String sql = "ALTER TABLE file_definition DROP COLUMN ";

    for (String col : deleteColumns) {
      PreparedStatement dropColStmt = conn.prepareStatement(sql + col);
      dropColStmt.execute();
    }
  }

  private List<FileDefinition> getFileDefinitions(Connection conn)
    throws SQLException, PositionException, IOException,
    DateTimeSpecificationException, ClassNotFoundException {

    List<FileDefinition> definitions = new ArrayList<FileDefinition>();

    String sql = "SELECT " + "id, description, column_separator, " // 3
      + "header_type, header_lines, header_end_string, column_header_rows, " // 7
      + "column_count, lon_format, lon_value_col, lon_hemisphere_col, " // 11
      + "lat_format, lat_value_col, lat_hemisphere_col, " // 14
      + "date_time_col, date_time_props, date_col, date_props, hours_from_start_col, hours_from_start_props, " // 20
      + "jday_time_col, jday_col, year_col, month_col, day_col, " // 25
      + "time_col, time_props, hour_col, minute_col, second_col, unix_col " // 30
      + "FROM file_definition";

    PreparedStatement stmt = conn.prepareStatement(sql);

    ResultSet records = stmt.executeQuery();

    while (records.next()) {

      long id = records.getLong(1);
      String description = records.getString(2);
      String separator = records.getString(3);
      int headerType = records.getInt(4);
      int headerLines = records.getInt(5);
      String headerEndString = records.getString(6);
      int columnHeaderRows = records.getInt(7);
      int columnCount = records.getInt(8);

      LongitudeSpecification lonSpec = buildLongitudeSpecification(records);
      LatitudeSpecification latSpec = buildLatitudeSpecification(records);
      DateTimeSpecification dateTimeSpec = buildDateTimeSpecification(records);

      FileDefinition fileDefinition = new FileDefinition(id, description,
        separator, headerType, headerLines, headerEndString, columnHeaderRows,
        columnCount, lonSpec, latSpec, dateTimeSpec, null, "TimeDataFile");

      definitions.add(fileDefinition);
    }

    return definitions;
  }

  // Copied from InstrumentDB
  private LongitudeSpecification buildLongitudeSpecification(ResultSet record)
    throws SQLException, PositionException {
    LongitudeSpecification spec = null;

    int format = record.getInt(9);
    int valueColumn = record.getInt(10);
    int hemisphereColumn = record.getInt(11);

    if (format != -1) {
      spec = new LongitudeSpecification(format, valueColumn, hemisphereColumn);
    }
    return spec;
  }

  // Copied from InstrumentDB
  private LatitudeSpecification buildLatitudeSpecification(ResultSet record)
    throws SQLException, PositionException {
    LatitudeSpecification spec = null;

    int format = record.getInt(12);
    int valueColumn = record.getInt(13);
    int hemisphereColumn = record.getInt(14);

    if (format != -1) {
      spec = new LatitudeSpecification(format, valueColumn, hemisphereColumn);
    }

    return spec;
  }

  // Copied from InstrumentDB
  private DateTimeSpecification buildDateTimeSpecification(ResultSet record)
    throws SQLException, IOException, DateTimeSpecificationException {
    int headerLines = record.getInt(5);

    int dateTimeCol = record.getInt(15);
    Properties dateTimeProps = StringUtils
      .propertiesFromString(record.getString(16));
    int dateCol = record.getInt(17);
    Properties dateProps = StringUtils
      .propertiesFromString(record.getString(18));
    int hoursFromStartCol = record.getInt(19);
    Properties hoursFromStartProps = StringUtils
      .propertiesFromString(record.getString(20));
    int jdayTimeCol = record.getInt(21);
    int jdayCol = record.getInt(22);
    int yearCol = record.getInt(23);
    int monthCol = record.getInt(24);
    int dayCol = record.getInt(25);
    int timeCol = record.getInt(26);
    Properties timeProps = StringUtils
      .propertiesFromString(record.getString(27));
    int hourCol = record.getInt(28);
    int minuteCol = record.getInt(29);
    int secondCol = record.getInt(30);
    int unixCol = record.getInt(31);

    return new DateTimeSpecification(headerLines > 0, dateTimeCol,
      dateTimeProps, dateCol, dateProps, hoursFromStartCol, hoursFromStartProps,
      jdayTimeCol, jdayCol, yearCol, monthCol, dayCol, timeCol, timeProps,
      hourCol, minuteCol, secondCol, unixCol);
  }
}
