package uk.ac.exeter.QuinCe.web;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserPreferences;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.User.LoginBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Several Managed Beans are used in the QuinCe application. This abstract class provides a
 * set of useful methods for inheriting concrete bean classes to use.
 * @author Steve Jones
 *
 */
public abstract class BaseManagedBean {

  /**
   * The default result for successful completion of a process. This will be used in the
   * {@code faces-config.xml} file to determine the next navigation destination.
   */
  public static final String SUCCESS_RESULT = "Success";

  /**
   * The default result for indicating that an error occurred during a processing action.
   * This will be used in the {@code faces-config.xml} file to determine the next navigation destination.
   * @see #internalError(Throwable)
   */
  public static final String INTERNAL_ERROR_RESULT = "InternalError";

  /**
   * The default result for indicating that the data validation failed for a given processing action.
   * This will be used in the {@code faces-config.xml} file to determine the next navigation destination.
   */
  public static final String VALIDATION_FAILED_RESULT = "ValidationFailed";

  /**
   * The session attribute where the current full instrument is stored
   */
  private static final String CURRENT_FULL_INSTRUMENT_ATTR = "currentFullInstrument";

  /**
   * The instruments owned by the user
   */
  private List<InstrumentStub> instruments;

  /**
   * Long date format for displaying dates
   */
  private final String longDateFormat = "yyyy-MM-dd HH:mm:ss";

  /**
   * Set this to force reloading the instrument even though it is already set
   */
  private boolean forceInstrumentReload = false;

  /**
   * Set a message that can be displayed to the user on a form
   * @param componentID The component ID to which the message relates (can be null)
   * @param messageString The message string
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
   * Generates a JSF component ID for a given form input name by combining
   * it with the bean's form name.
   * @param componentName The form input name
   * @return The JSF component ID
   * @see #getFormName()
   */
  protected String getComponentID(String componentName) {
    return getFormName() + ":" + componentName;
  }

  /**
   * Register an internal error. This places the error
   * stack trace in the messages, and sends back a result
   * that will redirect the application to the internal error page.
   *
   * This should only be used for errors that can't be handled properly,
   * e.g. database failures and the like.
   *
   * @param error The error
   * @return A result string that will direct to the internal error page.
   * @see #INTERNAL_ERROR_RESULT
   */
  public String internalError(Throwable error) {
    setMessage("STACK_TRACE", StringUtils.stackTraceToString(error));
    if (null != error.getCause()) {
      setMessage("CAUSE_STACK_TRACE", StringUtils.stackTraceToString(error.getCause()));
    }
    return INTERNAL_ERROR_RESULT;
  }

  /**
   * Retrieve a parameter from the request
   * @param paramName The name of the parameter to retrieve
   * @return The parameter value
   */
  public String getRequestParameter(String paramName) {
    return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(paramName);
  }

