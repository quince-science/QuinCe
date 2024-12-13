package uk.ac.exeter.QuinCe.web.Instrument;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * Wrapper around the {@link Instrument} class that adds some extra features for
 * use in a web context.
 */
public class BeanInstrument extends Instrument {

  private boolean canShare;

  protected BeanInstrument(Instrument instrument, User currentUser) {
    super(instrument);
    canShare = currentUser.isAdminUser()
      || currentUser.equals(instrument.getOwner());
  }

  public boolean getCanShare() {
    return canShare;
  }
}
