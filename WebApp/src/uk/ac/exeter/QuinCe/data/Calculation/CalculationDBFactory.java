package uk.ac.exeter.QuinCe.data.Calculation;

import uk.ac.exeter.QuinCe.EquilibratorPco2.EquilibratorPco2DB;

/**
 * Factory class for CalculationDB instances
 * @author Steve Jones
 *
 */
public class CalculationDBFactory {

  // TODO This definitely needs to use reflection.

  /**
   * The single instance of the only calculation DB we know about so far
   */
  private static CalculationDB calculationDB = null;

  /**
   * Get a CalculationDB instance
   * @return The instance
   */
  public static CalculationDB getCalculationDB() {
    if (null == calculationDB)  {
      calculationDB = new EquilibratorPco2DB();
    }

    return calculationDB;
  }

  /**
   * Get a CalculationDB instance matching the specified identifier
   * @param identifier The identifier
   * @return The instance
   * @throws CalculatorException If the identifier is not recognised
   */
  public static CalculationDB getCalculationDB(String identifier) throws CalculatorException {
    if (identifier.equalsIgnoreCase("equilibrator_pco2")) {
      return new EquilibratorPco2DB();
    } else {
      throw new CalculatorException("Unknown calculation path '" + identifier + "'");
    }
  }

}
