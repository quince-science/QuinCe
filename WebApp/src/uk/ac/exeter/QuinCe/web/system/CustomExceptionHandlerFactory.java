package uk.ac.exeter.QuinCe.web.system;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

/**
 * Factory class for custom exception handlers
 * 
 * @author Steve Jones
 *
 */
public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {

  /**
   * The parent {@code ExceptionHandlerFactory} provided by the system
   */
  private ExceptionHandlerFactory parent;

  /**
   * Constructor - grabs a link to the parent {@code ExceptionHandlerFactory}
   * 
   * @param parent
   *          The parent factory
   */
  public CustomExceptionHandlerFactory(ExceptionHandlerFactory parent) {
    this.parent = parent;
  }

  @Override
  public ExceptionHandler getExceptionHandler() {
    return new BaseExceptionHandler(parent.getExceptionHandler());
  }
}
