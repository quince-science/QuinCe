package uk.ac.exeter.QuinCe.web.PlotPage.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.primefaces.json.JSONArray;

import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;

public class MapRecords extends ArrayList<MapRecord> {

  public MapRecords(int size) {
    super(size);
  }

  public String getDisplayJson(GeoBounds bounds) {
    List<MapRecord> boundsFiltered =
      stream().
      filter(p -> bounds.inBounds(p.position)).
      collect(Collectors.toList());

    List<MapRecord> decimated;

    if (boundsFiltered.size() <= 1000) {
      decimated = boundsFiltered;
    } else {
      decimated = new ArrayList<MapRecord>(1000);

      int nth = (int) Math.floor(boundsFiltered.size() / 1000);
      for (int i = 0; i < boundsFiltered.size(); i+=nth) {
        decimated.add(boundsFiltered.get(i));
      }
    }

    JSONArray json = new JSONArray();
    for (MapRecord record : decimated) {
      json.put(record.getJsonArray());
    }

    return json.toString();
  }
}
