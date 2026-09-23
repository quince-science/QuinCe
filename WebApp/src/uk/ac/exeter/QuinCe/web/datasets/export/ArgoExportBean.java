package uk.ac.exeter.QuinCe.web.datasets.export;

import java.io.OutputStream;
import java.sql.Connection;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;

/**
 * Version of the Export Bean for Argo exports.
 *
 * <p>
 * There are no export options, only a direct export to CSV in a fixed format.
 * </p>
 */
@ManagedBean
@RequestScoped
public class ArgoExportBean extends ExportBean {

  public void export() {
    try (Connection conn = getDataSource().getConnection();) {

      DatasetExport export = getDatasetExport(getCurrentInstrument(), dataset,
        new ArgoExportOption(dataset), getProgress());

      FacesContext fc = FacesContext.getCurrentInstance();
      ExternalContext ec = fc.getExternalContext();

      ec.responseReset();
      ec.setResponseContentType("text/csv");

      // File size
      ec.setResponseContentLength(export.getContent().length);

      // Filename
      ec.setResponseHeader("Content-Disposition", "attachment; filename=\""
        + dataset.getInstrument().getName() + ".csv\"");

      OutputStream outputStream = ec.getResponseOutputStream();
      outputStream.write(export.getContent());

      fc.responseComplete();

      dataset.markExported();

      // Note that manual exports will not trigger the NRT Export recording,
      // otherwise the automatic NRT export would be prevented from doing its
      // job.
      DataSetDB.setDatasetExported(conn, dataset.getId(), false);
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }
}
