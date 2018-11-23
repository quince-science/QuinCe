package uk.ac.exeter.QuinCe.api.nrt;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * API call for uploading new files for an instrument
 * @author Steve Jones
 *
 */
@Path("/nrt/UploadFile")
public class UploadFile {

  /**
   * Main API method
   * @return The upload response
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadFile(@FormDataParam("instrument") long instrumentId,
      @FormDataParam("file") InputStream is,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {

    System.out.print(fileDetail.getFileName());
    return Response.status(Status.OK).build();
  }
}
