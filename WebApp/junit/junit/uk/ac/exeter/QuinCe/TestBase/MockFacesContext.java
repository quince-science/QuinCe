package junit.uk.ac.exeter.QuinCe.TestBase;

import javax.faces.context.FacesContext;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * A mocked {@link FacesContext}.
 *
 * <p>
 * Taken from <a href=
 * "http://illegalargumentexception.blogspot.com/2011/12/jsf-mocking-facescontext-for-unit-tests.html">
 * http://illegalargumentexception.blogspot.com/2011/12/jsf-mocking-facescontext-for-unit-tests.html
 * </a>
 * </p>
 *
 * @author Steve Jones
 *
 */
public abstract class MockFacesContext extends FacesContext {

  private static final Release RELEASE = new Release();

  private MockFacesContext() {

  }

  public static FacesContext mockFacesContext() {
    FacesContext context = Mockito.mock(FacesContext.class);
    setCurrentInstance(context);
    Mockito.doAnswer(RELEASE).when(context).release();
    return context;
  }

  private static class Release implements Answer<Void> {
    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      setCurrentInstance(null);
      return null;
    }

  }
}
