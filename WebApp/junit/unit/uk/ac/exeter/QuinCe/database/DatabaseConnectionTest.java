package unit.uk.ac.exeter.QuinCe.database;

import java.io.FileInputStream;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Test;

public class DatabaseConnectionTest extends BaseDbTestCase {

	@Override
	protected IDataSet getDataSet() throws Exception {
		return new FlatXmlDataSetBuilder().build(new FileInputStream("empty_dataset.xml"));
	}
	
	@Test
	public void testConnection() throws Exception {
		IDatabaseConnection connection = getConnection();
		assertNotNull(connection);
	}
}
