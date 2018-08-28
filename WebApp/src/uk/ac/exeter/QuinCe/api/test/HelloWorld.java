package uk.ac.exeter.QuinCe.api.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/test/HelloWorld")
public class HelloWorld {

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String get() {
    return "Hello World!";
  }

}
