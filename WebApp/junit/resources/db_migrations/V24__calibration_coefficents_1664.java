package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class V24__calibration_coefficents_1664 extends BaseJavaMigration {

  private PreparedStatement searchStmt;

  private PreparedStatement updateStmt;

  Gson gson;

  @Override
  public void migrate(Context context) throws Exception {

    gson = new Gson();
    Connection conn = context.getConnection();

    searchStmt = conn.prepareStatement(
      "SELECT id, coefficients, instrument_id FROM calibration WHERE class = ?");
    updateStmt = conn
      .prepareStatement("UPDATE calibration SET coefficients = ? WHERE id = ?");

    // Do the simple ones first.
    convertPolynomialSensorCalibrations();
    convertCalculationCoefficients();
    convertExternalStandards(conn);
  }

  private void convertPolynomialSensorCalibrations() throws Exception {
    searchStmt.setString(1, "PolynomialSensorCalibration");

    try (ResultSet records = searchStmt.executeQuery()) {
      while (records.next()) {
        String[] coeffs = records.getString(2).split(";");

        JsonObject json = new JsonObject();
        json.addProperty("x⁵", coeffs[0]);
        json.addProperty("x⁴", coeffs[1]);
        json.addProperty("x³", coeffs[2]);
        json.addProperty("x²", coeffs[3]);
        json.addProperty("x", coeffs[4]);
        json.addProperty("Intercept", coeffs[5]);

        updateStmt.setString(1, gson.toJson(json));
        updateStmt.setLong(2, records.getLong(1));
        updateStmt.execute();
      }
    }
  }

  private void convertCalculationCoefficients() throws Exception {
    searchStmt.setString(1, "CalculationCoefficient");

    try (ResultSet records = searchStmt.executeQuery()) {
      while (records.next()) {

        JsonObject json = new JsonObject();
        json.addProperty("Value", records.getString(2));

        updateStmt.setString(1, gson.toJson(json));
        updateStmt.setLong(2, records.getLong(1));
        updateStmt.execute();
      }
    }
  }

  /**
   * Convert external standards. Currently all external standards store both
   * xCO2 and xH2O. We need to remove xH2O if the instrument doesn't have it
   * defined.
   *
   * @param conn
   */
  private void convertExternalStandards(Connection conn) throws Exception {

    searchStmt.setString(1, "ExternalStandard");

    try (ResultSet records = searchStmt.executeQuery()) {

      while (records.next()) {

        boolean hasXH2O = false;

        try (PreparedStatement sensorTypeStmt = conn.prepareStatement(
          "SELECT COUNT(*) FROM file_column WHERE file_definition_id IN "
            + "(SELECT id FROM file_definition WHERE instrument_id = ?) AND "
            + "sensor_type = (SELECT id FROM sensor_types WHERE name = "
            + "'xH₂O (with standards)');")) {

          sensorTypeStmt.setLong(1, records.getLong(3));

          try (ResultSet sensorTypeCount = sensorTypeStmt.executeQuery()) {
            sensorTypeCount.next();
            if (sensorTypeCount.getInt(1) > 0) {
              hasXH2O = true;
            }
          }
        }

        String[] coeffs = records.getString(2).split(";");

        JsonObject json = new JsonObject();
        json.addProperty("xCO₂ (with standards)", coeffs[0]);
        if (hasXH2O) {
          json.addProperty("xH₂O (with standards)", coeffs[1]);
        }

        updateStmt.setString(1, gson.toJson(json));
        updateStmt.setLong(2, records.getLong(1));
        updateStmt.execute();
      }
    }
  }
}
