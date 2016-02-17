package uk.ac.exeter.QuinCe.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import uk.ac.exeter.QCRoutines.data.DataRecord;
import uk.ac.exeter.QCRoutines.data.DataRecordException;
import uk.ac.exeter.QCRoutines.messages.Message;

public class QCRecord extends DataRecord {

	public static final String COL_DATE = "Date";
	
	public static final String COL_LONGITUDE = "Longitude";
	
	public static final String COL_LATITUDE = "Latitude";
	
	public static final String COL_INTAKE_TEMP_1 = "INTAKE_TEMP_1";
	
	public static final String COL_INTAKE_TEMP_2 = "INTAKE_TEMP_2";
	
	public static final String COL_INTAKE_TEMP_3 = "INTAKE_TEMP_3";
	
	public static final String COL_SALINITY_1 = "SALINITY_1";
	
	public static final String COL_SALINITY_2 = "SALINITY_2";
	
	public static final String COL_SALINITY_3 = "SALINITY_3";
	
	public static final String COL_EQT_1 = "EQT_1";
	
	public static final String COL_EQT_2 = "EQT_2";
	
	public static final String COL_EQT_3 = "EQT_3";
	
	public static final String COL_EQP_1 = "EQP_1";
	
	public static final String COL_EQP_2 = "EQP_2";
	
	public static final String COL_EQP_3 = "EQP_3";
	
	public static final String COL_MOISTURE = "Moisture";
	
	public static final String COL_ATMOSPHERIC_PRESSURE = "Atmospheric Pressure";
	
	public static final String COL_CO2 = "CO2";
	
	public static final String COL_MEAN_INTAKE_TEMP = "Intake Temperature (Mean)";
	
	public static final String COL_MEAN_SALINITY = "Salinity (Mean)";
	
	public static final String COL_MEAN_EQT = "Equilibrator Temperature (Mean)";
	
	public static final String COL_MEAN_EQP = "Equilibrator Pressure (Mean)";
	
	public static final String COL_TRUE_MOISTURE = "True Moisture";
	
	public static final String COL_DRIED_CO2 = "Dried CO2";
	
	public static final String COL_CALIBRATED_CO2 = "Calibrated CO2";
	
	public static final String COL_PCO2_TE_DRY = "Dry pCO2 at Equilibrator Temperature";
	
	public static final String COL_PH2O = "pH2O";
	
	public static final String COL_PCO2_TE_WET = "pCO2 at 100% Humidity";
	
	public static final String COL_FCO2_TE = "fCO2 at Equilibrator Temperature";
	
	public static final String COL_FCO2 = "fCO2 at SST";
	
	public static final String COL_INTAKE_TEMP_1_USED = "INTAKE_TEMP_1_USED";
	
	public static final String COL_INTAKE_TEMP_2_USED = "INTAKE_TEMP_2_USED";
	
	public static final String COL_INTAKE_TEMP_3_USED = "INTAKE_TEMP_3_USED";
	
	public static final String COL_SALINITY_1_USED = "SALINITY_1_USED";
	
	public static final String COL_SALINITY_2_USED = "SALINITY_2_USED";
	
	public static final String COL_SALINITY_3_USED = "SALINITY_3_USED";
	
	public static final String COL_EQT_1_USED = "EQT_1_USED";
	
	public static final String COL_EQT_2_USED = "EQT_2_USED";
	
	public static final String COL_EQT_3_USED = "EQT_3_USED";
	
	public static final String COL_EQP_1_USED = "EQP_1_USED";
	
	public static final String COL_EQP_2_USED = "EQP_2_USED";
	
	public static final String COL_EQP_3_USED = "EQP_3_USED";
	
	private List<String> fieldValues;
	
	private static List<String> columnNames = null;
	
	private long dataFileId;
	
	private Instrument instrument;
	
	private int woceFlag = FLAG_NOT_SET;
	
	private List<Message> woceMessages;
	
