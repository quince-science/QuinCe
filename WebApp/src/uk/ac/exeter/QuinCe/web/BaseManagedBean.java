package uk.ac.exeter.QuinCe.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.primefaces.event.NodeCollapseEvent;
import org.primefaces.event.NodeExpandEvent;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.User.UserPreferences;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.User.LoginBean;
import uk.ac.exeter.QuinCe.web.datasets.DataSetsBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Several Managed Beans are used in the QuinCe application. This abstract class
 * provides a set of useful methods for use by inheriting concrete bean classes.
 *
 * <p>
 * <b>WARNING: Performing a "Find Usages" search for some of these methods will
 * not return any results. This <i>DOES NOT</i> mean that the methods are not
 * being used; they can be called directly from JSP pages, which your IDE may
 * not be capable of detecting.</b>
 * </p>
 */
public abstract class BaseManagedBean {

  /**
   * The default result for successful completion of a process. This will be
   * used in the {@code faces-config.xml} file to determine the next navigation
   * destination.
   */
  public static final String SUCCESS_RESULT = "Success";

  /**
   * The default result for indicating that an error occurred during a
   * processing action. This will be used in the {@code faces-config.xml} file
   * to determine the next navigation destination.
   *
   * @see #internalError(Throwable)
   */
  public static final String INTERNAL_ERROR_RESULT = "InternalError";

  /**
   * The default result for indicating that the data validation failed for a
   * given processing action. This will be used in the {@code faces-config.xml}
   * file to determine the next navigation destination.
   */
  public static final String VALIDATION_FAILED_RESULT = "ValidationFailed";

  /**
   * The session attribute where the current full {@link Instrument} is stored.
   */
  private static final String CURRENT_FULL_INSTRUMENT_ATTR = "currentFullInstrument";

  /**
   * Session attribute to store the last date selected by the user.
   *
   * Used to pre-populate date selectors, since many actions happen for the same
   * date.
   */
  private static final String LAST_DATE_ATTR = "lastDate";

  /**
   * The {@link Instrument}s owned by the user.
   */
  private List<Instrument> instruments;

  /**
   * Long date format for displaying timestamps.
   */
  private final String longDateFormat = "yyyy-MM-dd HH:mm:ss";

  /**
   * Set this to force reloading the {@link Instrument} even though it is
   * already set.
   */
  private boolean forceInstrumentReload = false;

  /**
   * Information for a progress bar.
   */
  protected Progress progress = new Progress();

  /**
   * Flag to indicate the successful completion of an Ajax call.
   */
  protected boolean ajaxOK = false;

  /**
   * Set a message that can be displayed to the user on a form.
   *
   * @param componentID
   *          The component ID to which the message relates. Can be {@code null}
   *          if the message is not related to a specific component.
   * @param messageString
   *          The message.
   * @see #getComponentID(String)
   */
  protected void setMessage(String componentID, String messageString) {
    FacesContext context = FacesContext.getCurrentInstance();
    FacesMessage message = new FacesMessage();
    message.setSeverity(FacesMessage.SEVERITY_ERROR);
    message.setSummary(messageString);
    message.setDetail(messageString);
    context.addMessage(componentID, message);
  }

  /**
   * Generates a JSF component ID for a given form input name by combining it
   * with the bean's form name.
   *
   * @param componentName
   *          The form input name.
   * @return The JSF component ID.
   * @see #getFormName()
   */
  protected String getComponentID(String componentName) {
    return getFormName() + ":" + componentName;
  }

  /**
   * Register an internal error. This places the error stack trace in the
   * messages, and sends back a result that will redirect the application to the
   * internal error page.
   *
   * <p>
   * This should only be used for errors that can't be handled properly, e.g.
   * database failures and the like. The user will be directed to a general
   * error page, from which their only option will be to return to the login
   * page to start a new session.
   * </p>
   *
   * @param error
   *          The error.
   * @return A result string that will direct to the internal error page.
   * @see #INTERNAL_ERROR_RESULT
   */
  public String internalError(Throwable error) {
    setMessage("STACK_TRACE", StringUtils.stackTraceToString(error));
    if (null != error.getCause()) {
      setMessage("CAUSE_STACK_TRACE",
        StringUtils.stackTraceToString(error.getCause()));
    }
    ExceptionUtils.printStackTrace(error);
    return INTERNAL_ERROR_RESULT;
  }

