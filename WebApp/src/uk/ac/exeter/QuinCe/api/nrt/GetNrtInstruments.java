package uk.ac.exeter.QuinCe.api.nrt;

import java.util.List;

import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@Path("/nrt/GetInstruments")
public class GetNrtInstruments {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<NrtInstrument> getNrtInstruments() throws Exception {

    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
      return InstrumentDB.getNrtInstruments(dataSource);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

}
