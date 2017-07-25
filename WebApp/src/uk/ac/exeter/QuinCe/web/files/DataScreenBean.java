package uk.ac.exeter.QuinCe.web.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.Files.CommentSet;
import uk.ac.exeter.QuinCe.data.Files.CommentSetEntry;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.FileDataInterrogator;
import uk.ac.exeter.QuinCe.data.Files.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunType;
import uk.ac.exeter.QuinCe.data.QC.QCDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.FileJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Managed bean to handle data for the main QC screen
 * 
 * @author Steve Jones
 *
 */
public class DataScreenBean extends BaseManagedBean {

	/**
	 * The navigation result returned after the bean is initialised
	 * @see #start()
	 */
	public static final String PAGE_START = "data_screen";
	
	/**
	 * Navigation result returned after the user leaves the QC pages
	 * @see #end()
	 */
	public static final String PAGE_END = "file_list";
	
	/**
	 * Indicator for HTML controls for the plot configuration popup
	 * @see #makeCheckbox(String, String, String, String)
	 * @see #makePlotCheckbox(String, String, String)
	 */
	private static final String POPUP_PLOT = "plot";
	
	/**
	 * Indicator for HTML controls for the map configuration popup
	 * @see #makeCheckbox(String, String, String, String)
	 * @see #makeMapCheckbox(String, String, String)
	 */
	private static final String POPUP_MAP = "map";
	
	/**
	 * The database ID of the data file being QCed
	 */
	private long fileId;
	
	/**
	 * The details of the file being edited
	 */
	private FileInfo fileDetails = null;
	
	/**
	 * The data columns being used in the left plot.
	 * 
	 * <p>
	 *   The columns are stored as a semi-colon separated list of internal column names. The first
	 *   column is the X axis, and all subsequent columns will be displayed on the Y axis.
	 *   The Javascript on the user interface will be responsible for ensuring that
	 *   the columns are set appropriately; the bean will not perform any checks.
	 * </p>
	 * 
	 * <p>
	 *   The row number, QC and WOCE flag, are automatically added to this list, but
	 *   are hidden when the plot is rendered.
	 *   The row is used to cross-reference with the data table, and the flags
	 *   are used to highlight values on the plot.
	 * </p>
	 * 
	 * @see FileDataInterrogator
	 * @see #getPlotData(List) 
	 */
	private String leftPlotColumns = null;
	
	/**
	 * The data for the left plot.
	 * 
	 * <p>
	 *   The data is stored as a JSON string, which will be parsed by the Javascript on
	 *   the user interface.
	 * </p>
	 * @see #generateLeftPlotData()
	 * @see #getPlotData(List)
	 */
	private String leftPlotData = null;
	
	/**
	 * The human-readable names of the columns shown in the left plot.
	 * @see #leftPlotColumns
	 */
	private String leftPlotNames = null;

	/**
	 * The data column being used in the left map.

	 * @see FileDataInterrogator
	 * @see #getMapData(String, List) 
	 */
	private String leftMapColumn = null;
	
	/**
	 * The bounds of the left map display.
	 * This is a list of [minx, miny, maxx, maxy]
	 */
	private List<Double> leftMapBounds = null;
	
	/**
	 * The scale limits for the left map
	 */
	private List<Double> leftMapScaleLimits = null;
	
	/**
	 * Indicates whether or not the scale for the left map should be updated
	 */
	private boolean leftMapUpdateScale = false;
	
	/**
	 * The data for the left map.
	 * 
	 * <p>
	 *   The data is stored as a JSON string, which will be parsed by the Javascript on
	 *   the user interface.
	 * </p>
	 * @see #generateLeftMapData()
	 * @see #getMapData(String, List)
	 */
	private String leftMapData = null;
	
	/**
	 * The human-readable names of the column shown in the left map.
	 * @see #leftMapColumn
	 */
	private String leftMapName = null;

	/**
	 * The data columns being used in the right plot.
	 * 
	 * <p>
	 *   The columns are stored as a semi-colon separated list of internal column names. The first
	 *   column is the X axis, and all subsequent columns will be displayed on the Y axis.
	 *   The Javascript on the user interface will be responsible for ensuring that
	 *   the columns are set appropriately; the bean will not perform any checks.
	 * </p>
	 * 
	 * <p>
	 *   The row number, QC and WOCE flag, are automatically added to this list, but
	 *   are hidden when the plot is rendered.
	 *   The row is used to cross-reference with the data table, and the flags
	 *   are used to highlight values on the plot.
	 * </p>
	 * 
	 * @see FileDataInterrogator
	 * @see #getPlotData(List) 
	 */
	private String rightPlotColumns = null;
	
	/**
	 * The data for the right plot.
	 *
	 * <p>
	 *   The data is stored as a JSON string, which will be parsed by the Javascript on
	 *   the user interface.
	 * </p>
	 * @see #generateRightPlotData()
	 * @see #getPlotData(List)
	 */
	private String rightPlotData = null;
	
	/**
	 * The names of the columns shown in the right plot.
	 * @see #rightPlotColumns
	 */
	private String rightPlotNames = null;

	/**
	 * The data column being used in the right map.

	 * @see FileDataInterrogator
	 * @see #getMapData(String, List)
	 */
	private String rightMapColumn = null;
	
	/**
	 * The bounds of the right map display.
	 * This is a list of [minx, miny, maxx, maxy]
	 */
	private List<Double> rightMapBounds = null;
	
	/**
	 * The scale limits for the right map
	 */
	private List<Double> rightMapScaleLimits = null;
	
	/**
	 * Indicates whether or not the scale for the right map should be updated
	 */
	private boolean rightMapUpdateScale = false;
	
	/**
	 * The data for the right map.
	 * 
	 * <p>
	 *   The data is stored as a JSON string, which will be parsed by the Javascript on
	 *   the user interface.
	 * </p>
	 * @see #generateRightMapData()
	 * @see #getMapData(String, List)
	 */
	private String rightMapData = null;
	
	/**
	 * The human-readable names of the column shown in the right map.
	 * @see #rightMapColumn
	 */
	private String rightMapName = null;

	/**
	 * The type of CO<sub>2</sub> measurements being viewed.
	 * Can only be one of {@link RunType#RUN_TYPE_WATER} or {@link RunType#RUN_TYPE_ATMOSPHERIC}.
	 * The behaviour of the application if this is set to any other value is undefined.
	 */
	private int co2Type = RunType.RUN_TYPE_WATER;
	
	/**
	 * Indicates that records with the specified flags will also be included in the plot/map.
	 * 
	 * <p>
	 * 	By default, only records with the flags:
	 * </p>
	 * <ul>
	 *   <li>{@link Flag#VALUE_GOOD}</li>
	 *   <li>{@link Flag#VALUE_ASSUMED_GOOD}</li>
	 *   <li>{@link Flag#VALUE_QUESTIONABLE}</li>
	 *   <li>{@link Flag#VALUE_NEEDED}</li>
	 * </ul>
	 * 
	 * <p>
	 *   are displayed in plots and maps. Records with
	 *   other flags can be included if they are added
	 *   to this field.
	 * </p>
	 */
	private List<String> optionalFlags = null;
	
	/**
	 * The current table mode, which indicates which columns are to be displayed
	 */
	private String tableMode = "basic";
	
	/**
	 * The data for the current view of the data table.
	 * 
	 * <p>
	 *   The table data is loaded on demand as the user scrolls around, to
	 *   eliminate the delay and memory requirements of loading a complete
	 *   voayge's data in one go. When the user scrolls to a particular row,
	 *   the data for that row and a set of rows before and after it is loaded.
	 * </p>
	 * 
	 * @see #generateTableData()
	 * @see <a href="https://datatables.net/examples/data_sources/server_side.html">DataTables Server-Side Processing</a>
	 */
	private String tableJsonData = null;
	
	/**
	 * A Javascript array string containing the list of all row numbers in the current data file that can be
	 * selected in the data table. Selectable rows can have their WOCE flag set by the user.
	 * Unselectable rows are typically rows that have their QC flag set to FATAL, which means they
	 * cannot be processed at all.
	 * 
	 * <p>
	 * The rows are loaded during {@link #start} via {@link #loadSelectableRowNumbers}.
	 * </p>
	 */
	private String selectableRowNumbers = null;

