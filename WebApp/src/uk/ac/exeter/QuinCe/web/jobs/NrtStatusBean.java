package uk.ac.exeter.QuinCe.web.jobs;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.NrtStatus;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

@ManagedBean
@ViewScoped
public class NrtStatusBean extends BaseManagedBean {

  private List<NrtStatus> status = null;

  public List<NrtStatus> getNrtStatus() {
    return status;
  }

  public void update() {
    try {
      status = DataSetDB.getNrtStatus(getDataSource());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
