package uk.ac.exeter.QuinCe.web.files;

import java.util.Set;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;

public class MissingRunType implements Comparable<MissingRunType> {

  private final FileDefinition fileDefinition;

  private RunTypeAssignment runType;

  protected MissingRunType(FileDefinition fileDefinition,
    RunTypeAssignment runType) {
    this.fileDefinition = fileDefinition;
    this.runType = runType;
  }

  public FileDefinition getFileDefinition() {
    return fileDefinition;
  }

  public RunTypeAssignment getRunType() {
    return runType;
  }

  protected void setRunType(RunTypeAssignment runType) {
    this.runType = runType;
  }

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
