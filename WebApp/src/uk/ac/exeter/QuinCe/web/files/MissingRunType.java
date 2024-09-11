package uk.ac.exeter.QuinCe.web.files;

import java.util.Set;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;

public record MissingRunType(FileDefinition fileDefinition,
  RunTypeAssignment runType) implements Comparable<MissingRunType> {

  protected static boolean contains(Set<MissingRunType> list,
    FileDefinition fileDefinition, String runName) {
    return list.stream().anyMatch(r -> r.fileDefinition.equals(fileDefinition)
      && r.runType.getRunName().equals(runName));
  }

  @Override
  public int compareTo(MissingRunType o) {
    return runType.getRunName().compareTo(o.runType.getRunName());
  }
}
