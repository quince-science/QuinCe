package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;

import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for transient instrument operations, e.g.
 * listing instruments.
 * 
 * @author Steve Jones
 *
 */
public class InstrumentListBean extends BaseManagedBean {

	/**
	 * Returns a list of the instruments owned by the current user
	 * @return The instruments owned by the current user
	 */
	public List<InstrumentStub> getInstrumentList() {
		List<InstrumentStub> instruments = null;
		
		try {
			instruments = InstrumentDB.getInstrumentList(ServletUtils.getDBDataSource(), getUser());
		} catch (Exception e) {
			internalError(e);
		}
		return instruments;
	}
}
