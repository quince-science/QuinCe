package uk.ac.exeter.QuinCe.database.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;

public class DataReductionDB {

	private static final String CLEAR_DATA_REDUCTION_STATEMENT = "DELETE FROM data_reduction WHERE data_file_id = ?";
	
	public static void clearRawData(DataSource dataSource, long fileId) throws DatabaseException {
		
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
	

}
