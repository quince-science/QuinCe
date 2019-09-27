package uk.ac.exeter.QuinCe.api.export;

import java.sql.Connection;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.export.ExportBean;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API call to export a complete dataset as a ZIP file.
 *
 * <p>
 * The ZIP will contain:
 * </p>
 * <ul>
 * <li>The dataset in all available export formats</li>
 * <li>The raw data files used to build the datases</li>
 * <li>A manifest containing file lists and metadata, in JSON format</li>
 * </ul>
 *
 * <p>
 * The ZIP file will be named {@code <Dataset Name>.zip}. If the dataset was
 * built from the raw files {@code origin1.csv} and {@code origin2.csv}, and
 * there are two export formats available named {@code Tabbed} and {@code CSV},
 * the ZIP file's structure will be as follows:
 * </p>
 *
 * <pre>
 * BSBS20150807.zip
 * |- manifest.json
 * |- raw
 * |  |- origin1.csv
 * |  |- origin2.csv
 * |
 * |- dataset
 *    |- Tabbed
 *    |  |- BSBS20150807.tsv
 *    |
 *    |- CSV
 *       |- BSBS20150807.csv
 * </pre>
 *
 * <p>
 * The {@code manifest.json} file will contain the following information:
 * <p>
 *
 * <pre>
 * {
 *   "manifest": {
 *     "metadata": {
 *       "name": "BSBS20150807",
 *       "platformCode": "BSBS",
 *       "nrt": false,
 *       "records": 52688,
 *       "startdate": "2015-08-07T18:20:34.000Z",
 *       "enddate": "2015-08-28T00:20:36.000Z",
 *       "bounds": {
 *         "east": 0.092179,
 *         "south": 14.21337,
 *         "north": 50.486686,
 *         "west": -71.59652
 *       },
 *       "last_touched": "2019-09-25T16:16:44.715Z",
 *       "quince_information": "Data processed using QuinCe version v2.0.6.1",
 *     },
 *     "raw": [
 *       {
 *         "filename": "origin1.csv",
 *         "startDate": "2015-08-04T04:14:65.000Z"
 *         "endDate": "2015-08-18T12:43:45.000Z",
 *       },
 *       {
 *         "filename": "origin2.csv",
 *         "startDate": "2015-08-18T12:44:00.000Z"
 *         "endDate": "2015-08-28T06:53:12.000Z",
 *       }
 *     ],
 *     "dataset": [
 *       {
 *         "destination": "Tabbed",
 *         "filename": "BSBS20150807.tsv"
 *       },
 *       {
 *         "destination": "CSV",
 *         "filename": "BSBS20150807.csv"
 *       }
 *     ]
 *   }
 * }
 * </pre>
 *
 * @author Steve Jones
 *
 */
@Path("/export/exportDataset")
public class ExportDataset {

  /**
   * The main processing method for the API call.
   *
   * @return The export ZIP file.
   * @throws Exception
   *           If any errors occur while retrieving the dataset details. Results
   *           in a
   *           {@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR}
   *           being sent back to the client.
   */
  @POST
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getDatasetZip(@FormParam("id") long id) throws Exception {

    Connection conn = null;
    Response response;
    Status responseCode = Status.OK;
    byte[] zip = null;

    try {
      ResourceManager resourceManager = ResourceManager.getInstance();
      DataSource dataSource = resourceManager.getDBDataSource();
      SensorsConfiguration sensorConfig = resourceManager
        .getSensorsConfiguration();
      RunTypeCategoryConfiguration runTypeConfig = resourceManager
        .getRunTypeCategoryConfiguration();

      conn = dataSource.getConnection();
      DataSet dataset = DataSetDB.getDataSet(conn, id);
      Instrument instrument = InstrumentDB.getInstrument(conn,
        dataset.getInstrumentId(), sensorConfig, runTypeConfig);
      if (dataset.getStatus() != DataSet.STATUS_READY_FOR_EXPORT) {
        responseCode = Status.FORBIDDEN;
      } else {
        zip = ExportBean.buildExportZip(conn, instrument, dataset, null);
        DataSetDB.setDatasetStatus(conn, id, DataSet.STATUS_EXPORTING);
      }
    } catch (RecordNotFoundException e) {
      responseCode = Status.NOT_FOUND;
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    if (!responseCode.equals(Status.OK)) {
      response = Response.status(responseCode).build();
    } else {
      response = Response.ok(zip, MediaType.APPLICATION_OCTET_STREAM_TYPE)
        .build();
    }

    return response;
  }
}
