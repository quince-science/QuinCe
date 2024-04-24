package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;

public record MissingRunType(FileDefinition fileDefinition,
  RunTypeAssignment runType) {
  protected static boolean contains(List<MissingRunType> list,
    FileDefinition fileDefinition, String runName) {
    return list.stream().anyMatch(r -> r.fileDefinition.equals(fileDefinition)
      && r.runType.getRunName().equals(runName));
  }
}
