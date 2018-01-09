package uk.ac.exeter.QuinCe.web.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecordFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.PlotPageBean;

/**
 * User QC bean
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class ManualQcBean extends PlotPageBean {

	/**
	 * Navigation to the calibration data plot page
	 */
	private static final String NAV_PLOT = "user_qc";
	
	/**
	 * Navigation to the dataset list
	 */
	private static final String NAV_DATASET_LIST = "dataset_list";

	/**
	 * The ID of the data set being processed
	 */
	private long datasetId;
	
	/**
	 * The data set being processed
	 */
	private DataSet dataset;
	
	/**
	 * The number of sensor columns
	 */
	private int sensorColumnCount = 0;
	
	/**
	 * The number of calculation columns
	 */
	private int calculationColumnCount = 0;
	
	/**
	 * The column containing the auto QC flag
	 */
	private int autoFlagColumn = 0;
	
	/**
	 * The column containing the manual QC flag
	 */
	private int userFlagColumn = 0;
	
	/**
	 * Initialise the required data for the bean
	 */
	@Override
	public void init() {
		try {
			dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the dataset ID
	 * @return The dataset ID
	 */
	public long getDatasetId() {
		return datasetId;
	}
	
	/**
	 * Set the dataset ID
	 * @param datasetId The dataset ID
	 */
	public void setDatasetId(long datasetId) {
		this.datasetId = datasetId;
	}
	
	/**
	 * Get the current DataSet object
	 * @return The data set
	 */
	public DataSet getDataset() {
		return dataset;
	}
	
	@Override
	protected List<Long> loadRowIds() throws Exception {
		return DataSetDataDB.getMeasurementIds(getDataSource(), datasetId);
	}

	@Override
	protected String buildTableHeadings() throws Exception {
		List<String> dataHeadings = DataSetDataDB.getDatasetDataColumnNames(getDataSource(), dataset);
		sensorColumnCount = dataHeadings.size() - 4; // Skip id, date, lat, lon
		List<String> calculationHeadings = CalculationDBFactory.getCalculationDB().getCalculationColumnHeadings();
		calculationColumnCount = calculationHeadings.size();
	
		StringBuilder headings = new StringBuilder();
		
		headings.append('[');

		for (String heading : dataHeadings) {
			headings.append('"');
			headings.append(heading);
			headings.append("\",");
		}
		
		for (int i = 0; i < calculationHeadings.size(); i++) {
			headings.append('"');
			headings.append(calculationHeadings.get(i));
			headings.append('"');
			
			headings.append(',');
		}
		
		headings.append("\"Automatic QC\",\"Automatic QC Message\",\"Manual QC\",\"Manual QC Message\"]");
		
		return headings.toString();
	}

	@Override
	protected String getScreenNavigation() {
		return NAV_PLOT;
	}

	@Override
	protected String loadPlotData(int plotIndex) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String buildSelectableRows() throws Exception {
		List<Long> ids = CalculationDBFactory.getCalculationDB().getSelectableMeasurementIds(getDataSource(), datasetId);
		
		StringBuilder result = new StringBuilder();
		
		result.append('[');
		
		for (int i = 0; i < ids.size(); i++) {
			result.append(ids.get(i));
			if (i < ids.size() - 1) {
				result.append(',');
			}
		}
		
		result.append(']');
		
		return result.toString();
	}

	@Override
	protected String buildPlotLabels(int plotIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String loadTableData(int start, int length) throws Exception {
		List<DataSetRawDataRecord> datasetData = DataSetDataDB.getMeasurements(getDataSource(), getDataset(), start, length);
		
		List<CalculationRecord> calculationData = new ArrayList<CalculationRecord>(datasetData.size());
		for (DataSetRawDataRecord record : datasetData) {
			CalculationRecord calcRecord = CalculationRecordFactory.makeCalculationRecord(datasetId, record.getId());
			CalculationDBFactory.getCalculationDB().getCalculationValues(getDataSource(), calcRecord);
			calculationData.add(calcRecord);
		}
		
		StringBuilder json = new StringBuilder();
		json.append('[');
		int rowId = start - 1;
		for (int i = 0; i < datasetData.size(); i++) {
			rowId++;
			
			DataSetRawDataRecord dsData = datasetData.get(i);
			CalculationRecord calcData = calculationData.get(i);
			
			json.append('{');
			json.append(StringUtils.makeJsonField("DT_RowId", "row" + rowId, true));
			json.append(',');
			
			int columnIndex = 0;
			json.append(StringUtils.makeJsonField(columnIndex, dsData.getId())); // ID
			json.append(',');

			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, DateTimeUtils.dateToLong(dsData.getDate()))); // Date
			json.append(',');

			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, dsData.getLongitude())); // Longitude
			json.append(',');

			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, dsData.getLatitude())); // Latitude
			json.append(',');

			for (Map.Entry<String, Double> entry : dsData.getSensorValues().entrySet()) {
				columnIndex++;
				Double value = entry.getValue();
				if (null == value) {
					json.append(StringUtils.makeJsonNull(columnIndex));
				} else {
					json.append(StringUtils.makeJsonField(columnIndex, entry.getValue()));
				}
				json.append(',');
			}

			List<String> calcColumns = calcData.getCalculationColumns();
			
			for (int j = 0; j < calcColumns.size(); j++) {
				columnIndex++;
				json.append(StringUtils.makeJsonField(columnIndex, calcData.getNumericValue(calcColumns.get(j))));
				json.append(',');
			}
			
			columnIndex++;
			autoFlagColumn = columnIndex;
			json.append(StringUtils.makeJsonField(columnIndex, calcData.getAutoFlag()));
			json.append(',');

			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, calcData.getAutoQCMessagesString(), true));
			json.append(',');
			
			columnIndex++;
			userFlagColumn = columnIndex;
			json.append(StringUtils.makeJsonField(columnIndex, calcData.getUserFlag()));
			json.append(',');
			
			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, calcData.getUserMessage(), true));
			
			json.append('}');
			if (i < datasetData.size() - 1) {
				json.append(',');
			}
		}
		
		json.append(']');
		
		return json.toString();
	}

	/**
	 * Finish the calibration data validation
	 * @return Navigation to the data set list
	 */
	public String finish() {
		return NAV_DATASET_LIST;
	}

	@Override
	public String getAdditionalTableData() {
		StringBuilder json = new StringBuilder();
		json.append("{\"sensorColumnCount\":");
		json.append(sensorColumnCount);
		json.append(",\"calculationColumnCount\":");
		json.append(calculationColumnCount);
		json.append(",\"flagColumns\":[");
		json.append(autoFlagColumn);
		json.append(',');
		json.append(userFlagColumn);
		json.append("]}");
		
		return json.toString();
	}
}
