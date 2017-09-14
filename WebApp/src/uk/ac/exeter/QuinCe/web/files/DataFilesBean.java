package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;

/**
 * Bean for handling raw data files
 * @author Steve Jones
 */
@ManagedBean
@SessionScoped
public class DataFilesBean extends FileUploadBean {
	
	/**
	 * The instruments owned by the user
	 */
	private List<InstrumentStub> instruments;
	
	@Override
	public void processUploadedFile() {
		// TODO Auto-generated method stub	
	}

	/**
	 * Get the list of instruments owned by the user
	 * @return The list of instruments
	 * @throws MissingParamException If any internal calls are missing required parameters
	 * @throws DatabaseException If a database error occurs
	 */
	public List<InstrumentStub> getInstruments() throws MissingParamException, DatabaseException {
		if (null == instruments) {
			instruments = InstrumentDB.getInstrumentList(getDataSource(), getUser());
		}
		
		return instruments;
	}
	
	/**
	 * Get the instrument that the user is currently viewing
	 * @return The current instrument
	 */
	public long getCurrentInstrument() {
		return getUserPrefs().getLastInstrument();
	}
	
	/**
	 * Set the current instrument
	 * @param currentInstrument The current instrument
	 */
	public void setCurrentInstrument(long currentInstrument) {
		getUserPrefs().setLastInstrument(currentInstrument);
	}
}
