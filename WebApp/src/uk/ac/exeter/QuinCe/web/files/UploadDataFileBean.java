package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import org.primefaces.context.RequestContext;

import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class UploadDataFileBean extends FileUploadBean {

	static {
		FORM_NAME = "uploadFileForm";
	}
	
	private long instrument;
	
	@Override
	public void processUploadedFile() {
		// We don't do anything immediately - the file
		// is handled by subsequent commands
	}
	
	/**
	 * Get the list of instruments owned by the current user
	 * @return The list of instruments
	 * @throws MissingParamException
	 * @throws DatabaseException
	 * @throws ResourceException
	 */
	public List<InstrumentStub> getInstrumentList() throws MissingParamException, DatabaseException, ResourceException {
		return InstrumentDB.getInstrumentList(ServletUtils.getDBDataSource(), getUser());
	}
	
	public long getInstrument() {
		return instrument;
	}
	
	public void setInstrument(long instrument) {
		this.instrument = instrument;
	}
	
	public void checkFile() {
		
		RequestContext context = RequestContext.getCurrentInstance();
		context.addCallbackParam("CHECK_OUTPUT", "This is the output");
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
