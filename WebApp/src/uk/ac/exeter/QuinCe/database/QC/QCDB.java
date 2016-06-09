package uk.ac.exeter.QuinCe.database.QC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.config.ColumnConfig;
import uk.ac.exeter.QCRoutines.data.DataRecordException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.InvalidFlagException;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.RebuildCode;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.QCRecord;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;

public class QCDB {

	private static final int FIELD_ROW_NUMBER = 1;
	
	private static final int FIELD_QC_FLAG = 2;
	
	private static final int FIELD_QC_COMMENT = 3;
	
	private static final int FIELD_WOCE_FLAG = 4;
	
	private static final int FIELD_WOCE_COMMENT = 5;
	
	private static final int FIRST_DATA_FIELD = 6;

	private static final String CLEAR_QC_STATEMENT = "DELETE FROM qc WHERE data_file_id = ?";
	
	private static final String GET_QC_RECORDS_STATEMENT = "SELECT r.row, q.qc_flag, q.qc_message, q.woce_flag, q.woce_message, "
			+ "r.co2_type, r.date_time, r.longitude, "
			+ "r.latitude, r.intake_temp_1, r.intake_temp_2, r.intake_temp_3, "
			+ "r.salinity_1, r.salinity_2, r.salinity_3, r.eqt_1, r.eqt_2, r.eqt_3, r.eqp_1, r.eqp_2, r.eqp_3, "
			+ "r.moisture, r.atmospheric_pressure, r.co2, "
			+ "d.mean_intake_temp, d.mean_salinity, d.mean_eqt, d.mean_eqp, d.true_moisture, d.dried_co2, "
			+ "d.calibrated_co2, d.pco2_te_dry, d.ph2o, d.pco2_te_wet, d.fco2_te, d.fco2, "
			+ "q.intake_temp_1_used, q.intake_temp_2_used, q.intake_temp_3_used, "
			+ "q.salinity_1_used, q.salinity_2_used, q.salinity_3_used, "
			+ "q.eqt_1_used, q.eqt_2_used, q.eqt_3_used, "
			+ "q.eqp_1_used, q.eqp_2_used, q.eqp_3_used "
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
	
	private static final String SET_QC_RESULT_STATEMENT = "UPDATE qc SET qc_flag = ?, qc_message = ?,"
			+ "woce_flag = ?, woce_message = ? WHERE data_file_id = ? AND row = ?";
	
	private static final String GET_QC_MESSAGES_QUERY = "SELECT qc_message FROM qc WHERE data_file_id = ? AND row = ?";

	private static final String GET_QC_FLAG_QUERY = "SELECT qc_flag FROM qc WHERE data_file_id = ? AND row = ?";

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
	public static List<QCRecord> getQCRecords(DataSource dataSource, ColumnConfig columnConfig, long fileId, Instrument instrument) throws DatabaseException {
		
		Connection conn = null;
		PreparedStatement readStatement = null;
		ResultSet records = null;
		List<QCRecord> qcRecords = new ArrayList<QCRecord>();
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			readStatement = conn.prepareStatement(GET_QC_RECORDS_STATEMENT);
			readStatement.setLong(1,  fileId);
			
			records = readStatement.executeQuery();
			
			while (records.next()) {
				
				// Extract the row number and QC flags/comments
				int rowNumber = records.getInt(FIELD_ROW_NUMBER);
				Flag qcFlag = new Flag(records.getInt(FIELD_QC_FLAG));
				List<Message> qcComments = RebuildCode.getMessagesFromRebuildCodes(records.getString(FIELD_QC_COMMENT));
				Flag woceFlag = new Flag(records.getInt(FIELD_WOCE_FLAG));
				String woceComment = records.getString(FIELD_WOCE_COMMENT);

				// The remainder of the fields are data fields for the QC record
				List<String> recordData = new ArrayList<String>();
				recordData.add(null); // Field indices are 1-based
				for (int i = FIRST_DATA_FIELD; i <= FIRST_DATA_FIELD + columnConfig.getColumnCount() - 1; i++) {
					recordData.add(records.getString(i));
				}
				
				qcRecords.add(new QCRecord(fileId, instrument, columnConfig, rowNumber, recordData, qcFlag, qcComments, woceFlag, woceComment));
			}

			conn.commit();
			
		} catch (SQLException|DataRecordException|MessageException|InvalidFlagException e) {
			throw new DatabaseException("An error occurred while retrieving records for QC", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(readStatement);
			DatabaseUtils.closeConnection(conn);
		}
		
		return qcRecords;
	}
	
	public static void createQCRecord(Connection conn, long fileId, int row, Instrument instrument) throws DatabaseException {
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(ADD_QC_RECORD_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			stmt.setBoolean(3, instrument.hasIntakeTemp1());
			stmt.setBoolean(4, instrument.hasIntakeTemp2());
			stmt.setBoolean(5, instrument.hasIntakeTemp3());
			stmt.setBoolean(6, instrument.hasSalinity1());
			stmt.setBoolean(7, instrument.hasSalinity2());
			stmt.setBoolean(8, instrument.hasSalinity3());
			stmt.setBoolean(9, instrument.hasEqt1());
			stmt.setBoolean(10, instrument.hasEqt2());
			stmt.setBoolean(11, instrument.hasEqt3());
			stmt.setBoolean(12, instrument.hasEqp1());
			stmt.setBoolean(13, instrument.hasEqp2());
			stmt.setBoolean(14, instrument.hasEqp3());
			stmt.setInt(15, Flag.NOT_SET.getFlagValue());
			stmt.setString(16, "");
			stmt.setInt(17, Flag.NOT_SET.getFlagValue());
			stmt.setString(18, "");
			
			stmt.execute();

		} catch (SQLException e) {
			throw new DatabaseException("Error while creating QC record for file " + fileId + ", row " + row, e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
		
	}
	
	public static void setQC(Connection conn, long fileId, QCRecord record) throws DatabaseException, MessageException {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(SET_QC_RESULT_STATEMENT);
			
			stmt.setInt(1, record.getQCFlag().getFlagValue());
			stmt.setString(2, RebuildCode.getRebuildCodes(record.getMessages()));
			stmt.setInt(3, record.getWoceFlag().getFlagValue());
			stmt.setString(4, record.getWoceComment());
			stmt.setLong(5, fileId);
			stmt.setInt(6, record.getLineNumber());
			
			stmt.execute();
			
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while storing the QC result", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	public static List<Message> getQCMessages(Connection conn, long fileId, int row) throws MessageException, DatabaseException, RecordNotFoundException {
		
		PreparedStatement stmt = null;
		ResultSet record = null;
		List<Message> result = null;
		
		try {
			stmt = conn.prepareStatement(GET_QC_MESSAGES_QUERY);
			
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			
			record = stmt.executeQuery();
			
			if (!record.next()) {
				throw new RecordNotFoundException("Could not find QC record for file " + fileId + ", row " + row);
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

	public static Flag getQCFlag(Connection conn, long fileId, int row) throws MessageException, DatabaseException, RecordNotFoundException {
		
		PreparedStatement stmt = null;
		ResultSet record = null;
		Flag result = null;
		
		try {
			stmt = conn.prepareStatement(GET_QC_FLAG_QUERY);
			
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			
			record = stmt.executeQuery();
			
			if (!record.next()) {
				throw new RecordNotFoundException("Could not find QC record for file " + fileId + ", row " + row);
			} else {
				result = new Flag(record.getInt(1));
			}
			
			return result;
		} catch (SQLException|InvalidFlagException e) {
			throw new DatabaseException("An error occurred while retrieving QC messages", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
		}
	}
}
