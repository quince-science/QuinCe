package uk.ac.exeter.QuinCe.database.QC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.data.DataRecordException;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.QCRecord;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;

public class QCDB {

	private static final int QC_RECORDS_COMMON_FIELD_COUNT = 30;

	private static final String CLEAR_QC_STATEMENT = "DELETE FROM qc WHERE data_file_id = ?";
	
	private static final String GET_PRE_QC_RECORDS_STATEMENT = "SELECT r.row, r.co2_type, r.date_time, r.longitude, "
			+ "r.latitude, r.intake_temp_1, r.intake_temp_2, r.intake_temp_3, "
			+ "r.salinity_1, r.salinity_2, r.salinity_3, r.eqt_1, r.eqt_2, r.eqt_3, r.eqp_1, r.eqp_2, r.eqp_3, "
			+ "r.moisture, r.atmospheric_pressure, r.co2, "
			+ "d.mean_intake_temp, d.mean_salinity, d.mean_eqt, d.mean_eqp, d.true_moisture, d.dried_co2, "
			+ "d.calibrated_co2, d.pco2_te_dry, d.ph2o, d.pco2_te_wet, d.fco2_te, d.fco2 "
			+ "FROM raw_data as r "
			+ "INNER JOIN data_reduction as d ON r.data_file_id = d.data_file_id AND r.row = d.row "
			+ "WHERE r.data_file_id = ? ORDER BY r.row ASC";

	private static final String GET_QC_RECORDS_STATEMENT = "SELECT r.row, r.co2_type, r.date_time, r.longitude, "
			+ "r.latitude, r.intake_temp_1, r.intake_temp_2, r.intake_temp_3, "
			+ "r.salinity_1, r.salinity_2, r.salinity_3, r.eqt_1, r.eqt_2, r.eqt_3, r.eqp_1, r.eqp_2, r.eqp_3, "
			+ "r.moisture, r.atmospheric_pressure, r.co2, "
			+ "d.mean_intake_temp, d.mean_salinity, d.mean_eqt, d.mean_eqp, d.true_moisture, d.dried_co2, "
			+ "d.calibrated_co2, d.pco2_te_dry, d.ph2o, d.pco2_te_wet, d.fco2_te, d.fco2, "
			+ "qc.intake_temp_1_used, qc.intake_temp_2_used, qc.intake_temp_3_used, "
			+ "qc.salinity_1_used, qc.salinity_2_used, qc.salinity_3_used, "
			+ "qc.eqt_1_used, qc.eqt_2_used, qc.eqt_3_used, "
			+ "qc.eqp_1_used, qc.eqp_2_used, qc.eqp_3_used, "
			+ "qc.qc_flag, qc.qc_message, qc.woce_flag, qc.woce_message"
			+ "FROM raw_data as r "
			+ "INNER JOIN data_reduction as d ON r.data_file_id = d.data_file_id AND r.row = d.row "
			+ "INNER JOIN qc as q ON d.data_file_id  = q.data_file_id AND d.row = q.row "
			+ "WHERE r.data_file_id = ? ORDER BY r.row ASC";
	
	private static final String ADD_QC_RECORD_STATEMENT = "INSERT INTO qc (data_file_id, row, "
			+ "intake_temp_1_used, intake_temp_2_used, intake_temp_3_used, "
			+ "salinity_1_used, salinity_2_used, salinity_3_used, "
			+ "eqt_1_used, eqt_2_used, eqt_3_used, "
			+ "eqp_1_used, eqp_2_used, eqp_3_used, "
			+ "qc_flag, qc_message, woce_flag, woce_message) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


	public static void clearQCData(DataSource dataSource, long fileId) throws DatabaseException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = dataSource.getConnection();
			
