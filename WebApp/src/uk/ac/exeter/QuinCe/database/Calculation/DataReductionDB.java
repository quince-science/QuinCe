package uk.ac.exeter.QuinCe.database.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class DataReductionDB {

	private static final String CLEAR_DATA_REDUCTION_STATEMENT = "DELETE FROM data_reduction WHERE data_file_id = ?";
	
	private static final String STORE_ROW_STATEMENT = "INSERT INTO data_reduction ("
			+ "data_file_id, row, co2_type, mean_intake_temp, mean_salinity, mean_eqt, mean_eqp, "
			+ "true_moisture, dried_co2, calibrated_co2) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public static void clearDataReductionData(DataSource dataSource, long fileId) throws DatabaseException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = dataSource.getConnection();
			
			stmt = conn.prepareStatement(CLEAR_DATA_REDUCTION_STATEMENT);
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
	
	public static void storeRow(Connection conn, long fileId, int row, int co2Type, double meanIntakeTemp,
			double meanSalinity, double meanEqt, double meanEqp, double trueMoisture, double driedCo2,
			double calibratedCo2) throws DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkPositive(row, "row");
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(STORE_ROW_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			stmt.setInt(3, co2Type);
			stmt.setDouble(4, meanIntakeTemp);
			stmt.setDouble(5, meanSalinity);
			stmt.setDouble(6, meanEqt);
			stmt.setDouble(7, meanEqp);
			stmt.setDouble(8, trueMoisture);
			stmt.setDouble(9, driedCo2);
			stmt.setDouble(10, calibratedCo2);
			
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while storing the row", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
		
	}
}
