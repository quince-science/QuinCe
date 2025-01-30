package uk.ac.exeter.QuinCe.web;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Bean providing utility methods for the Credits page
 */
@ManagedBean
@SessionScoped
public class CreditsBean extends BaseManagedBean {

  public List<String> getInstanceCredits() {
    return StringUtils.delimitedToList(
      ResourceManager.getInstance().getConfig().getProperty("instance_credits"),
      ";");
  }

  public String getInstanceHost() {
    return ResourceManager.getInstance().getConfig()
      .getProperty("instance_host");
  }

}