 	/**
 	 * An internal value for the DataTables library,
 	 * used when drawing retrieving table data from the server
	 * @see <a href="https://datatables.net/examples/data_sources/server_side.html">DataTables Server-Side Processing</a>
 	 */
	private int tableDataDraw;		

	/**
	 * The first row of the table data view to be loaded for DataTables
	 * @see <a href="https://datatables.net/examples/data_sources/server_side.html">DataTables Server-Side Processing</a>
	 */
	private int tableDataStart;		

	/**
	 * The number of rows to be loaded for DataTables
	 * @see <a href="https://datatables.net/examples/data_sources/server_side.html">DataTables Server-Side Processing</a>
	 */
	private int tableDataLength;		

	/**
	 * The total number of records in the data set. A negative value indicates that the record count is not known and
	 *   needs to be retrieved from the server.
	 * 
	 * <p>
	 *   Note that this is the number
	 *   of atmospheric or ocean records, depending on what is being displayed.
	 * </p>
	 */
	private int recordCount = -1;	

	/**
	 * The row numbers that have been selected by the user. Stored as a comma-separated list.
	 */
	private String selectedRows = null;
	
	/**
	 * The WOCE comment entered by the user. This will be applied to the selected records
	 * when {@link #applyWoceFlag()} is called.
	 */
	private String woceComment = null;
	
	/**
	 * The WOCE flag selected by the user. This will be applied to the selected records
	 * when {@link #applyWoceFlag()} is called.
	 */
	private int woceFlag = Flag.VALUE_NEEDED;
	
	/**
	 * The instrument to which the data file belongs
	 */
	private Instrument instrument;

	/**
	 * Indicates whether or not any WOCE flags have been changed.
	 * If they have, the data file will be resubmitted for data reduction
	 * when the user leaves the data screen.
	 */
	private boolean dirty = false;
	
	/**
	 * The set of comments for the WOCE dialog. Stored as a Javascript array of entries, with each entry containing
	 * Comment, Count and Flag value
	 */
	private String woceCommentList = null;
	
	/**
	 * The worst flag set on the selected rows
	 */
	private Flag worstSelectedFlag = Flag.GOOD;

	/**
	 * The bounds of the data. This is a list of values, of the form:
	 * <ul>
	 *   <li>Upper latitude</li>
	 *   <li>Right-most longitude</li>
	 *   <li>Lower latitude</li>
	 *   <li>Left-most longitude</li>
	 *   <li>Centre longitude</li>
	 *   <li>Centre latitude</li>
	 * </ul>
	 * 
	 * The values will take into account data that crosses the 180° line.
	 * These values are calculated by {@link FileDataInterrogator#getGeographicalBounds}
	 */
	private List<Double> dataBounds = null;
	
	/**
	 * Required basic constructor. This does nothing: all the actual construction
	 * is done in {@link #start()}.
	 */
	public DataScreenBean() {
		// Do nothing
	}

	/**
	 * Initialises the bean with the details of the selected data file.
	 * Any data from previous data files is removed first.
	 * @return The navigation to the QC screen
	 * @throws Exception If any errors occur
	 * @see #PAGE_START
	 */
	public String start() throws Exception {
		clearData();
		loadFileDetails();
		loadSelectableRowNumbers();
		
		// Temporarily always show Bad flags
		List<String> badFlags = new ArrayList<String>(1);
		badFlags.add("4");
		setOptionalFlags(badFlags);
		
		return PAGE_START;
	}
	
	/**
	 * Finishes the user's session on the QC page. If anything has been changed,
	 * the file is resubmitted for data reduction.
	 * @return The navigation to the file list
	 * @throws Exception If any errors occur during processing
	 * @see #end()
	 */
	public String end() throws Exception {
		
		if (dirty) {
			Map<String, String> parameters = new HashMap<String, String>(1);
			parameters.put(FileJob.FILE_ID_KEY, String.valueOf(fileId));
			
			DataSource dataSource = ServletUtils.getDBDataSource();
			Connection conn = dataSource.getConnection();
			
			JobManager.addJob(conn, getUser(), FileInfo.getJobClass(FileInfo.JOB_CODE_REDUCTION), parameters);
			DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_REDUCTION);
		}
		
