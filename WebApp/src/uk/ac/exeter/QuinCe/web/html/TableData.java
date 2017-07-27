package uk.ac.exeter.QuinCe.web.html;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Class to build the information required to construct a table
 * on a web page. The table is built as a String, and there is
 * a corresponding Javascript function to parse the string
 * and draw the table in HTML
 * 
 * The table must be constructed in order, by calling
 * setHeaders (which sets the number of columns) followed by
 * repeated setColumn calls.
 * 
 * The table does not support colspan or rowspan functionality.
 * 
 *
 * 
 * @author Steve Jones
 *
 */
public class TableData {

	//TODO Replace this with JSON sent to the front end, rendered with Javascript
	
	/**
	 * The String representation of the table
	 */
	private List<TableCell> tableData = new ArrayList<TableCell>();
	
	/**
	 * Indicates whether or not the headers have been set for the table
	 */
	private boolean headersSet = false;
	
	/**
	 * Stores the number of columns in the table
	 */
	private int columnCount = 0;

	/**
	 * The CSS class for the table
	 */
	private String tableClass = null;
	
	/**
	 * The CSS class for the table header
	 */
	private String headerClass = null;
	
	
	public TableData(String tableClass, String headerClass) {
		this.tableClass = tableClass;
		this.headerClass = headerClass;
	}
	
	public TableData() {
	}
	
	/**
	 * Set the table headers. Also implicitly defines the number
	 * of columns in the table
	 * @param headers The headers
	 * @throws TableException If the headers have already been set.
	 */
	public void setHeaders(String... headers) throws TableException {
		if (headersSet) {
			throw new TableException("Headers have already been set");
		}

		for (String header : headers) {
			tableData.add(new TableCell(header));
		}

		columnCount = headers.length;
		headersSet = true;
	}

	/**
	 * Add a column to the table with a CSS class
	 * @param text The column text
	 * @param className The CSS class name
	 */
	public void addColumn(String text, String className) {
		tableData.add(new TableCell(text, className));
	}
	
	/**
	 * Add a column to the table with no CSS class
	 * @param text The column text
	 */
	public void addColumn(String text) {
		tableData.add(new TableCell(text));
	}
	
	/**
	 * Add an empty column to the table
	 */
	public void addEmptyColumn() {
		tableData.add(TableCell.emptyTableCell());
	}
	
	/**
	 * Add a date to the table with a CSS class
	 * @param date The date
	 * @param className The CSS class name
	 */
	public void addColumn(Calendar date, String className) {
		if (null == date) {
			addEmptyColumn();
		} else {
			tableData.add(new TableCell(DateTimeUtils.formatDate(date), className));
		}
	}
	
	/**
	 * Add a date to the table with no CSS class
	 * @param date The date
	 */
	public void addColumn(Calendar date) {
		if (null == date) {
			addEmptyColumn();
		} else {
			tableData.add(new TableCell(DateTimeUtils.formatDate(date)));
		}
	}

	public String toHtml() {
		StringBuffer html = new StringBuffer();
		
		html.append("<table");
		if (null != tableClass) {
			html.append(" class=\"");
			html.append(tableClass);
			html.append('"');
		}
		html.append('>');
		
		int currentColumn = 0;
		
		html.append("<tr>");
		
		// The first n columns are headers
		while (currentColumn < columnCount) {
			html.append("<th");
			if (null != headerClass) {
				html.append(" class=\"");
				html.append(headerClass);
				html.append('"');
			}
			html.append('>');
			
			html.append(tableData.get(currentColumn).content);
			html.append("</th>");
			currentColumn++;
		}
		
		html.append("<tr>");
		
		for (; currentColumn < tableData.size(); currentColumn++) {
			if (currentColumn % columnCount == 0) {
				html.append("<tr>");
			}
				
			TableCell cell = tableData.get(currentColumn);
			html.append("<td");
			if (null != cell.className) {
				html.append(" class=\"");
				html.append(cell.className);
				html.append('"');
			}
			html.append('>');
			html.append(cell.content);
			html.append("</td>");
			
			if (currentColumn % columnCount == (columnCount - 1)) {
				html.append("</tr>");
			}
		}
		
		html.append("</table>");
		
		return html.toString();
	}
}

class TableCell {
	
	protected String content;
	
	protected String className;
	
	protected TableCell(String content, String className) {
		this.content = content;
		this.className = className;
	}
	
	protected TableCell(String content) {
		this.content = content;
		this.className = null;
	}
	
	protected static TableCell emptyTableCell() {
		return new TableCell("");
	}
}