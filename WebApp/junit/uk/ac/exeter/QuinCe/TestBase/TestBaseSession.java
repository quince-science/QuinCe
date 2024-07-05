package uk.ac.exeter.QuinCe.TestBase;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Implementation of {@link HttpSession} for JUnit tests.
 */
public class TestBaseSession implements HttpSession {

  private LocalDateTime creationTime = LocalDateTime.now();

  private HashMap<String, Object> attributes = new HashMap<String, Object>();

  private boolean valid = true;

  @Override
  public long getCreationTime() {
    return DateTimeUtils.dateToLong(creationTime);
  }

  @Override
  public String getId() {
    return "Test HttpSession";
  }

  @Override
  public long getLastAccessedTime() {
    return DateTimeUtils.dateToLong(LocalDateTime.now());
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }

  @Override
  public void setMaxInactiveInterval(int interval) {
    // NOOP
  }

  @Override
  public int getMaxInactiveInterval() {
    return Integer.MAX_VALUE;
  }

  @Override
  public HttpSessionContext getSessionContext() {
    return null;
  }

  @Override
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Object getValue(String name) {
    return getAttribute(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  @Override
  public String[] getValueNames() {
    return (String[]) attributes.keySet().toArray();
  }

  @Override
  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  @Override
  public void putValue(String name, Object value) {
    setAttribute(name, value);
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public void removeValue(String name) {
    attributes.remove(name);
  }

  @Override
  public void invalidate() {
    if (!valid) {
      throw new IllegalStateException("Session already invalidated");
    }
    valid = false;
  }

  @Override
  public boolean isNew() {
    return false;
  }
}
