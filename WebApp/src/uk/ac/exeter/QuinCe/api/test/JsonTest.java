package uk.ac.exeter.QuinCe.api.test;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/test/JsonTest")
public class JsonTest {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<JsonBean> getJson() {

    List<JsonBean> result = new ArrayList<JsonBean>();
    result.add(new JsonBean(43542, "I am a bean"));
    result.add(new JsonBean(34, "I am a bean"));
    result.add(new JsonBean(98, "I am also a bean"));

    return result;
  }

}
