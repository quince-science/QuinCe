package uk.ac.exeter.QuinCe.data.Calculation;

import org.primefaces.json.JSONArray;

import uk.ac.exeter.QCRoutines.messages.Flag;

/**
 * A single entry in a comment set
 * @author Steve Jones
 * @see CommentSet
 */
public class CommentSetEntry implements Comparable<CommentSetEntry> {

  /**
   * The comment
   */
  private String comment;

  /**
   * The flag for the comment
   */
  private Flag flag;

  /**
   * The number of instances of this comment
   */
  private int count;

  /**
   * Basic constructor
   * @param comment The comment string
   * @param flag The flag for the comment
   */
  protected CommentSetEntry(String comment, Flag flag) {
    this.comment = comment;
    this.flag = flag;
    this.count = 1;
  }

  /**
   * Add a new instance of this comment. If the flag
   * for the new comment is 'worse' than the existing flag,
   * it is updated.
   *
   * @param flag The flag for the new instance
   */
  protected void increment(Flag flag) {
    this.count++;
    if (flag.moreSignificantThan(this.flag)) {
      this.flag = flag;
    }
  }

  /**
   * Determines whether or not this entry matches the specified comment. The matching
   * is case insensitive.
   *
   * @param comment The comment to match
   * @return {@code true} if this entry matches the comment; {@code false} if it does not.
   */
  public boolean matches(String comment) {
    return comment.equalsIgnoreCase(this.comment);
  }

  /**
   * Get the comment string for this entry
   * @return The comment
   */
  public String getComment() {
    return comment;
  }

  /**
   * Get the number of instances of this comment that have been recorded
   * @return The instance count
   */
  public int getCount() {
    return count;
  }

  /**
   * Get the 'worst' flag assigned to instances of this comment
   * @return The flag
   */
  public Flag getFlag() {
    return flag;
  }

  /**
   * CommentSetEntry objects are ordered by their comment string
   */
  @Override
  public int compareTo(CommentSetEntry o) {
    return comment.compareTo(o.comment);
  }

  /**
   * Get the comment entry as a JSON array
   * @return The JSON array
   */
  public JSONArray toJson() {
    JSONArray json = new JSONArray();
    json.put(comment);
    json.put(flag.getFlagValue());
    json.put(count);
    return json;
  }

}
