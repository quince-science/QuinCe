package uk.ac.exeter.QuinCe.api;

import java.sql.Connection;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.utils.EmailSender;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Send a test email to a user.
 */
@Path("/emailTest")
public class EmailTest {

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public Response sendEmail(@FormParam("userid") long userid) throws Exception {
    Connection conn = null;
    Response response;
    String result = "Email sending triggered. Check whether it's been received";

    try {
      ResourceManager resourceManager = ResourceManager.getInstance();
      DataSource dataSource = resourceManager.getDBDataSource();

      conn = dataSource.getConnection();

      User user = UserDB.getUser(conn, userid);

      EmailSender.sendEmail(resourceManager.getConfig(), user.getEmailAddress(),
        "Test email from QuinCe",
        "This is a test email from QuinCe. If you are receiving this you are probably expecting it. If you are not expecting it, please contact your QuinCe administrator.");

      response = Response.ok(result.getBytes(), MediaType.TEXT_PLAIN).build();

    } catch (Exception e) {
      e.printStackTrace();
      response = Response.serverError().build();
    }

    return response;
  }
}
