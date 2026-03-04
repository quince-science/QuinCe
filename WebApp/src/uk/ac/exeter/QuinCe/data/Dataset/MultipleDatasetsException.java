package uk.ac.exeter.QuinCe.data.Dataset;

@SuppressWarnings("serial")
/**
 * Exception for multiple datasets found in a dataset query
 */
public class MultipleDatasetsException extends DataSetException {

  /**
   * Constructor
   *
   * @param datasetName
   *          The datasetName corresponding to several database entries
   */
  public MultipleDatasetsException(String datasetName) {
    super("Multiple data sets found corresponding to the dataset name: " + datasetName);
  }

}
