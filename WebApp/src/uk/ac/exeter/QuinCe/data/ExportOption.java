package uk.ac.exeter.QuinCe.data;

import java.util.List;

import uk.ac.exeter.QuinCe.database.files.FileDataInterrogator;

public class ExportOption {

	private int index;
	
	private String name;
	
	private String separator;
	
	private List<String> columns;

	private int co2Type;
	
	public ExportOption(int index, String name, String separator, List<String> columns, int co2Type) throws ExportException {
		this.index = index;
		this.name = name;
		this.separator = separator;
		this.columns = columns;
		this.co2Type = co2Type;
		
		String invalidColumn = FileDataInterrogator.validateColumnNames(columns);
		
		if (null != invalidColumn) {
			throw new ExportException(name, "Invalid column name '" + invalidColumn + "'");
		}
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getName() {
		return name;
	}
	
	public List<String> getColumns() {
		return columns;
	}
	
	public String getSeparator() {
		return separator;
	}
	
	public int getCo2Type() {
		return co2Type;
	}
}
