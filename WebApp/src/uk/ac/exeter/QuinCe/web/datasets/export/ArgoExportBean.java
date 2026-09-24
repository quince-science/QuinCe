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
import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
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

    for (Coordinate coordinate : data.getCoordinates()) {
      boolean firstColumn = true;
      ArgoCoordinate castCoordinate = (ArgoCoordinate) coordinate;

      List<PlotPageColumnHeading> baseColumns = data.getColumnHeadings()
        .get(ManualQCData.ROOT_FIELD_GROUP);

      for (PlotPageColumnHeading column : baseColumns) {
        if (allowedExportColumns.contains(column)) {
          if (firstColumn) {
            firstColumn = false;
          } else {
            result.append(exportOption.getSeparator());
          }

          PlotPageTableValue value = castCoordinate
            .getPlotPageTableValue(column);

          addValueToOutput(result, exportOption, column.getId(), value,
            column.hasQC(), column.includeType(), data.getSensorValues());
        }
      }

      // End of line
      result.append('\n');
      result.addRecord();
    }

    // ++_+_+_+_+_+_+_+_+_+_+_+__+_+_+_+_+_+_+_+_

    /*
     * // Process each row of the data for (Long rowId : data.getRowIDs()) { if
     * (data.contains(rowId, exportOption.includeRawSensors())) { boolean
     * firstColumn = true;
     * 
     * // Time and position List<PlotPageColumnHeading> baseColumns =
     * data.getColumnHeadings() .get(ManualQCData.ROOT_FIELD_GROUP);
     * 
     * for (PlotPageColumnHeading column : baseColumns) { if
     * (allowedExportColumns.contains(column)) { if (firstColumn) { firstColumn
     * = false; } else { result.append(exportOption.getSeparator()); }
     * 
     * PlotPageTableValue value = data.getColumnValue(rowId, column.getId());
     * 
     * addValueToOutput(result, exportOption, column.getId(), value,
     * column.hasQC(), column.includeType(), data.getSensorValues()); } }
     * 
     * // Measurement values Measurement measurement =
     * data.getMeasurement(rowId);
     * 
     * List<PlotPageColumnHeading> measurementValueColumns = data
     * .getColumnHeadings().get(ManualQCData.MEASUREMENTVALUES_FIELD_GROUP);
     * 
     * for (PlotPageColumnHeading column : measurementValueColumns) { if
     * (allowedExportColumns.contains(column)) { SensorType sensorType =
     * ResourceManager.getInstance()
     * .getSensorsConfiguration().getSensorType(column.getId());
     * 
     * PlotPageTableValue value = null;
     * 
     * if (null != measurement && measurement.hasMeasurementValue(sensorType)) {
     * value = measurement.getMeasurementValue(sensorType); } else { value = new
     * NullPlotPageTableValue(); }
     * 
     * boolean useValueInThisColumn;
     * 
     * if (columnsWithId(measurementValueColumns, column.getId()) == 1) {
     * 
     * // There is only one column registered for this SensorType, so use // it
     * useValueInThisColumn = true; } else { // There are multiple columns for
     * this SensorType (e.g. xCO2 is // for // underway marine pCO2 and underway
     * atmospheric pCO2, so where // the // value goes is determined by the
     * measurement's Run Type if (null == measurement) { // There is no
     * measurement, so we leave the column blank useValueInThisColumn = false;
     * 
     * } else {
     * 
     * // If this column is for the Run Type of the measurement, we // populate
     * it. Otherwise we leave it blank - there'll be // another // column for
     * the Run Type somewhere (or perhaps not, if it's a // non-measurement run
     * type eg gas standard run) String runType = measurement
     * .getRunType(Measurement.RUN_TYPE_DEFINES_VARIABLE);
     * 
     * // Look through all the column headings defined for the run type // to
     * see if it contains our current column. If it does, we add // the value.
     * If not, it'll be blank. Set<ColumnHeading> runTypeColumns = instrument
     * .getAllVariableColumnHeadings(runType);
     * 
     * useValueInThisColumn = ColumnHeading
     * .containsColumnWithCode(runTypeColumns, column.getCodeName()); } }
     * 
     * result.append(exportOption.getSeparator()); addValueToOutput(result,
     * exportOption, column.getId(), useValueInThisColumn ? value : null, true,
     * true, data.getSensorValues()); } }
     * 
     * // Data Reduction for all variables for (Variable variable :
     * exportOption.getVariables()) { if
     * (instrument.getVariables().contains(variable)) {
     * List<CalculationParameter> params = DataReducerFactory
     * .getCalculationParameters(variable,
     * exportOption.includeCalculationColumns());
     * 
     * for (CalculationParameter param : params) { if
     * (allowedExportColumns.contains(param)) {
     * result.append(exportOption.getSeparator());
     * 
     * PlotPageTableValue value = data.getColumnValue(rowId, param.getId());
     * 
     * // // If the data reduction is bad, store an empty value instead. // //
     * Note that it's possible for a Measurement to be Good but the // data
     * reduction for an individual Variable to be Bad. // if (null != value &&
     * exportOption.skipBad() && dataset.getFlagScheme()
     * .isBad(value.getQcFlag(data.getSensorValues()))) { value = null; }
     * 
     * addValueToOutput(result, exportOption, param.getId(), value,
     * param.isResult(), false, data.getSensorValues()); } } } }
     * 
     * if (exportOption.includeRawSensors()) { List<PlotPageColumnHeading>
     * sensorHeadings = data.getColumnHeadings()
     * .get(ManualQCData.SENSORS_FIELD_GROUP);
     * 
     * for (PlotPageColumnHeading heading : sensorHeadings) { if
     * (allowedExportColumns.contains(heading)) {
     * result.append(exportOption.getSeparator());
     * 
     * PlotPageTableValue value = data.getColumnValue(rowId, heading.getId());
     * addValueToOutput(result, exportOption, heading.getId(), value, true,
     * false, data.getSensorValues()); } }
     * 
     * List<PlotPageColumnHeading> diagnosticHeadings = data
     * .getColumnHeadings().get(ManualQCData.DIAGNOSTICS_FIELD_GROUP);
     * 
     * if (null != diagnosticHeadings) { for (PlotPageColumnHeading heading :
     * diagnosticHeadings) { if (allowedExportColumns.contains(heading)) {
     * result.append(exportOption.getSeparator());
     * 
     * PlotPageTableValue value = data.getColumnValue(rowId, heading.getId());
     * addValueToOutput(result, exportOption, heading.getId(), value, true,
     * false, data.getSensorValues()); } } } }
     * 
     * result.append('\n'); result.addRecord(); } }
     */
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

    // Measurement Sensor Types - these are the calculated sensor values
    // used as input for the data reducers
    for (PlotPageColumnHeading measurementValueHeading : data
      .getColumnHeadings().get(ManualQCData.MEASUREMENTVALUES_FIELD_GROUP)) {

      addHeader(headers, exportOption, measurementValueHeading, allowedColumns);
    }

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
