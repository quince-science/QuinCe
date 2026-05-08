package uk.ac.exeter.QuinCe.data.Dataset.QC;

@SuppressWarnings("serial")
public class StubRoutineException extends RoutineException {

  protected StubRoutineException(Routine routine) {
    super("Routine is a stub. (" + routine.getName() + ")");
  }
}
