package uk.ac.exeter.QuinCe.web.User;

import javax.servlet.annotation.WebFilter;

import uk.ac.exeter.QuinCe.User.User;

/**
 * Filter to determine whether a user has permission to view and manage jobs
 * 
 * @author Steve Jones
 *
 */
@WebFilter("/jobs/*")
public class JobsPermissionFilter extends PermissionsFilter {

  @Override
  public boolean hasPermission(User user) {
    return user.getJobManager();
  }
}
