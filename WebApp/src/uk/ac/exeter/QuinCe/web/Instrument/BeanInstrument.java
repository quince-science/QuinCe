package uk.ac.exeter.QuinCe.web.Instrument;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * Wrapper around the {@link Instrument} class that adds some extra features for
 * use in a web context.
 */
public class BeanInstrument extends Instrument {

  /**
   * Indicates whether or not the current user can share this instrument.
   */
  private boolean canShare;

  /**
   * Simple constructor.
   *
   * @param instrument
   *          The {@link Instrument} whose sharing options we are interested in.
   * @param currentUser
   *          The currently logged in {@link User}.
   */
  protected BeanInstrument(Instrument instrument, User currentUser) {
    super(instrument);
    canShare = currentUser.isAdminUser()
      || currentUser.equals(instrument.getOwner());
  }

  /**
   * Determine whether or not this instrument can be shared.
   *
   * @return {@code true} if the instrument can be shared. {@code false}
   *         otherwise.
   */
  public boolean getCanShare() {
    return canShare;
  }
}