		clearData();
		return PAGE_END;
	}
	
	/**
	 * Clears all data regarding the current data file from the bean
	 */
	private void clearData() {
		fileDetails = null;
		leftPlotColumns = null;
		leftPlotData = null;
		rightPlotColumns = null;
		rightPlotData = null;
		optionalFlags = null;
		tableJsonData = null;
		recordCount = -1;
		dirty = false;
	}
	
	/**
	 * Returns the database ID of the current data file
	 * @return The database ID of the data file
	 */
	public long getFileId() {
		return fileId;
	}
	
	/**
	 * Set the database ID of the current data file
	 * @param fileId The database ID of the data file
	 */
	public void setFileId(long fileId) {
		this.fileId = fileId;
	}
	
	/**
	 * Get the file details of the current data file
	 * @return The file details
	 */
	public FileInfo getFileDetails() {
		return fileDetails;
	}
	
	/**
	 * Get the columns to be displayed in the left plot.
	 * @return The columns for the left plot
	 * @see #leftPlotColumns
	 */
	public String getLeftPlotColumns() {
		return leftPlotColumns;
	}
	
	/**
	 * Set the columns to be displayed in the left plot.
	 * @param leftPlotColumns The columns for the left plot
	 * @see #leftPlotColumns
	 */
	public void setLeftPlotColumns(String leftPlotColumns) {
		this.leftPlotColumns = leftPlotColumns;
	}
	
	/**
	 * Get the data for the left plot.
	 * @return The data for the left plot
	 * @see #leftPlotData
	 */
	public String getLeftPlotData() {
		return leftPlotData;
	}
	
	/**
	 * Set the data for the left plot.
	 * @param leftPlotData The data for the left plot
	 * @see #leftPlotData
	 */
	public void setLeftPlotData(String leftPlotData) {
		this.leftPlotData = leftPlotData;
	}
	
	/**
	 * Get the human-readable names of the columns being displayed
	 * in the left plot.
	 * @return The columns names
	 * @see #leftPlotNames
	 */
	public String getLeftPlotNames() {
		return leftPlotNames;
	}
	
	/**
	 * Set the human-readable names of the columns being displayed
	 * in the left plot.
	 * @param leftPlotNames The column names
	 * @see #leftPlotNames
	 */
	public void setLeftPlotNames(String leftPlotNames){
		this.leftPlotNames = leftPlotNames;
	}
	
	/**
	 * Get the column to be displayed in the right map
	 * @return The right map column
	 */
	public String getRightMapColumn() {
		return rightMapColumn;
	}
	
	/**
	 * Set the column to be displayed in the right map
	 * @param rightMapColumn The right map column
	 */
	public void setRightMapColumn(String rightMapColumn) {
		this.rightMapColumn = rightMapColumn;
	}
	
	/**
	 * Get the current bounds of the right map viewport
	 * @return The right map bounds
	 */
	public String getRightMapBounds() {
		return StringUtils.listToDelimited(rightMapBounds, ",");
	}
	
	/**
	 * Set the current bounds of the right map viewport.
	 * Converts a csv list to a Java list
	 * @param rightMapBounds The right map bounds
	 */
	public void setRightMapBounds(String rightMapBounds) {
		this.rightMapBounds = StringUtils.delimitedToDoubleList(rightMapBounds);
	}
	
	/**
	 * Get the scale limits for the right map
	 * @return The scale limits
	 */
	public String getRightMapScaleLimits() {
		return '[' + StringUtils.listToDelimited(rightMapScaleLimits, ",") + ']';
	}
	
	/**
	 * Set the scale limits for the right map
	 * @param rightMapScaleLimits The scale limits
	 */
	public void setRightMapScaleLimits(String rightMapScaleLimits) {
		this.rightMapScaleLimits = StringUtils.delimitedToDoubleList(rightMapScaleLimits);
	}
	
	/**
	 * Determine whether the right map's scale limits need to be updated
	 * @return {@code true} if the scale is to be updated; {@code false} otherwise.
	 */
	public boolean getRightMapUpdateScale() {
		return rightMapUpdateScale;
	}
	
	/**
	 * Set whether the right map's scale limits need to be updated
	 * @param rightMapUpdateScale {@code true} if the scale is to be updated; {@code false} otherwise.
	 */
	public void setRightMapUpdateScale(boolean rightMapUpdateScale) {
		this.rightMapUpdateScale = rightMapUpdateScale;
	}
	
	/**
	 * Get the data for the right map
	 * @return The map data
	 */
	public String getRightMapData() {
		return rightMapData;
	}
	
	/**
	 * Set the data for the right map.
	 * @param rightMapData The data for the right map
	 * @see #rightMapData
	 */
	public void setRightMapData(String rightMapData) {
		this.rightMapData = rightMapData;
	}
	
	/**
	 * Get the human-readable name of the column being displayed
	 * in the right map.
	 * @return The human-readable column name
	 * @see #rightMapName
	 */
	public String getRightMapName(){
		return rightMapName;
	}

	/**
	 * Set the human-readable name of the column being displayed
	 * in the right map.
	 * @param rightMapName The column name
	 * @see #rightMapName
	 */
	public void setRightMapName(String rightMapName){
		this.rightMapName = rightMapName;
	}

	/**
	 * Get the columns to be displayed in the right plot.
	 * @return The columns for the right plot
	 * @see #rightPlotColumns
	 */
	public String getRightPlotColumns() {
		return rightPlotColumns;
	}
	
	/**
	 * Set the columns to be displayed in the right plot.
	 * @param rightPlotColumns The columns for the right plot
	 * @see #rightPlotColumns
	 */
	public void setRightPlotColumns(String rightPlotColumns) {
		this.rightPlotColumns = rightPlotColumns;
	}
	
	/**
	 * Get the data for the right plot.
	 * @return The data for the right plot
	 * @see #rightPlotData
	 */
	public String getRightPlotData() {
		return rightPlotData;
	}
	
	/**
	 * Set the data for the right plot.
	 * @param rightPlotData The data for the right plot
	 * @see #rightPlotData
	 */
	public void setRightPlotData(String rightPlotData) {
		this.rightPlotData = rightPlotData;
	}
	
	/**
	 * Get the human-readable names of the columns being displayed
	 * in the right plot.
	 * @return The columns names
	 * @see #rightPlotNames
	 */
	public String getRightPlotNames() {
		return rightPlotNames;
	}
	
	/**
	 * Set the human-readable names of the columns being displayed
	 * in the right plot.
	 * @param rightPlotNames The column names
	 * @see #rightPlotNames
	 */
	public void setRightPlotNames(String rightPlotNames){
		this.rightPlotNames = rightPlotNames;
	}
	
	/**
	 * Get the column to be displayed in the left map
	 * @return The left map column
	 */
	public String getLeftMapColumn() {
		return leftMapColumn;
	}
	
	/**
	 * Set the column to be displayed in the left map
	 * @param leftMapColumn The left map column
	 */
	public void setLeftMapColumn(String leftMapColumn) {
		this.leftMapColumn = leftMapColumn;
	}
	
	/**
	 * Get the current bounds of the left map viewport
	 * @return The left map bounds
	 */
	public String getLeftMapBounds() {
		return StringUtils.listToDelimited(leftMapBounds, ",");
	}
	
	/**
	 * Set the current bounds of the left map viewport.
	 * Converts a CSV list to a proper Java list
	 * @param leftMapBounds The left map bounds
	 */
	public void setLeftMapBounds(String leftMapBounds) {
		this.leftMapBounds = StringUtils.delimitedToDoubleList(leftMapBounds);
	}
	
	
	/**
	 * Get the scale limits for the left map
	 * @return The scale limits
	 */
	public String getLeftMapScaleLimits() {
		return '[' + StringUtils.listToDelimited(leftMapScaleLimits, ",") + ']';
	}
	
	/**
	 * Set the scale limits for the left map
	 * @param leftMapScaleLimits The scale limits
	 */
	public void setLeftMapScaleLimits(String leftMapScaleLimits) {
		this.leftMapScaleLimits = StringUtils.delimitedToDoubleList(leftMapScaleLimits);
	}
	
	/**
	 * Determine whether the left map's scale limits need to be updated
	 * @return {@code true} if the scale is to be updated; {@code false} otherwise.
	 */
	public boolean getLeftMapUpdateScale() {
		return leftMapUpdateScale;
	}
	
	/**
	 * Set whether the left map's scale limits need to be updated
	 * @param leftMapUpdateScale {@code true} if the scale is to be updated; {@code false} otherwise.
	 */
	public void setLeftMapUpdateScale(boolean leftMapUpdateScale) {
		this.leftMapUpdateScale = leftMapUpdateScale;
	}

	/**
	 * Get the data for the left map
	 * @return The map data
	 */
	public String getLeftMapData() {
		return leftMapData;
	}
	
	/**
	 * Set the data for the left map.
	 * @param leftMapData The data for the left map
	 * @see #leftMapData
	 */
	public void setLeftMapData(String leftMapData) {
		this.leftMapData = leftMapData;
	}
	
	/**
	 * Get the human-readable name of the column being displayed
	 * in the left map.
	 * @return The human-readable column name
	 * @see #leftMapName
	 */
	public String getLeftMapName(){
		return leftMapName;
	}

	/**
	 * Set the human-readable name of the column being displayed
	 * in the left map.
	 * @param leftMapName The column name
	 * @see #leftMapName
	 */
	public void setLeftMapName(String leftMapName){
		this.leftMapName = leftMapName;
	}

	/**
	 * Return the type of CO<sub>2</sub> measurement being viewed
	 * @return The CO<sub>2</sub> measurement type
	 * @see #co2Type
	 */
	public int getCo2Type() {
		return co2Type;
	}
	
	/**
	 * Get the type of CO<sub>2</sub> measurement to be displayed.
	 * Must be one of {@link RunType#RUN_TYPE_WATER} or {@link RunType#RUN_TYPE_ATMOSPHERIC}.
	 * The behaviour of the user interface is undefined if this is set to anything else.
	 * @param co2Type The type of measurement
	 * @see #co2Type
	 */
	public void setCo2Type(int co2Type) {
		this.co2Type = co2Type;
	}
	
	/**
	 * Get the list of flags for display of records in addition to the default set.
	 * @return The list of additional flags
	 * @see #optionalFlags
	 */
	public List<String> getOptionalFlags() {
		return optionalFlags;
	}
	
	/**
	 * Set the list of flags for display of records in addition to the default set.
	 * @param optionalFlags The list of additional flags
	 * @see #optionalFlags
	 */
	public void setOptionalFlags(List<String> optionalFlags) {
		if (optionalFlags.contains(String.valueOf(Flag.VALUE_BAD)) && !optionalFlags.contains(String.valueOf(Flag.VALUE_FATAL))) {
			optionalFlags.add(String.valueOf(Flag.VALUE_FATAL));
		}
		
		this.optionalFlags = optionalFlags;
		
		// Reset the record count, so it is retrieved from the database again.		
		recordCount = -1;
	}
	
	/**
	 * Get the table display mode. This determines which columns are displayed in the table.
	 * @return The table display mode.
	 */
	public String getTableMode() {
		return tableMode;
	}
	
	/**
	 * Set the table display mode. This determines which columns are displayed in the table.
	 * @param tableMode The table display mode
	 */
	public void setTableMode(String tableMode) {
		this.tableMode = tableMode;
	}
	
	/**
	 * Get the data for the current view in the data table
	 * @return The table data
	 * @see #tableJsonData
	 */
	public String getTableJsonData() {
 		return tableJsonData;		
 	}
	
	/**
	 * Set the data for the current view in the data table
	 * @param tableJsonData The table data
	 * @see #tableJsonData
	 */
 	public void setTableJsonData(String tableJsonData) {
 		this.tableJsonData = tableJsonData;
 	}		
 
 	/**
 	 * Get the current value for the DataTables internal {@code draw} parameter
 	 * @return The DataTables {@code draw} parameter
 	 * @see #tableDataDraw
 	 */
 	public int getTableDataDraw() {		
		return tableDataDraw;		
	}		
			
 	/**
 	 * Set the value for the DataTables internal {@code draw} parameter
 	 * @param tableDataDraw The DataTables {@code draw} parameter
 	 * @see #tableDataDraw
 	 */
	public void setTableDataDraw(int tableDataDraw) {		
		this.tableDataDraw = tableDataDraw;		
	}		

	/**
	 * Get the first row of the current view in the data table
	 * @return The first row of the view
	 * @see #tableDataStart
	 */
	public int getTableDataStart() {		
		return tableDataStart;		
	}		

	/**
	 * Set the first row of the view in the data table
	 * @param tableDataStart The first row of the view
	 * @see #tableDataStart
	 */
	public void setTableDataStart(int tableDataStart) {		
		this.tableDataStart = tableDataStart;		
	}		
	
	/**
	 * Get the number of rows in the current view in the data table
	 * @return The number of rows in the view
	 * @see #tableDataLength
	 */
	public int getTableDataLength() {		
		return tableDataLength;		
	}		

	/**
	 * Set the number of rows in the current view in the data file
	 * @param tableDataLength The number of rows in the view
	 * @see #tableDataLength
	 */
	public void setTableDataLength(int tableDataLength) {		
		this.tableDataLength = tableDataLength;		
	}		

	/**
	 * Get the total number of rows in the data file. If the number of rows is not
	 * known, the result will be negative.
	 * 
	 * <p>
	 *   Note that this is the number
	 *   of atmospheric or ocean records, depending on what is being displayed.
	 * </p>
	 * 
	 * @return The number of records
	 */
	public int getRecordCount() {		
		return recordCount;		
	}
 
	/**
	 * Set the total number of records in the data file. If the number of records
	 * is not known, set a negative value.
	 * 
	 * <p>
	 *   Note that this is the number
	 *   of atmospheric or ocean records, depending on what is being displayed.
	 * </p>
	 * 
	 * @param recordCount The number of records
	 */
	public void setRecordCount(int recordCount) {
		this.recordCount = recordCount;
  	}
 
	/**
	 * Get the set of rows that have been selected by the user.
	 * The rows are returned as an unsorted comma-separated list.
	 * @return The selected rows
	 */
	public String getSelectedRows() {
		return selectedRows;
	}
	
	/**
	 * Set the selected table rows.
	 * @param selectedRows The selected rows
	 * @see #selectedRows
	 */
	public void setSelectedRows(String selectedRows) {
		this.selectedRows = selectedRows;
	}
	
	/**
	 * Get the WOCE comment entered by the user.
	 * @return The WOCE comment
	 */
	public String getWoceComment() {
		return woceComment;
	}
	
	/**
	 * Record the WOCE comment entered by the user
	 * @param woceComment The WOCE comment
	 */
	public void setWoceComment(String woceComment) {
		this.woceComment = woceComment;
	}
	
	/**
	 * Get the WOCE flag selected by the user
	 * @return The WOCE flag
	 */
	public int getWoceFlag() {
		return woceFlag;
	}
	
	/**
	 * Record the WOCE flag selected by the user
	 * @param woceFlag The WOCE flag
	 */
	public void setWoceFlag(int woceFlag) {
		this.woceFlag = woceFlag;
	}
	
	/**
	 * Load details of the selected data file into the bean.
	 * 
	 * This only loads details used for referencing the data file and its
	 * general details; the actual data for the plots and table will be loaded
	 * dynamically at a later stage.
	 * 
	 * @throws MissingParamException If any parameters to the underlying data retrieval calls are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws ResourceException If the application resources cannot be accessed
	 * @throws RecordNotFoundException If the selected data file (or any of its related records) cannot be found
	 */
	private void loadFileDetails() throws MissingParamException, DatabaseException, ResourceException, RecordNotFoundException {
		fileDetails = DataFileDB.getFileDetails(ServletUtils.getDBDataSource(), fileId);
		dataBounds = FileDataInterrogator.getGeographicalBounds(ServletUtils.getDBDataSource(), fileId);
		DataFileDB.touchFile(ServletUtils.getDBDataSource(), fileId);
		instrument = InstrumentDB.getInstrumentByFileId(ServletUtils.getDBDataSource(), fileId);
	}
	
	/**
	 * Generate the check boxes to select columns for the data plots.
	 * @return The HTML for the check boxes
	 * @throws MissingParamException If any parameters for underlying data retrieval calls are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If any required database records are missing
	 * @throws ResourceException If the application resources cannot be accessed
	 */
	public String getPlotPopupEntries() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		
		Instrument instrument = InstrumentDB.getInstrument(ServletUtils.getDBDataSource(), fileDetails.getInstrumentId());
		
		StringBuffer output = new StringBuffer();
		
		output.append("<table><tr>");
		
		// First column
		output.append("<td><table>");
		
		output.append(makePlotCheckbox("datetime", "dateTime", "Date/Time"));
		output.append(makePlotCheckbox("longitude", "longitude", "Longitude"));
		output.append(makePlotCheckbox("latitude", "latitude", "Latitude"));

		// Intake temperature
		if (instrument.getIntakeTempCount() == 1) {
			output.append(makePlotCheckbox("intakeTemp", "intakeTempMean", "Intake Temperature"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Intake Temperature:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("intakeTemp", "intakeTempMean", "Mean"));
			
			if (instrument.hasIntakeTemp1()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp1", instrument.getIntakeTempName1()));
			}
			
			if (instrument.hasIntakeTemp2()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp2", instrument.getIntakeTempName2()));
			}
			
			if (instrument.hasIntakeTemp3()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp3", instrument.getIntakeTempName3()));
			}
			
			output.append("</table></td></tr>");
		}

		// Salinity
		if (instrument.getSalinityCount() == 1) {
			output.append(makePlotCheckbox("salinity", "salinityMean", "Salinity"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Salinity:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("salinity", "salinityMean", "Mean"));
			
			if (instrument.hasSalinity1()) {
				output.append(makePlotCheckbox("salinity", "salinity1", instrument.getSalinityName1()));
			}
			
			if (instrument.hasSalinity2()) {
				output.append(makePlotCheckbox("salinity", "salinity2", instrument.getSalinityName2()));
			}
			
			if (instrument.hasSalinity3()) {
				output.append(makePlotCheckbox("salinity", "salinity3", instrument.getSalinityName3()));
			}

			output.append("</table></td></tr>");
		}
		
		// End of first column/start of second
		output.append("</table></td><td><table>");
		
		boolean flowSensor = false;
		
		if (instrument.getAirFlowCount() > 0) {
			flowSensor = true;
			
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Air Flow:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			if (instrument.hasAirFlow1()) {
				output.append(makePlotCheckbox("airFlow", "airFlow1", instrument.getAirFlowName1()));
			}
			
			if (instrument.hasAirFlow2()) {
				output.append(makePlotCheckbox("airFlow", "airFlow2", instrument.getAirFlowName2()));
			}
			
			if (instrument.hasAirFlow3()) {
				output.append(makePlotCheckbox("airFlow", "airFlow3", instrument.getAirFlowName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		if (instrument.getWaterFlowCount() > 0) {
			flowSensor = true;
			
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Water Flow:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			if (instrument.hasWaterFlow1()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow1", instrument.getWaterFlowName1()));
			}
			
			if (instrument.hasWaterFlow2()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow2", instrument.getWaterFlowName2()));
			}
			
			if (instrument.hasWaterFlow3()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow3", instrument.getWaterFlowName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		if (flowSensor) {
			// End of 2nd column/start of 3rd
			output.append("</table></td><td><table>");
		}

		// Equilibrator temperature
		if (instrument.getEqtCount() == 1) {
			output.append(makePlotCheckbox("eqt", "eqtMean", "Equilibrator Temperature"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Equilibrator Temperature:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			output.append(makePlotCheckbox("eqt", "eqtMean", "Mean"));
			
			if (instrument.hasEqt1()) {
				output.append(makePlotCheckbox("eqt", "eqt1", instrument.getEqtName1()));
			}
			
			if (instrument.hasEqt2()) {
				output.append(makePlotCheckbox("eqt", "eqt2", instrument.getEqtName2()));
			}
			
			if (instrument.hasEqt3()) {
				output.append(makePlotCheckbox("eqt", "eqt3", instrument.getEqtName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		// Delta T
		output.append(makePlotCheckbox("deltaT", "deltaT", "Δ Temperature"));

		// Equilibrator Pressure
		if (instrument.getEqpCount() == 1) {
			output.append(makePlotCheckbox("eqp", "eqpMean", "Equilibrator Pressure"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Equilibrator Pressure:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("eqp", "eqpMean", "Mean"));
			
			if (instrument.hasEqp1()) {
				output.append(makePlotCheckbox("eqp", "eqp1", instrument.getEqpName1()));
			}
			
			if (instrument.hasEqp2()) {
				output.append(makePlotCheckbox("eqp", "eqp2", instrument.getEqpName2()));
			}
			
			if (instrument.hasEqp3()) {
				output.append(makePlotCheckbox("eqp", "eqp3", instrument.getEqpName3()));
			}

			output.append("</table></td></tr>");
		}
		
		// Atmospheric Pressure
		/*
		 * We'll put this in when we get to doing atmospheric stuff.
		 * It needs to specify whether it's measured or from external data
		 * 
		output.append(makePlotCheckbox("atmosPressure", "atmospressure", "Atmospheric Pressure"));
		output.append("</td><td>Atmospheric Pressure</td></tr>");
		*/
		
		// xH2O
		output.append("<tr><td colspan=\"2\" class=\"minorHeading\">xH<sub>2</sub>O:</td></tr>");
		output.append("<tr><td></td><td><table>");

		output.append(makePlotCheckbox("xh2o", "xh2oMeasured", "Measured"));
		output.append(makePlotCheckbox("xh2o", "xh2oTrue", "True"));
		
		output.append("</table></td></tr>");

		// pH2O
		output.append(makePlotCheckbox("pH2O", "pH2O", "pH<sub>2</sub>O"));

		// End of 3rd column/Start of 4th column
		output.append("</table></td><td><table>");

		// CO2
		output.append("<tr><td colspan=\"2\" class=\"minorHeading\">CO<sub>2</sub>:</td></tr>");
		output.append("<tr><td></td><td><table>");

		output.append(makePlotCheckbox("co2", "co2Measured", "Measured"));

		if (!instrument.getSamplesDried()) {
			output.append(makePlotCheckbox("co2", "co2Dried", "Dried"));
		}

		output.append(makePlotCheckbox("co2", "co2Calibrated", "Calibrated"));
		output.append(makePlotCheckbox("co2", "pCO2TEDry", "pCO<sub>2</sub> TE Dry"));
		output.append(makePlotCheckbox("co2", "pCO2TEWet", "pCO<sub>2</sub> TE Wet"));
		output.append(makePlotCheckbox("co2", "fCO2TE", "fCO<sub>2</sub> TE"));
		output.append(makePlotCheckbox("co2", "fCO2Final", "fCO<sub>2</sub> Final"));

		output.append("</table></td></tr>");

		// End of column 4
		output.append("</td></table>");
		
		// End of outer table
		output.append("</tr></table>");
		
		return output.toString();
	}
	
	/**
	 * Generate the check boxes to select columns for the data maps.
	 * @return The HTML for the check boxes
	 * @throws MissingParamException If any parameters for underlying data retrieval calls are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If any required database records are missing
	 * @throws ResourceException If the application resources cannot be accessed
	 */
	public String getMapPopupEntries() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		Instrument instrument = InstrumentDB.getInstrument(ServletUtils.getDBDataSource(), fileDetails.getInstrumentId());
		
		StringBuffer output = new StringBuffer();
		
		output.append("<table><tr>");
		
		// First column
		output.append("<td><table>");
		
		output.append(makeMapCheckbox("datetime", "dateTime", "Date/Time"));

		// Intake temperature
		if (instrument.getIntakeTempCount() == 1) {
			output.append(makeMapCheckbox("intakeTemp", "intakeTempMean", "Intake Temperature"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Intake Temperature:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makeMapCheckbox("intakeTemp", "intakeTempMean", "Mean"));
			
			if (instrument.hasIntakeTemp1()) {
				output.append(makeMapCheckbox("intakeTemp", "intakeTemp1", instrument.getIntakeTempName1()));
			}
			
			if (instrument.hasIntakeTemp2()) {
				output.append(makeMapCheckbox("intakeTemp", "intakeTemp2", instrument.getIntakeTempName2()));
			}
			
			if (instrument.hasIntakeTemp3()) {
				output.append(makeMapCheckbox("intakeTemp", "intakeTemp3", instrument.getIntakeTempName3()));
			}
			
			output.append("</table></td></tr>");
		}

		// Salinity
		if (instrument.getSalinityCount() == 1) {
			output.append(makeMapCheckbox("salinity", "salinityMean", "Salinity"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Salinity:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makeMapCheckbox("salinity", "salinityMean", "Mean"));
			
			if (instrument.hasSalinity1()) {
				output.append(makeMapCheckbox("salinity", "salinity1", instrument.getSalinityName1()));
			}
			
			if (instrument.hasSalinity2()) {
				output.append(makeMapCheckbox("salinity", "salinity2", instrument.getSalinityName2()));
			}
			
			if (instrument.hasSalinity3()) {
				output.append(makeMapCheckbox("salinity", "salinity3", instrument.getSalinityName3()));
			}

			output.append("</table></td></tr>");
		}
		
		// End of first column/start of second
		output.append("</table></td><td><table>");
		
		boolean flowSensor = false;
		
		if (instrument.getAirFlowCount() > 0) {
			flowSensor = true;
			
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Air Flow:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			if (instrument.hasAirFlow1()) {
				output.append(makeMapCheckbox("airFlow", "airFlow1", instrument.getAirFlowName1()));
			}
			
			if (instrument.hasAirFlow2()) {
				output.append(makeMapCheckbox("airFlow", "airFlow2", instrument.getAirFlowName2()));
			}
			
			if (instrument.hasAirFlow3()) {
				output.append(makeMapCheckbox("airFlow", "airFlow3", instrument.getAirFlowName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		if (instrument.getWaterFlowCount() > 0) {
			flowSensor = true;
			
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Water Flow:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			if (instrument.hasWaterFlow1()) {
				output.append(makeMapCheckbox("waterFlow", "waterFlow1", instrument.getWaterFlowName1()));
			}
			
			if (instrument.hasWaterFlow2()) {
				output.append(makeMapCheckbox("waterFlow", "waterFlow2", instrument.getWaterFlowName2()));
			}
			
			if (instrument.hasWaterFlow3()) {
				output.append(makeMapCheckbox("waterFlow", "waterFlow3", instrument.getWaterFlowName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		if (flowSensor) {
			// End of 2nd column/start of 3rd
			output.append("</table></td><td><table>");
		}

		// Equilibrator temperature
		if (instrument.getEqtCount() == 1) {
			output.append(makeMapCheckbox("eqt", "eqtMean", "Equilibrator Temperature"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Equilibrator Temperature:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			output.append(makeMapCheckbox("eqt", "eqtMean", "Mean"));
			
			if (instrument.hasEqt1()) {
				output.append(makeMapCheckbox("eqt", "eqt1", instrument.getEqtName1()));
			}
			
			if (instrument.hasEqt2()) {
				output.append(makeMapCheckbox("eqt", "eqt2", instrument.getEqtName2()));
			}
			
			if (instrument.hasEqt3()) {
				output.append(makeMapCheckbox("eqt", "eqt3", instrument.getEqtName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		// Delta T
		output.append(makePlotCheckbox("deltaT", "deltaT", "Δ Temperature"));

		// Equilibrator Pressure
		if (instrument.getEqpCount() == 1) {
			output.append(makeMapCheckbox("eqp", "eqpMean", "Equilibrator Pressure"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Equilibrator Pressure:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makeMapCheckbox("eqp", "eqpMean", "Mean"));
			
			if (instrument.hasEqp1()) {
				output.append(makeMapCheckbox("eqp", "eqp1", instrument.getEqpName1()));
			}
			
			if (instrument.hasEqp2()) {
				output.append(makeMapCheckbox("eqp", "eqp2", instrument.getEqpName2()));
			}
			
			if (instrument.hasEqp3()) {
				output.append(makeMapCheckbox("eqp", "eqp3", instrument.getEqpName3()));
			}

			output.append("</table></td></tr>");
		}
		
		// Atmospheric Pressure
		/*
		 * We'll put this in when we get to doing atmospheric stuff.
		 * It needs to specify whether it's measured or from external data
		 * 
		output.append(makePlotCheckbox("atmosPressure", "atmospressure", "Atmospheric Pressure"));
		output.append("</td><td>Atmospheric Pressure</td></tr>");
		*/
		
		// xH2O
		output.append("<tr><td colspan=\"2\" class=\"minorHeading\">xH<sub>2</sub>O:</td></tr>");
		output.append("<tr><td></td><td><table>");

		output.append(makeMapCheckbox("xh2o", "xh2oMeasured", "Measured"));
		output.append(makeMapCheckbox("xh2o", "xh2oTrue", "True"));
		
		output.append("</table></td></tr>");

		// pH2O
		output.append(makeMapCheckbox("pH2O", "pH2O", "pH<sub>2</sub>O"));

		// End of 3rd column/Start of 4th column
		output.append("</table></td><td><table>");

		// CO2
		output.append("<tr><td colspan=\"2\" class=\"minorHeading\">CO<sub>2</sub>:</td></tr>");
		output.append("<tr><td></td><td><table>");

		output.append(makeMapCheckbox("co2", "co2Measured", "Measured"));

		if (!instrument.getSamplesDried()) {
			output.append(makeMapCheckbox("co2", "co2Dried", "Dried"));
		}

		output.append(makeMapCheckbox("co2", "co2Calibrated", "Calibrated"));
		output.append(makeMapCheckbox("co2", "pCO2TEDry", "pCO<sub>2</sub> TE Dry"));
		output.append(makeMapCheckbox("co2", "pCO2TEWet", "pCO<sub>2</sub> TE Wet"));
		output.append(makeMapCheckbox("co2", "fCO2TE", "fCO<sub>2</sub> TE"));
		output.append(makeMapCheckbox("co2", "fCO2Final", "fCO<sub>2</sub> Final"));

		output.append("</table></td></tr>");

		// End of column 4
		output.append("</td></table>");
		
		// End of outer table
		output.append("</tr></table>");
		
		return output.toString();
	}
	
	/**
	 * Generate the HTML for a checkbox in the plot column selection popup
	 * @param group The group that will contain the checkbox
	 * @param field The name of the column
	 * @param label The label for the checkbox
	 * @return The checkbox HTML
	 */
	private String makePlotCheckbox(String group, String field, String label) {
		return makeCheckbox(POPUP_PLOT, group, field, label);
	}
	
	/**
	 * Generate the HTML for a checkbox in the map column selection popup
	 * @param group The group that will contain the checkbox
	 * @param field The name of the column
	 * @param label The label for the checkbox
	 * @return The checkbox HTML
	 */
	private String makeMapCheckbox(String group, String field, String label) {
		return makeCheckbox(POPUP_MAP, group, field, label);
	}
	
	/**
	 * Generate the HTML for a column selection checkbox
	 * @param popupType Either {@link #POPUP_PLOT} or {@link #POPUP_MAP}
	 * @param group The group that will contain the checkbox
	 * @param field The name of the column
	 * @param label The label for the checkbox
	 * @return The checkbox HTML
	 */
	private String makeCheckbox(String popupType, String group, String field, String label) {

		String inputID = popupType + "_" + group + "_" + field;
		
		StringBuffer checkbox = new StringBuffer();
		checkbox.append("<tr><td><input type=\"checkbox\" id=\"");
		checkbox.append(inputID);
		checkbox.append("\" value=\"");
		checkbox.append(field);
		checkbox.append("\"/></td><td><label for=\"");
		checkbox.append(inputID);
		checkbox.append("\">");
		checkbox.append(label);
		checkbox.append("</label></td></tr>");
		
		return checkbox.toString();
	}
	
	/**
	 * Generate the data for the left plot.
	 * @see #getPlotData(List)
	 */
	public void generateLeftPlotData() {
		List<String> columns = StringUtils.delimitedToList(leftPlotColumns);
		setLeftPlotData(getPlotData(columns));
		setLeftPlotNames(makePlotNames(columns));
	}
	
	/**
	 * Generate the data for the left map.
	 * @see #getMapData(String, List)
	 */
	public void generateLeftMapData() {
		if (leftMapUpdateScale) {
			leftMapScaleLimits = getMapScaleLimits(leftMapColumn);
		}
		
		setLeftMapData(getMapData(leftMapColumn, leftMapBounds));
		setLeftMapName(getPlotSeriesName(leftMapColumn));
	}

	/**
	 * Generate the data for the right plot.
	 * @see #getPlotData(List)
	 */
	public void generateRightPlotData() {
		List<String> columns = StringUtils.delimitedToList(rightPlotColumns);
		setRightPlotData(getPlotData(columns)); 
		setRightPlotNames(makePlotNames(columns));
	}
	
	/**
	 * Generate the data for the right map.
	 * @see #getMapData(String, List)
	 */
	public void generateRightMapData() {
		if (rightMapUpdateScale) {
			rightMapScaleLimits = getMapScaleLimits(rightMapColumn);
		}
		
		setRightMapData(getMapData(rightMapColumn, rightMapBounds));
		setRightMapName(getPlotSeriesName(rightMapColumn));
	}
	
	/**
	 * Get the value range for a column being shown on a map
	 * @param column The column
	 * @return The value range
	 */
	public List<Double> getMapScaleLimits(String column) {
		List<Double> result = null;
		
		try {
			result = FileDataInterrogator.getValueRange(ServletUtils.getDBDataSource(), fileId, column, co2Type);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (null == result) {
			result = new ArrayList<Double>(2);
			result.add(0.0);
			result.add(0.0);
		}
		
		return result;
	}
	

	/**
	 * Retrieve the data for a plot from the database as a JSON string.
	 * @param columns The list of columns for the plot. The first column will be for the X axis, and the subsequent columns will be display on the Y axis.
	 * @return The plot data
	 */
	private String getPlotData(List<String> columns) {
		
		String output;
		
		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			
			// Add in the row number and flags as the first Y-axis columns. We need it for syncing the graphs and the table
			// The list returned from delimitedToList does not allow inserting, so we have to do it the hard way.
			List<String> submittedColumnList = new ArrayList<String>(columns.size() + 1);
			
			// Add the X axis
			submittedColumnList.add(columns.get(0));
			
			// Now the row number
			submittedColumnList.add("row");
			
			// Add QC and WOCE flags
			submittedColumnList.add("qcFlag");
			submittedColumnList.add("woceFlag");
			
			// And the Y axis columns
			submittedColumnList.addAll(columns.subList(1, columns.size()));
			
			output = FileDataInterrogator.getJsonDataArray(dataSource, fileId, co2Type, submittedColumnList, null, getIncludeFlags(), 1, 0, true, false, false);
		} catch (Exception e) {
			e.printStackTrace();
			output = "***ERROR: " + e.getMessage();
		}
		
		return output;
	}
	
	/**
	 * Retrieve the data for a map from the database as a JSON string.
	 * @param column The list column to be displayed on the map
	 * @return The map data
	 */
	private String getMapData(String column, List<Double> bounds) {
		String output;
		
		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			
			// Add in the row number and flags as the first Y-axis columns. We need it for syncing the graphs and the table
			// The list returned from delimitedToList does not allow inserting, so we have to do it the hard way.
			List<String> submittedColumnList = new ArrayList<String>(6);
			
			// Position
			submittedColumnList.add("longitude");
			submittedColumnList.add("latitude");
			
			// Now the row number
			submittedColumnList.add("row");
			
			// Add QC and WOCE flags
			submittedColumnList.add("qcFlag");
			submittedColumnList.add("woceFlag");
			
			// And the Y axis columns
			submittedColumnList.add(column);
						
			output = FileDataInterrogator.getJsonDataArray(dataSource, fileId, co2Type, submittedColumnList, bounds, getIncludeFlags(), 1, 0, true, false, true);
		} catch (Exception e) {
			e.printStackTrace();
			output = "***ERROR: " + e.getMessage();
		}
		
		return output;
	}

	/**
	 * Retrieve the data for the table from the database as a JSON string.
	 * The data is stored in {@link #tableJsonData}.
	 */
	public void generateTableData() {

		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			
			if (recordCount < 0) {		
				setRecordCount(FileDataInterrogator.getRecordCount(dataSource, fileId, co2Type, getIncludeFlags()));
			}
			
			List<String> columns = new ArrayList<String>();
			columns.add("dateTime");
			columns.add("row");
			columns.add("longitude");
			columns.add("latitude");
			
			if (instrument.getIntakeTempCount() == 1) {
				columns.add("intakeTempMean");
			} else {
				if (instrument.hasIntakeTemp1()) {
					columns.add("intakeTemp1");
				}
				if (instrument.hasIntakeTemp2()) {
					columns.add("intakeTemp2");
				}
				if (instrument.hasIntakeTemp3()) {
					columns.add("intakeTemp3");
				}
				
				columns.add("intakeTempMean");
			}
			
			if (instrument.getSalinityCount() == 1) {
				columns.add("salinityMean");
			} else {
				if (instrument.hasSalinity1()) {
					columns.add("salinity1");
				}
				if (instrument.hasSalinity2()) {
					columns.add("salinity2");
				}
				if (instrument.hasSalinity3()) {
					columns.add("salinity3");
				}
				
				columns.add("salinityMean");
			}
			
			if (instrument.hasAirFlow1()) {
				columns.add("air_flow_1");
			}
			if (instrument.hasAirFlow2()) {
				columns.add("air_flow_2");
			}
			if (instrument.hasAirFlow3()) {
				columns.add("air_flow_3");
			}

			if (instrument.hasWaterFlow1()) {
				columns.add("water_flow_1");
			}
			if (instrument.hasWaterFlow2()) {
				columns.add("water_flow_2");
			}
			if (instrument.hasWaterFlow3()) {
				columns.add("water_flow_3");
			}
			
			if (instrument.getEqtCount() == 1) {
				columns.add("eqtMean");
			} else {
				if (instrument.hasEqt1()) {
					columns.add("eqt1");
				}
				if (instrument.hasEqt2()) {
					columns.add("eqt2");
				}
				if (instrument.hasEqt3()) {
					columns.add("eqt3");
				}
				
				columns.add("eqtMean");
			}
			
			columns.add("deltaT");
			
			if (instrument.getEqpCount() == 1) {
				columns.add("eqpMean");
			} else {
				if (instrument.hasEqp1()) {
					columns.add("eqp1");
				}
				if (instrument.hasEqp2()) {
					columns.add("eqp2");
				}
				if (instrument.hasEqp3()) {
					columns.add("eqp3");
				}
				
				columns.add("eqtMean");
			}
			
			columns.add("atmosPressure");
			columns.add("xh2oMeasured");
			columns.add("xh2oTrue");
			columns.add("pH2O");
			columns.add("co2Measured");
			columns.add("co2Dried");
			columns.add("co2Calibrated");
			columns.add("pCO2TEDry");
			columns.add("pCO2TEWet");
			columns.add("fCO2TE");
			columns.add("fCO2Final");
			columns.add("qcFlag");
			columns.add("qcMessage");
			columns.add("woceFlag");
			columns.add("woceMessage");
			
			setTableJsonData(FileDataInterrogator.getJsonDataObjects(dataSource, fileId, co2Type, columns, getIncludeFlags(), tableDataStart, tableDataLength, true, true, true));
		} catch (Exception e) {
			e.printStackTrace();
			setTableJsonData("***ERROR: " + e.getMessage());
		}
	}
	
	/**
	 * Retrieve the list of column headings for the data table. The result is a JSON string representing a Javascript array.
	 * @return The list of column headings
	 */
	public String getTableHeadings() {

		StringBuffer output = new StringBuffer('[');
		
		output.append("['Date/Time', 'Row', 'Longitude', 'Latitude', ");
			
		if (instrument.getIntakeTempCount() == 1) {
			output.append("'Intake Temp', ");
		} else {
			if (instrument.hasIntakeTemp1()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName1());
				output.append("', ");
			}
			if (instrument.hasIntakeTemp2()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName2());
				output.append("', ");
			}
			if (instrument.hasIntakeTemp3()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName3());
				output.append("', ");
			}
			
			output.append("'Intake Temp:<br/>Mean', ");
		}
			
		if (instrument.getSalinityCount() == 1) {
			output.append("'Salinity', ");
		} else {
			if (instrument.hasSalinity1()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName1());
				output.append("', ");
			}
			if (instrument.hasSalinity2()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName2());
				output.append("', ");
			}
			if (instrument.hasSalinity3()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName3());
				output.append("', ");
			}
			
			output.append("'Salinity:<br/>Mean', ");
		}
		
		if (instrument.hasAirFlow1()) {
			output.append("'Air Flow:<br/>");
			output.append(instrument.getAirFlowName1());
			output.append("', ");
		}
		if (instrument.hasAirFlow2()) {
			output.append("'Air Flow:<br/>");
			output.append(instrument.getAirFlowName2());
			output.append("', ");
		}
		if (instrument.hasAirFlow3()) {
			output.append("'Air Flow:<br/>");
			output.append(instrument.getAirFlowName3());
			output.append("', ");
		}

		if (instrument.hasWaterFlow1()) {
			output.append("'Water Flow:<br/>");
			output.append(instrument.getWaterFlowName1());
			output.append("', ");
		}
		if (instrument.hasWaterFlow2()) {
			output.append("'Water Flow:<br/>");
			output.append(instrument.getWaterFlowName2());
			output.append("', ");
		}
		if (instrument.hasWaterFlow3()) {
			output.append("'Water Flow:<br/>");
			output.append(instrument.getWaterFlowName3());
			output.append("', ");
		}

		if (instrument.getEqtCount() == 1) {
			output.append("'Equil. Temp', ");
		} else {
			if (instrument.hasEqt1()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName1());
				output.append("', ");
			}
			if (instrument.hasEqt2()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName2());
				output.append("', ");
			}
			if (instrument.hasEqt3()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName3());
				output.append("', ");
			}
			
			output.append("'Equil. Temp:<br/>Mean', ");
		}
		
		output.append("'Δ Temperature', ");

		if (instrument.getEqpCount() == 1) {
			output.append("'Equil. Pressure', ");
		} else {
			if (instrument.hasEqp1()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName1());
				output.append("', ");
			}
			if (instrument.hasEqp2()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName2());
				output.append("', ");
			}
			if (instrument.hasEqp3()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName3());
				output.append("', ");
			}
			
			output.append("'Equil. Pressure:<br/>Mean', ");
		}

		output.append("'Atmos. Pressure', 'xH₂O (Measured)', 'xH₂O (True)', 'pH₂O', 'CO₂ Measured', 'CO₂ Dried', 'CO₂ Calibrated', 'pCO₂ TE Dry', "
				+ "'pCO₂ TE Wet', 'fCO₂ TE', 'fCO₂ Final', 'QC Flag', 'QC Message', 'WOCE Flag', 'WOCE Message']");
		
		return output.toString();
	}

	/**
	 * Generate the list of WOCE flags that will be used to select records to be displayed on the data screen.
	 * Includes the default set of flags plus any other set in {@link #optionalFlags}.
	 * @return The list of flags
	 * @see uk.ac.exeter.QCRoutines.messages.Flag
	 */
	private List<Integer> getIncludeFlags() {
		List<Integer> includeFlags = new ArrayList<Integer>();
		includeFlags.add(Flag.VALUE_GOOD);
		includeFlags.add(Flag.VALUE_ASSUMED_GOOD);
		includeFlags.add(Flag.VALUE_QUESTIONABLE);
		includeFlags.add(Flag.VALUE_NEEDED);
		
		if (null != optionalFlags) {
			for (String optionalFlag : optionalFlags) {
				includeFlags.add(Integer.parseInt(optionalFlag));
			}
		}
		
		return includeFlags;
	}
	
	/**
	 * Retrieve the details of the instrument for the current data file
	 * @return The instrument details
	 */
	public Instrument getInstrument() {
		return instrument;
	}
	
	/**
	 * Apply the automatically generated QC flags to the rows selected in the table
	 */
	public void acceptQCFlags() {
		try {
			QCDB.acceptQCFlags(ServletUtils.getDBDataSource(), fileId, getSelectedRows());
			dirty = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Apply the entered WOCE flag and comment to the rows selected in the table
	 */
	public void applyWoceFlag() {
		try {
			QCDB.setWoceFlags(ServletUtils.getDBDataSource(), fileId, getSelectedRows(), getWoceFlag(), getWoceComment());
			dirty = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String getFormName() {
		return "dataScreen";
	}
	
	/**
	 * Generate the list of human-readable column names for a given set of
	 * internal column names
	 * @param columns The internal column names
	 * @return The human-readable column names
	 * @see #getPlotData(List)
	 * @see FileDataInterrogator
	 */
	private String makePlotNames(List<String> columns) {

		List<String> output = new ArrayList<String>(columns.size());
		
		// The first column is the X axis
		output.add(getPlotSeriesName(columns.get(0)));
		
		// Next are the row, QC Flag and WOCE Flag. These are fixed internal series
		// That are never displayed.
		output.add("Row");
		output.add("QC Flag");
		output.add("WOCE Flag");
		
		// Now the rest of the columns
		for (int i = 1; i < columns.size(); i++) {
			output.add(getPlotSeriesName(columns.get(i)));
		}
		
		return StringUtils.listToDelimited(output);
	}

	/**
	 * Get the complete human-readable name for a series in a plot.
	 * Sensors are given their user-entered name if they are defined.
	 * @param series The series name
	 * @return The human-readable name
	 */
	private String getPlotSeriesName(String series) {
		
		String result;
		
		switch (series) {
		case "dateTime": {
			result = ("Date/Time");
			break;
		}
		case("longitude"): {
			result = ("Longitude");
			break;
		}
		case("latitude"): {
			result = ("Latitude");
			break;
		}
		case("intakeTemp1"): {
			result = (instrument.getIntakeTempName1());
			break;
		}
		case("intakeTemp2"): {
			result = (instrument.getIntakeTempName2());
			break;
		}
		case("intakeTemp3"): {
			result = (instrument.getIntakeTempName3());
			break;
		}
		case("intakeTempMean"): {
			result = ("Mean Intake Temp");
			break;
		}
		case("salinity1"): {
			result = (instrument.getSalinityName1());
			break;
		}
		case("salinity2"): {
			result = (instrument.getSalinityName2());
			break;
		}
		case("salinity3"): {
			result = (instrument.getSalinityName3());
			break;
		}
		case("salinityMean"): {
			result = ("Mean Salinity");
			break;
		}
		case("eqt1"): {
			result = (instrument.getEqtName1());
			break;
		}
		case("eqt2"): {
			result = (instrument.getEqtName2());
			break;
		}
		case("eqt3"): {
			result = (instrument.getEqtName3());
			break;
		}
		case("eqtMean"): {
			result = ("Mean Equil Temp");
			break;
		}
		case("deltaT"): {
			result = ("Δ Temp");
			break;
		}
		case("eqp1"): {
			result = (instrument.getEqpName1());
			break;
		}
		case("eqp2"): {
			result = (instrument.getEqpName2());
			break;
		}
		case("eqp3"): {
			result = (instrument.getEqpName3());
			break;
		}
		case("eqpMean"): {
			result = ("Mean Equil Pres");
			break;
		}
		case("airFlow1"): {
			result = (instrument.getAirFlowName1());
			break;
		}
		case("airFlow2"): {
			result = (instrument.getAirFlowName2());
			break;
		}
		case("airFlow3"): {
			result = (instrument.getAirFlowName3());
			break;
		}
		case("waterFlow1"): {
			result = (instrument.getWaterFlowName1());
			break;
		}
		case("waterFlow2"): {
			result = (instrument.getWaterFlowName2());
			break;
		}
		case("waterFlow3"): {
			result = (instrument.getWaterFlowName3());
			break;
		}
		case("moistureMeasured"): {
			result = ("Moisture (Measured)");
			break;
		}
		case("moistureTrue"): {
			result = ("Moisture (True)");
			break;
		}
		case("pH2O"): {
			result = ("pH₂O");
			break;
		}
		case("co2Measured"): {
			result = ("Measured CO₂");
			break;
		}
		case("co2Dried"): {
			result = ("Dried CO₂");
			break;
		}
		case("co2Calibrated"): {
			result = ("Calibrated CO₂");
			break;
		}
		case("pCO2TEDry"): {
			result = ("pCO₂ TE Dry");
			break;
		}
		case("pCO2TEWet"): {
			result = ("pCO₂ TE Wet");
			break;
		}
		case("fCO2TE"): {
			result = ("fCO₂ TE");
			break;
		}
		case("fCO2Final"): {
			result = ("Final fCO₂");
			break;
		}
		default: {
			result = ("***UNKNOWN COLUMN " + series + "***");
		}
		}

		return result;
	}

/**
	 * Get the data bounds for the current data file
	 * @return The data bounds
	 * @see #dataBounds
	 */
	public String getDataBounds() {
		StringBuilder output = new StringBuilder();
		output.append('[');
		output.append(StringUtils.listToDelimited(dataBounds, ","));
		output.append(']');
		return output.toString();
	}
	
	/**
	 * Retrieve the list of selectable row numbers for this data file from the database.
	 * @see #selectableRowNumbers
	 */
	private void loadSelectableRowNumbers() throws Exception {
		selectableRowNumbers = FileDataInterrogator.getSelectableRowNumbers(ServletUtils.getDBDataSource(), fileId, getIncludeFlags());
	}
	
	/**
	 * Get the list of all selectable row numbers in the current data file as a javascript array string
	 * @return The row numbers
	 * @see #selectableRowNumbers
	 */
	public String getSelectableRowNumbers() {
		return selectableRowNumbers;
	}
	
	/**
	 * Returns the set of comments for the WOCE dialog
	 * @return The comments for the WOCE dialog
	 */
	public String getWoceCommentList() {
		return woceCommentList;
	}
	
	/**
	 * Dummy method to allow the data screen form to submit properly.
	 * We don't actually process the value.
	 * @param commentList The comment list from the form
	 */
	public void setWoceCommentList(String commentList) {
	}
	
	/**
	 * Get the worst flag set on the selected rows
	 * @return The worst flag on the selected rows
	 */
	public int getWorstSelectedFlag() {
		return worstSelectedFlag.getFlagValue();
	}
	
	/**
	 * Dummy method to allow the data screen form to submit properly.
	 * We don't actually process the value.
	 * @param flag The comment list from the form
	 */
	public void setWorstSelectedFlag(int flag) {
	}
	
	/**
	 * Generate the list of comments for the WOCE dialog
	 */
	public void generateWoceCommentList() {

		worstSelectedFlag = Flag.GOOD;
		
		StringBuilder list = new StringBuilder();
		list.append('[');
		
		try {
			CommentSet comments = FileDataInterrogator.getCommentsForRows(ServletUtils.getDBDataSource(), fileId, selectedRows);
			for (CommentSetEntry entry : comments) {
				list.append("[\"");
				list.append(entry.getComment());
				list.append("\",");
				list.append(entry.getFlag().getFlagValue());
				list.append(",");
				list.append(entry.getCount());
				list.append("],");
				
				if (entry.getFlag().moreSignificantThan(worstSelectedFlag)) {
					worstSelectedFlag = entry.getFlag();
				}
			}
			
		} catch (Exception e) {
			list.append("[\"Existing comments could not be retrieved\", -1, 4],");
			worstSelectedFlag = Flag.BAD;
		}
		
		// Remove the trailing comma from the last entry
		if (list.charAt(list.length() - 1) == ',') {
			list.deleteCharAt(list.length() - 1);
		}
		list.append(']');
		
		woceCommentList = list.toString();
	}
}
