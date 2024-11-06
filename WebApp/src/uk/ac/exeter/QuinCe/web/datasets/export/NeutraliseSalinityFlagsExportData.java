package uk.ac.exeter.QuinCe.web.datasets.export;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

/**
 * A special version of the {@link ExportData} class that removes the influence
 * of salinity QC flags from the final fCO2 data.
 *
 * <p>
 * For Nuka Arctica, the salinity data is delayed for up to a year before it's
 * available for processing in QuinCe. (At the time of writing, we are even
 * unsure how we would ingest the delayed salinity data.) To get round this, all
 * Nuka Arctica data is pre-processed before being loaded into QuinCe, and has a
 * fake salinity column added with the value taken from the World Ocean Atlas
 * climatology. This causes some salinity values to be flagged as
 * {@link Flag#BAD}, which in turn cascades to the final fCO₂ value, marking
 * them all {@link Flag#QUESTIONABLE}. In this special case, the PI has
 * established that the influence of the fixed salinity value is not sufficient
 * to warrant the flag placed on fCO₂, and therefore it should be removed.
 * </p>
 *
 * <p>
 * This version of the {@link ExportData} class contains a post-processor that
 * removes the influence of the salinity flag from the final fCO₂ values, while
 * ensuring that all other flags are still honoured. It also fixes the Salinity
 * flag as Questionable for all records, since the climatological value is
 * probably reasonable but shouldn't be relied on.
 * </p>
 */
public class NeutraliseSalinityFlagsExportData extends ExportData {

  private static final String[] CASCADE_SENSOR_TYPES = { "Water Temperature",
    "Equilibrator Temperature", "Equilibrator Pressure (absolute)",
    "Equilibrator Pressure (differential)", "Pressure at instrument",
    "xCO₂ (with standards)" };

  public NeutraliseSalinityFlagsExportData(DataSource dataSource,
    Instrument instrument, DataSet dataset, ExportOption exportOption)
    throws SQLException {
    super(dataSource, instrument, dataset, exportOption);
  }

  @Override
  public void postProcess() throws Exception {

    List<Long> salinityColumns = instrument.getSensorAssignments()
      .getColumnIds("Salinity");

    List<Long> cascadeColumns = getCascadeColumns();

    Variable variable = instrument.getVariable("Underway Marine pCO₂");

    if (null != variable) {

      for (Long rowId : getRowIDs()) {
        DataReductionRecord dataReductionRecord = getDataReductionRecord(rowId,
          variable);

        if (null != dataReductionRecord) {

          // See if the fCO₂ flag is Questionable. If so, it's possible that
          // it was caused by bad salinity
          if (dataReductionRecord.getQCFlag().equals(Flag.QUESTIONABLE)) {

            // See if the salinity is marked Bad. If it is, then we must
            // recalculate the fCO₂ flag

            Flag salinityFlag = Flag.GOOD;

            // There might be multiple salinity columns
            for (Long salinityColumn : salinityColumns) {
              PlotPageTableValue salinity = getColumnValue(rowId,
                salinityColumn);
              if (null != salinity && salinity.getQcFlag(getAllSensorValues())
                .moreSignificantThan(salinityFlag)) {
                salinityFlag = salinity.getQcFlag(getAllSensorValues());
              }
            }

            if (salinityFlag.equals(Flag.BAD)) {

              // We're not using the built-in cascading here, because it's too
              // hard to extract the logic and even harder to extract the
              // logic without salinity. So we're just going to do it
              // manually.

              // The new QC info
              Flag newFlag = Flag.GOOD;
              List<String> qcComments = new ArrayList<String>();

              // Loop through all the incoming sensor fields
              for (Long columnId : cascadeColumns) {
                PlotPageTableValue cascadeValue = getColumnValue(rowId,
                  columnId);

                // If the flag is Bad or Questionable, record the QC comment
                // and upgrade the flag if needed
                Flag cascadeFlag = cascadeValue.getQcFlag(getAllSensorValues());
                if (cascadeFlag.equals(Flag.BAD)
                  || cascadeFlag.equals(Flag.QUESTIONABLE)) {

                  qcComments
                    .add(cascadeValue.getQcMessage(getAllSensorValues(), true));
                  if (cascadeFlag.moreSignificantThan(newFlag)) {
                    newFlag = cascadeFlag;
                  }
                }
              }

              // Update the record's QC
              dataReductionRecord.setQc(newFlag, qcComments);
            }
          }
        }

        // Now set the salinity flags (note we do this for every row
        for (Long salinityColumn : salinityColumns) {
          overrideQc(rowId, salinityColumn, Flag.QUESTIONABLE,
            "Climatological value from World Ocean Atlas");
        }
      }
    }
  }

  private List<Long> getCascadeColumns() throws SensorTypeNotFoundException {

    List<Long> result = new ArrayList<Long>();

    for (String sensorType : CASCADE_SENSOR_TYPES) {
      result.addAll(instrument.getSensorAssignments().getColumnIds(sensorType));
    }

    return result;
  }
}
