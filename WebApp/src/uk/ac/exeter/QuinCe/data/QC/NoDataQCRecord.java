package uk.ac.exeter.QuinCe.data.QC;

import java.util.List;

import org.joda.time.DateTime;

import uk.ac.exeter.QCRoutines.config.ColumnConfig;
import uk.ac.exeter.QCRoutines.data.DataRecordException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QuinCe.data.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * A special instance of the QCRecord class, which does not contain any data
 * @author Steve Jones
 *
 */
public class NoDataQCRecord extends QCRecord {

	public NoDataQCRecord(long dataFileId, Instrument instrument, ColumnConfig columnConfig, int lineNumber,
			boolean intakeTemp1Used, boolean intakeTemp2Used, boolean intakeTemp3Used,
			boolean salinity1Used, boolean salinity2Used, boolean salinity3Used,
			boolean eqt1Used, boolean eqt2Used, boolean eqt3Used,
			boolean eqp1Used, boolean eqp2Used, boolean eqp3Used,
			Flag qcFlag, List<Message> qcComments, Flag woceFlag, String woceComment) throws DataRecordException, MessageException {
		
		super(dataFileId, instrument, columnConfig, lineNumber, null,
		intakeTemp1Used, intakeTemp2Used, intakeTemp3Used,
		salinity1Used, salinity2Used, salinity3Used,
		eqt1Used, eqt2Used, eqt3Used,
		eqp1Used, eqp2Used, eqp3Used,
		qcFlag, qcComments, woceFlag, woceComment);
	}

	@Override
	public double getLatitude() {
		return RawDataDB.MISSING_VALUE;
	}
	
	@Override
	public double getLongitude() {
		return RawDataDB.MISSING_VALUE;
	}
	
	@Override
	public DateTime getTime() {
		return null;
	}
	
	@Override
	protected void setDataValues(List<String> values) {
		// Do nothing
	}
}
