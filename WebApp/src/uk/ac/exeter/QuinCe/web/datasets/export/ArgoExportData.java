package uk.ac.exeter.QuinCe.web.datasets.export;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.ArgoCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.DataReductionRecordPlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageDataException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SensorValuePlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ArgoManualQCData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQCData;

/**
 * Argo-specific implementation of {@link ExportData}.
 *
 * <p>
 * This class extends {@link ExportData}, but wraps its own local instance of
 * {@link ArgoManualQCData} so it behaves the same way that {@link ExportData}
 * accesses {@link ManualQCData}.
 * </p>
 */
public class ArgoExportData extends ExportData {

  private ArgoManualQCData localSourceData;

  public ArgoExportData(ManualQCData sourceData, ExportOption exportOption)
    throws Exception {
    super(sourceData, exportOption);
    this.localSourceData = (ArgoManualQCData) sourceData;
    localInit();
  }

  /**
   * Additional initialisation steps.
   */
  protected void localInit() {
    // Add the profile headers to the root column group
    List<PlotPageColumnHeading> rootColumnGroup = headingsWithProperties
      .get(ManualQCData.ROOT_FIELD_GROUP);

    rootColumnGroup.addAll(0, localSourceData.getProfileDataHeadings());
  }

  public List<ArgoCoordinate> getCoordinates() {
    return localSourceData.getCoordinates().stream()
      .map(c -> (ArgoCoordinate) c).toList();
  }

  public Measurement getMeasurement(Coordinate coordinate) {
    return localSourceData.getMeasurement(coordinate);
  }

  public PlotPageTableValue getColumnValue(ArgoCoordinate coordinate,
    long columnId) throws PlotPageDataException {

    PlotPageTableValue result = null;

    try {
      List<Long> sensorColumnIds = localSourceData.getInstrument()
        .getSensorAssignments().getSensorColumnIds();

      List<Long> diagnosticColumnIds = localSourceData.getInstrument()
        .getSensorAssignments().getDiagnosticColumnIds();

      if (sensorColumnIds.contains(columnId)
        || diagnosticColumnIds.contains(columnId)) {
        // SensorValue
        SensorValue sensorValue = sensorValues.getRawSensorValue(columnId,
          coordinate);
        if (null != sensorValue) {
          result = new SensorValuePlotPageTableValue(sensorValue);
        }
      } else {
        // Data Reduction value
        Variable variable = DataReducerFactory
          .getVariable(localSourceData.getInstrument(), columnId);
        CalculationParameter parameter = DataReducerFactory
          .getVariableParameter(variable, columnId);

        Measurement measurement = measurements.get(coordinate);
        if (null != measurement) {
          if (localSourceData.getDataReduction()
            .containsKey(measurement.getId())) {
            DataReductionRecord record = localSourceData.getDataReduction()
              .get(measurement.getId()).get(variable);
            if (null != record) {
              result = new DataReductionRecordPlotPageTableValue(record,
                parameter.getShortName());
            }
          }
        }
      }

    } catch (Exception e) {
      throw new PlotPageDataException("Error getting column value", e);
    }

    return result;
  }
}
