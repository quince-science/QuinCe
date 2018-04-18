package uk.ac.exeter.QuinCe.EquilibratorPco2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Calculation record for equilibrator-based pCO2 systems
 * @author Steve Jones
 *
 */
public class EquilibratorPco2CalculationRecord extends CalculationRecord {

  /**
   * The list of columns that contain calculation values
   */
  protected static List<String> calculationColumns = null;

  static {
    // TODO These should be retrieved from the configuration somehow
    calculationColumns = new ArrayList<String>();
    calculationColumns.add("Delta T");
    calculationColumns.add("True Moisture");
    calculationColumns.add("pH2O");
    calculationColumns.add("Dried CO2");
    calculationColumns.add("Calibrated CO2");
    calculationColumns.add("pCO2 TE Wet");
    calculationColumns.add("pCO2 SST");
    calculationColumns.add("fCO2");
  }

  /**
   * Creates an empty calculation record for the given dataset and measurement
   * @param datasetId The dataset ID
   * @param measurementId The measurement ID
   * @param resourceManager A Resource Manager containing the QC Column Config for Equilibrator pCO2
   */
  public EquilibratorPco2CalculationRecord(long datasetId, long measurementId, ResourceManager resourceManager) {
    super(datasetId, measurementId, resourceManager.getColumnConfig());
  }

  @Override
  protected CalculationDB getCalculationDB() {
    return CalculationDBFactory.getCalculationDB();
  }

  @Override
  public List<String> getCalculationColumns() {
    return calculationColumns;
  }

  @Override
  protected void buildColumnAliases() {
    columnAliases = new HashMap<String, String>();
    columnAliases.put("Delta T", "Delta Temperature");
    columnAliases.put("True xH2O", "True Moisture");
    columnAliases.put("pH2O", "pH2O");
    columnAliases.put("Dried CO2", "Dried CO2");
    columnAliases.put("Calibrated CO2", "Calibrated CO2");
    columnAliases.put("pCO2 TE Dry", "pCO2 TE Dry");
    columnAliases.put("pCO2 TE Wet", "pCO2 TE Wet");
    columnAliases.put("fCO2 TE", "fCO2 TE");
    columnAliases.put("fCO2", "fCO2");
  }
}
