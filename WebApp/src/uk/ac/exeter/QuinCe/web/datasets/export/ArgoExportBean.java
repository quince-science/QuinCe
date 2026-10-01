package uk.ac.exeter.QuinCe.web.datasets.export;

import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.ArgoCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.Progress;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ArgoManualQCData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQCData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQCDataFactory;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

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

      DatasetExport export = getArgoDatasetExport(getCurrentInstrument(),
        dataset, new ArgoExportOption(dataset), getProgress());

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

  /**
   * Argo-specific implementation of
   * {@link ExportBean#getDatasetExport(Instrument, DataSet, ExportOption, Progress)}.
   *
   * @param instrument
   * @param dataset
   * @param exportOption
   * @param progress
   * @return
   * @throws Exception
   */
  protected static DatasetExport getArgoDatasetExport(Instrument instrument,
    DataSet dataset, ArgoExportOption exportOption, Progress progress)
    throws Exception {

    DataSource dataSource = ResourceManager.getInstance().getDBDataSource();

    ArgoManualQCData sourceData = (ArgoManualQCData) ManualQCDataFactory
      .getManualQCData(dataSource, instrument, dataset);
    sourceData.loadData(progress);

    ArgoExportData data = (ArgoExportData) exportOption
      .makeExportData(sourceData);

    // Run the post-processor before generating the final output
    data.postProcess();

    // Initialise the output
    DatasetExport result = new DatasetExport();

    List<ColumnHeading> allowedExportColumns = getAllowedExportColumns(data,
      exportOption);

    List<String> headers = makeArgoHeaders(instrument, data, exportOption,
      allowedExportColumns, result);
    result.append(
      StringUtils.collectionToDelimited(headers, exportOption.getSeparator()));
    result.append('\n');

    for (ArgoCoordinate coordinate : data.getCoordinates()) {
      boolean firstColumn = true;

      List<PlotPageColumnHeading> baseColumns = data.getColumnHeadings()
        .get(ManualQCData.ROOT_FIELD_GROUP);

      for (PlotPageColumnHeading column : baseColumns) {
        if (allowedExportColumns.contains(column)) {
          if (firstColumn) {
            firstColumn = false;
          } else {
            result.append(exportOption.getSeparator());
          }

          PlotPageTableValue value = coordinate.getPlotPageTableValue(column);

          addValueToOutput(result, exportOption, column.getId(), value,
            column.hasQC(), column.includeType(), data.getSensorValues());
        }
      }

      // Data Reduction for all variables
      for (Variable variable : exportOption.getVariables()) {
        if (instrument.getVariables().contains(variable)) {
          List<CalculationParameter> params = DataReducerFactory
            .getCalculationParameters(variable,
              exportOption.includeCalculationColumns());

          for (CalculationParameter param : params) {
            if (allowedExportColumns.contains(param)) {
              result.append(exportOption.getSeparator());

              PlotPageTableValue value = data.getColumnValue(coordinate,
                param.getId());

              addValueToOutput(result, exportOption, param.getId(), value,
                param.isResult(), false, data.getSensorValues());
            }
          }
        }
      }

      if (exportOption.includeRawSensors()) {
        List<PlotPageColumnHeading> sensorHeadings = data.getColumnHeadings()
          .get(ManualQCData.SENSORS_FIELD_GROUP);

        for (PlotPageColumnHeading heading : sensorHeadings) {
          if (allowedExportColumns.contains(heading)) {
            result.append(exportOption.getSeparator());

            PlotPageTableValue value = data.getColumnValue(coordinate,
              heading.getId());
            addValueToOutput(result, exportOption, heading.getId(), value, true,
              false, data.getSensorValues());
          }
        }
      }

      // End of line
      result.append('\n');
      result.addRecord();
    }

    // Destroy the ExportData object so it cleans up its resources
    data.destroy();

    return result;
  }

  private static List<String> makeArgoHeaders(Instrument instrument,
    ExportData data, ExportOption exportOption,
    List<ColumnHeading> allowedColumns, DatasetExport export) throws Exception {

    List<String> headers = new ArrayList<String>();

    // Time and position
    for (PlotPageColumnHeading heading : data.getColumnHeadings()
      .get(ManualQCData.ROOT_FIELD_GROUP)) {
      addHeader(headers, exportOption, heading, allowedColumns);
    }

    // We don't export MeasurementValues

    // Headers for each variable
    for (Variable variable : exportOption.getVariables()) {
      if (instrument.getVariables().contains(variable)) {

        List<CalculationParameter> params = DataReducerFactory
          .getCalculationParameters(variable,
            exportOption.includeCalculationColumns());

        for (CalculationParameter param : params) {
          addHeader(headers, exportOption, param, allowedColumns);
        }
      }
    }

    // Raw sensors, if required. These always use the Short name, which
    // is the sensor name defined for the instrument. Includes diagnostics.
    if (exportOption.includeRawSensors()) {
      List<PlotPageColumnHeading> sensorHeadings = data.getColumnHeadings()
        .get(ManualQCData.SENSORS_FIELD_GROUP);

      for (PlotPageColumnHeading heading : sensorHeadings) {
        addHeader(headers, exportOption, heading,
          ExportOption.HEADER_MODE_SHORT, allowedColumns);
      }

      List<PlotPageColumnHeading> diagnosticHeadings = data.getColumnHeadings()
        .get(ManualQCData.DIAGNOSTICS_FIELD_GROUP);

      if (null != diagnosticHeadings) {
        for (PlotPageColumnHeading heading : diagnosticHeadings) {
          addHeader(headers, exportOption, heading,
            ExportOption.HEADER_MODE_SHORT, allowedColumns);
        }
      }
    }

    return headers;
  }
}
