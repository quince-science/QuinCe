package uk.ac.exeter.QuinCe.web.system;

import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Basic exception handler. Catches all exceptions
 * and dumps them to the console, so at least we
 * see them. In the long run an error can be added
 * to the JSF Messages framework.
 *
 * Based on {@link https://www.networkworld.com/article/2224081/opensource-subnet/how-to-add-exception-handling-to-jsf-applications.html}
 * @author Steve Jones
 *
 */
public class BaseExceptionHandler extends ExceptionHandlerWrapper {

  /**
   * The wrapped {@code ExceptionHandler}
   */
  private ExceptionHandler wrapped;

  /**
   * Constructor. Does whatever it needs to do.
   * @param wrapped The wrapped {code ExceptionHandler}
   */
  public BaseExceptionHandler(ExceptionHandler wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public ExceptionHandler getWrapped() {
    return wrapped;
  }

  @Override
  public void handle() throws FacesException {
    Iterator<ExceptionQueuedEvent> iterator = getUnhandledExceptionQueuedEvents().iterator();

    while (iterator.hasNext()) {
      ExceptionQueuedEvent event = iterator.next();
      ExceptionQueuedEventContext context = (ExceptionQueuedEventContext)event.getSource();

      Throwable throwable = context.getException();

      System.out.println(ExceptionUtils.getStackTrace(throwable));
    }
  }

 /*
  *
  * This is the original method from the tutorial page, which we can use
  * to expand functionality in the future. Pasted here in case
  * the page disappears.
  *
  @Override
  public void handle() throws FacesException {
    Iterator iterator = getUnhandledExceptionQueuedEvents().iterator();

    while (iterator.hasNext()) {
      ExceptionQueuedEvent event = (ExceptionQueuedEvent) iterator.next();
      ExceptionQueuedEventContext context = (ExceptionQueuedEventContext)event.getSource();

      Throwable throwable = context.getException();

      FacesContext fc = FacesContext.getCurrentInstance();

      try {
          Flash flash = fc.getExternalContext().getFlash();

          // Put the exception in the flash scope to be displayed in the error
          // page if necessary ...
          flash.put("errorDetails", throwable.getMessage());

          System.out.println("the error is put in the flash: " + throwable.getMessage());

          NavigationHandler navigationHandler = fc.getApplication().getNavigationHandler();

          navigationHandler.handleNavigation(fc, null, "error?faces-redirect=true");

          fc.renderResponse();
      } finally {
          iterator.remove();
      }
    }

    // Let the parent handle the rest
    getWrapped().handle();

  }
   */

}
