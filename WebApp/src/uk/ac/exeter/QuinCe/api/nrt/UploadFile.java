package uk.ac.exeter.QuinCe.api.nrt;

import java.io.InputStream;

import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.files.UploadedDataFile;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API call for uploading new files for an instrument
 * 
 * @author Steve Jones
 *
 */
@Path("/nrt/UploadFile")
public class UploadFile {

  /**
   * Main API method
   * 
   * @return The upload response
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  public Response uploadFile(@FormDataParam("instrument") long instrumentId,
    @FormDataParam("file") InputStream is,
    @FormDataParam("file") FormDataContentDisposition fileDetail) {

    int result = Status.OK.getStatusCode();
    String resultBody = null;

    try {
      ResourceManager resourceManager = ResourceManager.getInstance();
      DataSource dataSource = resourceManager.getDBDataSource();
      Instrument instrument = InstrumentDB.getInstrument(dataSource,
        instrumentId, resourceManager.getSensorsConfiguration(),
        resourceManager.getRunTypeCategoryConfiguration());

      // We don't allow uploads for non-NRT instruments
      if (!instrument.getNrt()) {
        result = Status.FORBIDDEN.getStatusCode();
        resultBody = "Not an NRT instrument";
      } else {
        UploadedDataFile upload = new APIUploadedDataFile(
          fileDetail.getFileName(), is);

        // Extract and check the file
        upload.extractFile(instrument, resourceManager.getConfig(), true, true);

        // See if extraction was successful
        if (!upload.isStore()) {
          result = upload.getStatusCode();
          resultBody = upload.getMessages();
        } else {
          DataFileDB.storeFile(dataSource, resourceManager.getConfig(),
            upload.getDataFile(), upload.getReplacementFile());
        }
      }
    } catch (Exception e) {
      result = Status.INTERNAL_SERVER_ERROR.getStatusCode();
      resultBody = e.getMessage();
    }

    ResponseBuilder responseBuilder = Response.status(result);
    if (null != resultBody) {
      responseBuilder = responseBuilder.entity(resultBody);
    }

    return responseBuilder.build();
  }
}