  /**
   * Retrieve a parameter from the current request.
   *
   * <p>
   * This is a shortcut to looking up values in
   * {@link ExternalContext#getRequestParameterMap}.
   * </p>
   *
   * @param paramName
   *          The name of the parameter to retrieve.
   * @return The parameter value.
   */
  public String getRequestParameter(String paramName) {
    return FacesContext.getCurrentInstance().getExternalContext()
      .getRequestParameterMap().get(paramName);
  }

  /**
   * Retrieves the current HTTP Session object.
   *
   * @return The HTTP Session object.
   */
  public HttpSession getSession() {
    return (HttpSession) FacesContext.getCurrentInstance().getExternalContext()
      .getSession(true);
  }

  /**
   * Directly navigate to a given result without going through the normal
   * routing process.
   *
   * @param navigation
   *          The navigation result.
   */
  public void directNavigate(String navigation) {
    ConfigurableNavigationHandler nav = (ConfigurableNavigationHandler) FacesContext
      .getCurrentInstance().getApplication().getNavigationHandler();
    nav.performNavigation(navigation);
  }

  /**
   * Evaluate a JSF Expression Language expression and return its value.
   *
   * @param expression
   *          The EL expression.
   * @return The result of evaluating the expression.
   */
  public String getELValue(String expression) {
    FacesContext context = FacesContext.getCurrentInstance();
    return context.getApplication().evaluateExpressionGet(context, expression,
      String.class);
  }

  /**
   * Returns the {@link User} object for the current session.
   *
   * @return The {@link User} object.
   */
  public User getUser() {
    return (User) getSession().getAttribute(LoginBean.USER_SESSION_ATTR);
  }

  /**
   * Get the user's preferences for the currently logged in {@link User}.
   *
   * @return The user's preferences.
   */
  public UserPreferences getUserPrefs() {
    UserPreferences result = (UserPreferences) getSession()
      .getAttribute(LoginBean.USER_PREFS_ATTR);
    if (null == result) {
      result = new UserPreferences(getUser().getDatabaseID());
    }

    return result;
  }

  /**
   * Accessing components requires the name of the form that they are in as well
   * as their own name.
   *
   * <p>
   * Most beans will only have one form, so this method will provide the name of
   * that form. Beans with multiple forms will need to employ a different
   * methodology.
   * </p>
   *
   * <p>
   * This class provides a default form name. Override the method to provide a
   * name specific to the bean.
   * </p>
   *
   * @return The form name for the bean.
   */
  protected String getFormName() {
    return "DEFAULT_FORM";
  }

  /**
   * Get the URL stub for the application.
   *
   * @return The application URL stub.
   */
  public String getUrlStub() {
    // TODO This can probably be replaced with something like
    // FacesContext.getCurrentInstance().getExternalContext().getRe‌​questContextPath()
    return ResourceManager.getInstance().getConfig().getProperty("app.urlstub");
  }

  /**
   * Get a {@link DataSource} from which database {@link java.sql.Connection}s
   * can be obtained.
   *
   * @return A {@link DataSource}.
   */
  public DataSource getDataSource() {
    return ResourceManager.getInstance().getDBDataSource();
  }

  /**
   * Get the application configuration.
   *
   * @return The application configuration.
   * @see ResourceManager#getConfig()
   */
  protected Properties getAppConfig() {
    return ResourceManager.getInstance().getConfig();
  }

