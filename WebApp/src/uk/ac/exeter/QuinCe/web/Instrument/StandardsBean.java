package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.Date;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.data.StandardConcentration;
import uk.ac.exeter.QuinCe.data.StandardStub;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.Instrument.GasStandardDB;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;
import uk.ac.exeter.QuinCe.web.validator.ExistingDateValidator;

/**
 * Bean for handling viewing and entry of gas standards
 * @author Steve Jones
 *
 */
public class StandardsBean extends BaseManagedBean {

	/**
	 * Navigation to the standard editor
	 */
	private static final String PAGE_STANDARD_EDITOR = "standardEditor";
	
	/**
	 * The list of concentrations to be entered.
	 * There will be one entry for each of the instrument's gas standard run types
	 */
	private List<StandardConcentration> concentrations;
	
	/**
	 * The date of the gas standard deployment
	 */
	private Date deployedDate;
	
	/**
	 * The ID of the standard record being edited. If this is a new
	 * standard, this will be NO_DATABASE_RECORD.
	 */
	private long chosenStandard = DatabaseUtils.NO_DATABASE_RECORD;
	
	/**
	 * Empty constructor
	 */
	public StandardsBean() {
		// Do nothing
	}
	
	/**
	 * Clear the bean's data
	 * @throws ResourceException 
	 * @throws DatabaseException 
	 * @throws MissingParamException 
	 */
	private void clearData() throws MissingParamException, DatabaseException, ResourceException {
		deployedDate = null;
		chosenStandard = DatabaseUtils.NO_DATABASE_RECORD;
		concentrations = null;
		getSession().removeAttribute(ExistingDateValidator.ATTR_ALLOWED_DATE);
	}
	
	/**
	 * Set up a new, empty standard and navigate to the editor
	 * and set the date to today.
	 * @return The navigation result
	 */
	public String newStandard() {
		try {
			clearData();
			chosenStandard = DatabaseUtils.NO_DATABASE_RECORD;
			deployedDate = new Date();
			InstrumentStub instrStub = (InstrumentStub) getSession().getAttribute(InstrumentListBean.ATTR_CURRENT_INSTRUMENT);
			Instrument instrument = instrStub.getFullInstrument();
			concentrations = StandardConcentration.initConcentrations(instrument);
		} catch (Exception e) {
			internalError(e);
		}

		return PAGE_STANDARD_EDITOR;
	}
	
	/**
	 * Store a standard in the database
	 * @return The navigation back to the standards list
	 */
	public String saveStandard() {
		String result = InstrumentListBean.PAGE_STANDARDS;
		
		try {
			if (chosenStandard == DatabaseUtils.NO_DATABASE_RECORD) {
				GasStandardDB.addStandard(ServletUtils.getDBDataSource(), getCurrentInstrumentID(), deployedDate, concentrations);
			} else {
				GasStandardDB.updateStandard(ServletUtils.getDBDataSource(), chosenStandard, deployedDate, concentrations);
			}
		} catch (Exception e) {
			result = internalError(e);
		} finally {
			try {
				clearData();
			} catch (Exception e) {
				return internalError(e);
			}
		}

		return result;
	}
	
	/**
	 * Cancels an edit action and returns to the standards list
	 * @return The navigation result
	 */
	public String cancelEdit() {
		try {
			clearData();
		} catch (Exception e) {
			return internalError(e);
		}
		return InstrumentListBean.PAGE_STANDARDS;
	}
	
	/**
	 * Begin editing an existing standard
	 * @return The navigation to the standard editor page
	 */
	public String editStandard() {
		try {
			StandardStub stub = GasStandardDB.getStandardStub(ServletUtils.getDBDataSource(), chosenStandard);
			deployedDate = stub.getDeployedDate();
			
			// Store the date in the session so the date validator knows to skip it
			getSession().setAttribute(ExistingDateValidator.ATTR_ALLOWED_DATE, deployedDate);
			
			concentrations = GasStandardDB.getConcentrations(ServletUtils.getDBDataSource(), stub);
			
		} catch (Exception e) {
			return internalError(e);
		}

		return PAGE_STANDARD_EDITOR;
	}
	
	public String deleteStandard() {
		try {
			GasStandardDB.deleteStandard(ServletUtils.getDBDataSource(), chosenStandard);
			clearData();
		} catch (Exception e) {
			return internalError(e);
		}
		
		return InstrumentListBean.PAGE_STANDARDS;
	}
	
	//////// *** GETTERS AND SETTERS *** ////////
	
	/**
	 * Returns the list of standard coefficient objects
	 * @return The list of standard coefficient objects
	 */
	public List<StandardConcentration> getConcentrations() {
		return concentrations;
	}
	
	/**
	 * Returns the deployed date
	 * @return The deployed date
	 */
	public Date getDeployedDate() {
		return deployedDate;
	}
	
	/**
	 * Sets the deployed date
	 * @param deployedDate The deployed date
	 */
	public void setDeployedDate(Date deployedDate) {
		this.deployedDate = deployedDate;
	}
	
	/**
	 * Returns a Date object representing today. Used
	 * to limit the standard date picker.
	 * @return A Date object representing today
	 */
	public Date getToday() {
		return new Date();
	}
	
	/**
	 * Returns the list of standards
	 * @return The list of standards
	 * @throws ResourceException 
	 * @throws DatabaseException 
	 * @throws MissingParamException 
	 */
	public List<StandardStub> getStandardsList() throws MissingParamException, DatabaseException, ResourceException {
		return GasStandardDB.getStandardList(ServletUtils.getDBDataSource(), getCurrentInstrumentID());
	}

	/**
	 * Retrieve the ID of the current instrument from the session
	 * @return The current instrument ID
	 */
	private long getCurrentInstrumentID() {
		InstrumentStub instrStub = (InstrumentStub) getSession().getAttribute(InstrumentListBean.ATTR_CURRENT_INSTRUMENT);
		return instrStub.getId();
	}
	
	/**
	 * Returns the database ID of the chosen standard
	 * @return The standard ID
	 */
	public long getChosenStandard() {
		return chosenStandard;
	}
	
	/**
	 * Sets the database ID of the chosen standard
	 * @param chosenStandard The standard ID
	 */
	public void setChosenStandard(long chosenStandard) {
		this.chosenStandard = chosenStandard;
	}
}
