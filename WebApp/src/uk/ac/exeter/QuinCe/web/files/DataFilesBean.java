package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
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
	 * Initialise the bean
	 */
	@PostConstruct
	public void initialise() {
		// Load the instruments list. Set the current instrument if it isn't already set.
		try {
			instruments = InstrumentDB.getInstrumentList(getDataSource(), getUser());
			if (getCurrentInstrument() == -1 && instruments.size() > 0) {
				setCurrentInstrument(instruments.get(0).getId());
			}
		} catch (Exception e) {
			// Fail quietly, but print the log
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the list of instruments owned by the user
	 * @return The list of instruments
	 */
	public List<InstrumentStub> getInstruments() {
		if (null == instruments) {
			initialise();
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