  /**
   * Initialise/reset the cached instrument details.
   */
  public void initialiseInstruments() {
    // Load the instruments list. Set the current instrument if it isn't already
    // set.
    try {
      instruments = InstrumentDB.getInstrumentList(getDataSource(), getUser());

      boolean userInstrumentExists = false;
      long currentUserInstrument = getUserPrefs().getLastInstrument();
      if (currentUserInstrument != -1) {
        for (Instrument instrument : instruments) {
          if (instrument.getId() == currentUserInstrument) {
            userInstrumentExists = true;
            break;
          }
        }
      }

      if (!userInstrumentExists) {
        if (instruments.size() > 0) {
          setCurrentInstrumentId(instruments.get(0).getId());
        } else {
          setCurrentInstrumentId(-1);
        }
      }

      Instrument currentInstrument = (Instrument) getSession()
        .getAttribute(CURRENT_FULL_INSTRUMENT_ATTR);
      if (
      // if forceInstrumentReload is set, always reload
      forceInstrumentReload ||

      // If the current instrument is now different to the one held in the
      // session, remove it so it will get reloaded on next access
        (null != currentInstrument
          && currentInstrument.getId() != currentUserInstrument)) {
        getSession().removeAttribute(CURRENT_FULL_INSTRUMENT_ATTR);
        setForceInstrumentReload(false);
      }
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  /**
   * Get the {@link Instrument}s owned by the current {@link User}.
   *
   * <p>
   * If the current user has administration permissions, the returned list will
   * contain all the {@link Instrument}s registered for all users.
   * </p>
   *
   * @return The list of {@link Instrument}s.
   * @see User#isAdminUser()
   */
  public List<Instrument> getInstruments() {
    if (null == instruments) {
      initialiseInstruments();
    }

    return instruments;
  }

  /**
   * Get the {@link Instrument} object with the specified database ID.
   *
   * <p>
   * The {@link Instrument} must be present in the list produced by
   * {@link #getInstruments()}; otherwise {@code null} will be returned.
   * </p>
   *
   * @param instrumentId
   *          The {@link Instrument}'s database ID.
   * @return The {@link Instrument} object.
   */
  public Instrument getInstrument(long instrumentId) {
    return getInstruments().stream().filter(i -> i.getId() == instrumentId)
      .findFirst().orElse(null);
  }

  /**
   * Retrieve the {@link Instrument} object for the instrument that is currently
   * being manipulated by the user.
   *
   * <p>
   * The "current instrument" is set whenever the user selects an instrument on
   * the DataSets or Data Files page, or calibrations are edited for an
   * instrument. When the user creates a new instrument, it is automatically set
   * as the current instrument.
   * </p>
   *
   * <p>
   * The current instrument persists until an action is taken that causes a new
   * instrument to be set as the current instrument. This will also persist
   * across user sessions.
   * </p>
   *
   * @return The current {@link Instrument}.
   * @see #setCurrentInstrumentId(long)
   */
  public Instrument getCurrentInstrument() {
    // Get the current instrument from the session
    Instrument currentFullInstrument = (Instrument) getSession()
      .getAttribute(CURRENT_FULL_INSTRUMENT_ATTR);

    try {
      // If there is nothing in the session, get the instrument from
      // the user prefs and put it in the session
      if (null == currentFullInstrument) {
        long currentInstrumentId = getUserPrefs().getLastInstrument();
        for (Instrument instrument : getInstruments()) {
          if (instrument.getId() == currentInstrumentId) {
            currentFullInstrument = instrument;
            getSession().setAttribute(CURRENT_FULL_INSTRUMENT_ATTR,
              currentFullInstrument);
          }
        }

        // If we still don't have an instrument, then get the first one from the
        // list
        if (null == currentFullInstrument) {
          if (instruments.size() == 0) {
            getUserPrefs().setLastInstrument(-1);
          } else {
            currentFullInstrument = instruments.get(0);
            getSession().setAttribute(CURRENT_FULL_INSTRUMENT_ATTR,
              currentFullInstrument);
            getUserPrefs().setLastInstrument(currentFullInstrument.getId());
          }
        }
      }
    } catch (Exception e) {
      // Swallow the error, but dump it
      ExceptionUtils.printStackTrace(e);
      currentFullInstrument = null;
    }

    return currentFullInstrument;
  }

  /**
   * Get the database ID of the instrument that is currently selected by the
   * user.
   *
   * @return The current instrument ID.
   * @see #getCurrentInstrument()
   */
  public long getCurrentInstrumentId() {

    long result = -1;

    if (null != getCurrentInstrument()) {
      result = getCurrentInstrument().getId();
    }

    return result;
  }

  /**
   * Set the instrument that is currently selected by the user.
   *
   * @param currentInstrumentId
   *          The instrument's database ID.
   * @see #getCurrentInstrument()
   */
  public void setCurrentInstrumentId(long currentInstrumentId) {

    if (null == instruments) {
      initialiseInstruments();
    }

    // Only do something if the instrument has actually changed
    if (getUserPrefs().getLastInstrument() != currentInstrumentId) {

      // Update the user preferences
      getUserPrefs().setLastInstrument(currentInstrumentId);

      // Remove the current instrument from the session;
      // It will be replaced on next access
      getSession().removeAttribute(CURRENT_FULL_INSTRUMENT_ATTR);

      try {
        UserDB.savePreferences(getDataSource(), getUserPrefs());
      } catch (Exception e) {
        ExceptionUtils.printStackTrace(e);
      }
    }
  }

  /**
   * Get the long form format for displaying timestamps.
   *
   * @return The long date format.
   */
  public String getLongDateFormat() {
    return longDateFormat;
  }

  /**
   * Determine whether or not there are instruments available for this user.
   *
   * @return {@code true} if the user has any instruments; {@code false} if not.
   */
  public boolean getHasInstruments() {
    List<Instrument> instruments = getInstruments();
    return (null != instruments && instruments.size() > 0);
  }

  /**
   * Set to true to make full instrument reload when
   * {@link #initialiseInstruments()}.
   *
   * <p>
   * This is automatically reset to {@code false} after
   * {@link #initialiseInstruments()} is called.
   * </p>
   *
   * @param forceReload
   *          The flag value.
   */
  public void setForceInstrumentReload(boolean forceReload) {
    this.forceInstrumentReload = forceReload;
  }

  /**
   * Get the list of available run type categories
   *
   * @return The run type categories
   * @throws ResourceException
   *           If the Resource Manager cannot be accessed
   */
  public List<RunTypeCategory> getRunTypeCategories() throws ResourceException {
    List<RunTypeCategory> allCategories = ServletUtils.getResourceManager()
      .getRunTypeCategoryConfiguration().getCategories(true, true);

    return removeUnusedVariables(allCategories);
  }

  /**
   * Remove from a list of Run Type Categories any category that is not related
   * to the {@link Variable}s assigned to the current {@link Instrument}.
   *
   * @param categories
   *          The Run Type Categories to be filtered.
   * @return The filtered Run Type Categories.
   */
  protected List<RunTypeCategory> removeUnusedVariables(
    List<RunTypeCategory> categories) {
    List<Long> instrumentVariables = getInstrumentVariableIDs();
    return categories.stream()
      .filter(c -> c.getType() < 0 || instrumentVariables.contains(c.getType()))
      .collect(Collectors.toList());
  }

  /**
   * Determine whether or not the current user can approve datasets for export
   *
   * @return {@code true} if the user can approve datasets; {@code false} if not
   */
  public boolean isApprovalUser() {
    return getUser().isApprovalUser();
  }

  /**
   * Get the IDs of the variables assigned to the current {@link Instrument}.
   *
   * @return The variable IDs.
   * @see #getCurrentInstrument()
   */
  protected List<Long> getInstrumentVariableIDs() {
    return getCurrentInstrument().getVariables().stream().map(Variable::getId)
      .collect(Collectors.toList());
  }

  /**
   * Event handler for node expansion in a PrimeFaces Tree.
   *
   * <p>
   * Sets the node's expanded status so it persists through tree updates.
   * Activated with the following:
   * </p>
   * <p>
   * {@code <p:ajax event="expand" listener= "#{[beanName].treeNodeExpand}"/>}
   * </p>
   *
   * @param event
   *          The node expansion event.
   */
  public void treeNodeExpand(NodeExpandEvent event) {
    event.getTreeNode().setExpanded(true);
  }

  /**
   * Event handler for node collapse in a PrimeFaces Tree.
   *
   * <p>
   * Sets the node's expanded status so it persists through tree updates.
   * Activated with the following:
   * </p>
   * <p>
   * {@code <p:ajax event="collapse" listener=
   * "#{[beanName].treeNodeCollapse}"/>}
   * </p>
   *
   * @param event
   *          The node collapse event.
   */
  public void treeNodeCollapse(NodeCollapseEvent event) {
    event.getTreeNode().setExpanded(false);
  }

  /**
   * A special little method that will trigger an error.
   *
   * <p>
   * Used for testing the error page. There is a link on the login page that's
   * usually hidden. Unhide it to throw an error on demand.
   * </p>
   *
   * @return The formatted error string.
   */
  public String throwError() {
    return internalError(new BeanException("Test exception"));
  }

  /**
   * Does absolutely nothing.
   *
   * <p>
   * This is useful for various things, like swallowing trigger events that need
   * to be sent but don't actually need to do anything.
   * </p>
   */
  public void noop() {
    // Do nothing
  }

  /**
   * Get the navigation result to take the user to the DataSets list page.
   *
   * <p>
   * The exact navigation string required depends on the current state of the
   * application, so this method calculates what it should be in any given
   * situation.
   * </p>
   *
   * @return The navigation string.
   */
  public String getDatasetListNavigation() {
    String result = (String) getSession()
      .getAttribute(DataSetsBean.CURRENT_VIEW_ATTR);
    if (null == result) {
      result = DataSetsBean.NAV_DATASET_LIST;
    }

    return result;
  }

  /**
   * Set the last used date session attribute for later use.
   *
   * <p>
   * This is used when the user is performing actions that require a timestamp
   * to be entered (e.g. editing calibrations). The timestamp is stored so that
   * the next action can be pre-populated with it, since many such actions are
   * often repeated for different parameters but will use the same tiemstamp.
   * </p>
   *
   * @param date
   *          The timestamp.
   */
  public void setLastDate(LocalDateTime date) {
    getSession().setAttribute(LAST_DATE_ATTR, date);
  }

  /**
   * Get the last used date from the session.
   *
   * If the date has not been set, returns the current date.
   *
   * @return The last date used date.
   * @see #setLastDate(LocalDateTime)
   */
  public LocalDateTime getLastDate() {
    LocalDateTime result = (LocalDateTime) getSession()
      .getAttribute(LAST_DATE_ATTR);
    if (null == result) {
      result = LocalDateTime.now().toLocalDate().atStartOfDay();
    }
    return result;
  }

  /**
   * Get the progress bar information.
   *
   * @return The progress bar information.
   */
  public Progress getProgress() {
    return progress;
  }

  /**
   * Get the HTTP Response object for the current request.
   *
   * @return The response object.
   */
  protected HttpServletResponse getResponse() {
    return (HttpServletResponse) FacesContext.getCurrentInstance()
      .getExternalContext().getResponse();
  }

  /**
   * Set the HTTP response code for the current request.
   *
   * @param code
   *          The response code.
   */
  protected void setResponseCode(int code) {
    getResponse().setStatus(code);
  }

  /**
   * Retrieve the state of the current AJAX activity.
   *
   * <p>
   * Some front end methods using AJAX calls need to know if those calls
   * completed successfully. Server side code can set this flag, which the front
   * end can examine to get the state of the call.
   * </p>
   *
   * @return The state of the {@link #ajaxOK} flag.
   */
  public boolean getAjaxOK() {
    return ajaxOK;
  }

  /**
   * Capture and swallow front-end attempts to set the {@link #ajaxOK} flag.
   *
   * <p>
   * No action is performed, since the front end should not set this flag.
   * </p>
   *
   * @param ok
   *          The flag value (ignored).
   */
  public void setAjaxOK(boolean ok) {
    // Ignore all attempts to set this value
  }
}
