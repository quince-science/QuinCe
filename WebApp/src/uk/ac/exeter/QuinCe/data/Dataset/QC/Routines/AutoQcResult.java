package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.util.ArrayList;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class AutoQcResult extends ArrayList<RoutineFlag> {

  /**
   * Return the overall flag that results from a set of flags
   * from QC routines. This is the most significant flag of the set
   * @param flags The flags
   * @return The most significant flag
   */
  public Flag getOverallFlag() {
    Flag result = Flag.GOOD;

    for (RoutineFlag flag : this) {
      if (flag.moreSignificantThan(result)) {
        result = flag;
      }
    }

    return result;
  }

  /**
   * Generate a JSON representation of this result
   * @return The JSON representation
   */
  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }
}