  /**
   * Retrieves the current HTTP Session object
   * @return The HTTP Session object
   */
  public HttpSession getSession() {
    return (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
  }

  /**
   * Directly navigate to a given result
   * @param navigation The navigation result
   */
  public void directNavigate(String navigation) {
    ConfigurableNavigationHandler nav = (ConfigurableNavigationHandler) FacesContext.getCurrentInstance().getApplication().getNavigationHandler();
    nav.performNavigation(navigation);
  }

  /**
   * Evaluate and EL expression and return its value
   * @param expression The EL expression
   * @return The result of evaluating the expression
   */
  public String getELValue(String expression) {
    FacesContext context = FacesContext.getCurrentInstance();
    return context.getApplication().evaluateExpressionGet(context, expression, String.class);
  }

  /**
   * Returns the User object for the current session
   * @return The User object
   */
  public User getUser() {
    return (User) getSession().getAttribute(LoginBean.USER_SESSION_ATTR);
  }

  public UserPreferences getUserPrefs() {
    UserPreferences result = (UserPreferences) getSession().getAttribute(LoginBean.USER_PREFS_ATTR);
    if (null == result) {
      result = new UserPreferences(getUser().getDatabaseID());
    }

    return result;
  }

  /**
   * Accessing components requires the name of the form
   * that they are in as well as their own name. Most beans will only have one form,
   * so this method will provide the name of that form.
   *
   * <p>
   *   This class provides a default form name. Override the method
   *   to provide a name specific to the bean.
   * </p>
   *
   * @return The form name for the bean
   */
  protected String getFormName() {
    return "DEFAULT_FORM";
  }

  /**
   * Get the URL stub for the application
   * @return The application URL stub
   */
  public String getUrlStub() {
    // TODO This can probably be replaced with something like FacesContext.getCurrentInstance().getExternalContext().getRe‌​questContextPath()
    return ResourceManager.getInstance().getConfig().getProperty("app.urlstub");
  }

  /**
   * Get a data source
   * @return The data source
   */
  public DataSource getDataSource() {
    return ResourceManager.getInstance().getDBDataSource();
  }

  /**
   * Get the application configuration
   * @return The application configuration
   */
  protected Properties getAppConfig() {
    return ResourceManager.getInstance().getConfig();
  }

  /**
   * Initialise/reset the bean
   */
  protected void initialiseInstruments() {
    // Load the instruments list. Set the current instrument if it isn't already set.
    try {
      instruments = InstrumentDB.getInstrumentList(getDataSource(), getUser());


      boolean userInstrumentExists = false;
      long currentUserInstrument = getUserPrefs().getLastInstrument();
      if (currentUserInstrument != -1) {
        for (InstrumentStub instrument : instruments) {
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


      Instrument currentInstrument = (Instrument) getSession().getAttribute(CURRENT_FULL_INSTRUMENT_ATTR);
      if (
          // if forceInstrumentReload is set, always reload
          isForceInstrumentReload() ||

          // If the current instrument is now different to the one held in the session,
          // remove it so it will get reloaded on next access
          (null != currentInstrument &&
          currentInstrument.getDatabaseId() != currentUserInstrument)
      ) {
        getSession().removeAttribute(CURRENT_FULL_INSTRUMENT_ATTR);
        setForceInstrumentReload(false);
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
      initialiseInstruments();
    }

    return instruments;
  }

  public Instrument getCurrentInstrument() {
    // Get the current instrument from the session
    Instrument currentFullInstrument = (Instrument) getSession().getAttribute(CURRENT_FULL_INSTRUMENT_ATTR);

    try {
      // If there is nothing in the session, get the instrument from
      // the user prefs and put it in the session
      if (null == currentFullInstrument) {
        long currentInstrumentId = getUserPrefs().getLastInstrument();
        for (InstrumentStub instrumentStub : instruments) {
          if (instrumentStub.getId() == currentInstrumentId) {
            currentFullInstrument = instrumentStub.getFullInstrument();
            getSession().setAttribute(CURRENT_FULL_INSTRUMENT_ATTR, currentFullInstrument);
          }
        }

        // If we still don't have an instrument, then get the first one from the list
        if (null == currentFullInstrument) {
          if (instruments.size() == 0) {
            getUserPrefs().setLastInstrument(-1);
          } else {
            InstrumentStub stub = instruments.get(0);
            currentFullInstrument = stub.getFullInstrument();
            getSession().setAttribute(CURRENT_FULL_INSTRUMENT_ATTR, currentFullInstrument);
            getUserPrefs().setLastInstrument(stub.getId());
          }
        }
      }
    } catch (Exception e) {
      // Swallow the error, but dump it
      e.printStackTrace();
      currentFullInstrument = null;
    }

    return currentFullInstrument;
  }

  /**
   * Get the current instrument
   * @return The current instrument
   */
  public long getCurrentInstrumentId() {

    long result = -1;

    if (null != getCurrentInstrument()) {
      result = getCurrentInstrument().getDatabaseId();
    }

    return result;
  }

  /**
   * Set the current instrument
   * @param currentInstrument The current instrument
   */
  public void setCurrentInstrumentId(long currentInstrumentId) {

    // Only do something if the instrument has actually changed
    if (getUserPrefs().getLastInstrument() != currentInstrumentId) {

      // Update the user preferences
      getUserPrefs().setLastInstrument(currentInstrumentId);

      // Remove the current instrument from the session;
      // It will be replaced on next access
      getSession().removeAttribute(CURRENT_FULL_INSTRUMENT_ATTR);
    }
  }

  /**
   * @return the longDateFormat
   */
  public String getLongDateFormat() {
    return longDateFormat;
  }

  /**
   * Determine whether or not there are instruments available for this user
   * @return {@code true} if the user has any instruments; {@code false} if not.
   */
  public boolean getHasInstruments() {
    List<InstrumentStub> instruments = getInstruments();
    return instruments.size() > 0;
  }

  /**
   * @return true if instrument should always be reloaded on initialiseInstrument
   */
  public boolean isForceInstrumentReload() {
    return forceInstrumentReload;
  }

  /**
   * Set to true to make full instrument reload on initialiseInstrument
   * This is reset to false after running initialiseInstrument
   * @param forceReload
   */
  public void setForceInstrumentReload(boolean forceReload) {
    this.forceInstrumentReload = forceReload;
  }

  /**
   * Get the list of available run type categories
   * @return The run type categories
   * @throws ResourceException If the Resource Manager cannot be accessed
   */
  public List<RunTypeCategory> getRunTypeCategories() throws ResourceException {
    List<RunTypeCategory> allCategories =
      ServletUtils.getResourceManager().getRunTypeCategoryConfiguration()
        .getCategories(true, true);

    return removeUnusedVariables(allCategories);
  }

  protected List<RunTypeCategory> removeUnusedVariables(List<RunTypeCategory> categories) {
    List<Long> instrumentVariables = getInstrumentVariableIDs();
    return categories
      .stream()
      .filter(c -> c.getType() < 0 || instrumentVariables.contains(c.getType()))
      .collect(Collectors.toList());
  }

  /**
   * Determine whether or not the current user can
   * approve datasets for export
   * @return {@code true} if the user can approve datasets;
   *         {@code false} if not
   */
  public boolean isApprovalUser() {
    return getUser().isApprovalUser();
  }

  /**
   * Get the IDs of the variables assigned to the current instrument
   * @return The variable IDs
   */
  protected List<Long> getInstrumentVariableIDs() {
    return getCurrentInstrument().getVariables()
      .stream()
      .map(InstrumentVariable::getId)
      .collect(Collectors.toList());
  }
}
