package uk.ac.exeter.QuinCe.web;

import java.util.List;

import javax.el.ELContext;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * The ServletUtilsBean holds utility functions and data that needs to last
 * throughout the session, and can be used globally by the QuinCe application.
 *
 * @author Jonas F. Henriksen
 *
 */
@ManagedBean(name = "utils")
@SessionScoped
public class ServletUtilsBean {

  /**
   * Application site name
   */
  private final String siteName = "QuinCe";

  /**
   * Use this to get the GlobalSessionData object for this session.
   * 
   * @return an instance of the GlobalSessionData
   */
  public static ServletUtilsBean getInstance() {
    ELContext elContext = FacesContext.getCurrentInstance().getELContext();
    return (ServletUtilsBean) FacesContext.getCurrentInstance().getApplication()
      .getELResolver().getValue(elContext, null, "global");
  }

  /**
   * Shorthand to retrieve the run type categories from the configuration
   * 
   * @return
   * @throws ResourceException
   */
  public List<RunTypeCategory> getRunTypeCategories() throws ResourceException {
    return ServletUtils.getResourceManager().getRunTypeCategoryConfiguration()
      .getCategories(true, true);
  }

  /**
   * The site name for the QuinCe application can be used throughout the
   * application for labels and headers
   *
   * @return
   */
  public String getSiteName() {
    return siteName;
  }
}
