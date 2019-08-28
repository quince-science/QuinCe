package uk.ac.exeter.QuinCe.utils;

public class EmptyMissingParamException extends MissingParamException {

  public EmptyMissingParamException(String varName) {
    super(varName, "is empty");
  }

}
