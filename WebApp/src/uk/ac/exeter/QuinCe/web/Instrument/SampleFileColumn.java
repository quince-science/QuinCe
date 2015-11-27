package uk.ac.exeter.QuinCe.web.Instrument;

/**
 * Simple class to represent a column in the sample data file.
 * Used by the PrimeFaces DataTable when building the table to
 * display sample file contents.
 * 
 * Note that the column name is a dummy value - it's
 * never seen by the user as it's overwritten in the view.
 * 
 * @author Steve Jones
 *
 */
public class SampleFileColumn {
	
	/**
	 * The column index
	 */
	int colIndex;
	
	/**
	 * Builds a column object for the specified column index
	 * @param colIndex The column index
	 */
	public SampleFileColumn(int colIndex) {
		this.colIndex = colIndex;
	}
	
	/**
	 * Returns the column name, which is col&lt;index&gt;
	 * @return The column name
	 */
	public String getHeader() {
		return "col" + colIndex;
	}
	
	/**
	 * Returns the column index
	 * @return The column index
	 */
	public Integer getColumnIndex() {
		return colIndex;
	}

}