			stmt = conn.prepareStatement(CLEAR_QC_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.execute();
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("An error occurred while clearing out previous data", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Build and retrieve a complete set of QC data for a given data file.
	 * This method assumes that no QC records have yet been created, and creates them.
	 * @param dataSource A data source
	 * @param fileId The data file ID
	 * @param instrument The instrument to which the data file belongs
	 * @return The list of QC records ready to be processed
	 * @throws DatabaseException If the QC records cannot be retrieved, or cannot be created.
	 */
	public static List<QCRecord> getPreQCRecords(DataSource dataSource, long fileId, Instrument instrument) throws DatabaseException {
		
		Connection conn = null;
		PreparedStatement readStatement = null;
		ResultSet records = null;
		List<PreparedStatement> saveQCStatements = new ArrayList<PreparedStatement>();
		List<QCRecord> qcRecords = new ArrayList<QCRecord>();
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			readStatement = conn.prepareStatement(GET_PRE_QC_RECORDS_STATEMENT);
			readStatement.setLong(1,  fileId);
			
			records = readStatement.executeQuery();
			
			while (records.next()) {
				List<String> recordData = new ArrayList<String>();
				
				// Add the raw data and data reduction values
				populateRawAndReductionData(recordData, records);
				
				// Since QC is only just starting, all values are used
				// if the instrument has those sensors
				recordData.add(String.valueOf(instrument.hasIntakeTemp1()));
				recordData.add(String.valueOf(instrument.hasIntakeTemp2()));
				recordData.add(String.valueOf(instrument.hasIntakeTemp3()));
				recordData.add(String.valueOf(instrument.hasSalinity1()));
				recordData.add(String.valueOf(instrument.hasSalinity2()));
				recordData.add(String.valueOf(instrument.hasSalinity3()));
				recordData.add(String.valueOf(instrument.hasEqt1()));
				recordData.add(String.valueOf(instrument.hasEqt2()));
				recordData.add(String.valueOf(instrument.hasEqt3()));
				recordData.add(String.valueOf(instrument.hasEqp1()));
				recordData.add(String.valueOf(instrument.hasEqp2()));
				recordData.add(String.valueOf(instrument.hasEqp3()));
								
				// Store the default QC data in the database
				PreparedStatement qcStatement = conn.prepareStatement(ADD_QC_RECORD_STATEMENT);
				
				qcStatement.setLong(1, fileId);
				qcStatement.setInt(2, records.getInt(1));
				qcStatement.setBoolean(3, instrument.hasIntakeTemp1());
				qcStatement.setBoolean(4, instrument.hasIntakeTemp2());
				qcStatement.setBoolean(5, instrument.hasIntakeTemp3());
				qcStatement.setBoolean(6, instrument.hasSalinity1());
				qcStatement.setBoolean(7, instrument.hasSalinity2());
				qcStatement.setBoolean(8, instrument.hasSalinity3());
				qcStatement.setBoolean(9, instrument.hasEqt1());
				qcStatement.setBoolean(10, instrument.hasEqt2());
				qcStatement.setBoolean(11, instrument.hasEqt3());
				qcStatement.setBoolean(12, instrument.hasEqp1());
				qcStatement.setBoolean(13, instrument.hasEqp2());
				qcStatement.setBoolean(14, instrument.hasEqp3());
				qcStatement.setInt(15, QCRecord.FLAG_NOT_SET);
				qcStatement.setString(16, "");
				qcStatement.setInt(17, QCRecord.FLAG_NOT_SET);
				qcStatement.setString(18, "");
				
				qcStatement.execute();
				saveQCStatements.add(qcStatement);
				
				qcRecords.add(new QCRecord(fileId, recordData, records.getInt(1), instrument));
			}

			conn.commit();
			
		} catch (SQLException|DataRecordException e) {
			throw new DatabaseException("An error occurred while retrieving records for QC", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(readStatement);
			DatabaseUtils.closeStatements(saveQCStatements);
			DatabaseUtils.closeConnection(conn);
		}
		
		return qcRecords;
	}
	
	private static void populateRawAndReductionData(List<String> recordData, ResultSet records) throws SQLException {
		
		// The row and CO2 type are not used in the QC, so skip the first to fields
		for (int i = 3; i <= 2 + QC_RECORDS_COMMON_FIELD_COUNT; i++) {
			recordData.add(records.getString(i));
		}
	}

}