	static {
		if (null == columnNames) {
			columnNames = new ArrayList<String>();
			columnNames.add(COL_DATE);
			columnNames.add(COL_LONGITUDE);
			columnNames.add(COL_LATITUDE);
			columnNames.add(COL_INTAKE_TEMP_1);
			columnNames.add(COL_INTAKE_TEMP_2);
			columnNames.add(COL_INTAKE_TEMP_3);
			columnNames.add(COL_SALINITY_1);
			columnNames.add(COL_SALINITY_2);
			columnNames.add(COL_SALINITY_3);
			columnNames.add(COL_EQT_1);
			columnNames.add(COL_EQT_2);
			columnNames.add(COL_EQT_3);
			columnNames.add(COL_EQP_1);
			columnNames.add(COL_EQP_2);
			columnNames.add(COL_EQP_3);
			columnNames.add(COL_MOISTURE);
			columnNames.add(COL_ATMOSPHERIC_PRESSURE);
			columnNames.add(COL_CO2);
			columnNames.add(COL_MEAN_INTAKE_TEMP);
			columnNames.add(COL_MEAN_SALINITY);
			columnNames.add(COL_MEAN_EQT);
			columnNames.add(COL_MEAN_EQP);
			columnNames.add(COL_TRUE_MOISTURE);
			columnNames.add(COL_DRIED_CO2);
			columnNames.add(COL_CALIBRATED_CO2);
			columnNames.add(COL_PCO2_TE_DRY);
			columnNames.add(COL_PH2O);
			columnNames.add(COL_PCO2_TE_WET);
			columnNames.add(COL_FCO2_TE);
			columnNames.add(COL_FCO2);
			columnNames.add(COL_INTAKE_TEMP_1_USED);
			columnNames.add(COL_INTAKE_TEMP_2_USED);
			columnNames.add(COL_INTAKE_TEMP_3_USED);
			columnNames.add(COL_SALINITY_1_USED);
			columnNames.add(COL_SALINITY_2_USED);
			columnNames.add(COL_SALINITY_3_USED);
			columnNames.add(COL_EQT_1_USED);
			columnNames.add(COL_EQT_2_USED);
			columnNames.add(COL_EQT_3_USED);
			columnNames.add(COL_EQP_1_USED);
			columnNames.add(COL_EQP_2_USED);
			columnNames.add(COL_EQP_3_USED);
		}
	}
	
	public QCRecord(long dataFileId, List<String> dataFields, int lineNumber, Instrument instrument, int qcFlag, ) throws DataRecordException {
		super(dataFields, lineNumber);
		this.dataFileId = dataFileId;
		this.instrument = instrument;
		this.woceMessages = new ArrayList<Message>();
	}

	@Override
	public DateTime getTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getLongitude() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLatitude() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void setDataValues(List<String> dataFields) throws DataRecordException {
		if (dataFields.size() != columnNames.size()) {
			throw new DataRecordException("Incorrect number of data fields");
		} else {
			fieldValues = dataFields;
		}
	}

	@Override
	public String getValue(String columnName) throws DataRecordException {
		int columnIndex = columnNames.indexOf(columnName);
		
		if (columnIndex < 0) {
			throw new DataRecordException("Column '" + columnName + "' does not exist");
		} else {
			return fieldValues.get(columnIndex);
		}
	}
	
	public long getDataFileId() {
		return dataFileId;
	}
	
	public static String getColumnDisplayName(String columnName, Instrument instrument) {
		// TODO Make display names
		return "TO DO!";
	}
	
	public String getColumnDisplayName(String columnName) {
		return getColumnDisplayName(columnName, instrument);
	}
	
	public int getQCFlag() {
		return getFlag();
	}
	
	public int getWoceFlag() {
		return woceFlag;
	}
	
	public void setFlag(int flag) {
		super.setFlag(flag);
		if (flag != FLAG_GOOD) {
			woceFlag = flag;
		}
	}
}
