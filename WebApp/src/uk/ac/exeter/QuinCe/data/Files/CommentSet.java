package uk.ac.exeter.QuinCe.data.Files;

import java.util.Iterator;
import java.util.TreeSet;

import uk.ac.exeter.QCRoutines.messages.Flag;

/**
 * Holds a set of QC/WOCE comments. Comments with the same text are unique, and counts
 * are kept of the number of each distinct comment.
 * @author Steve Jones
 *
 */
@Deprecated
public class CommentSet implements Iterable<CommentSetEntry> {

	/**
	 * The comment set entries
	 */
	private TreeSet<CommentSetEntry> entries;
	
	/**
	 * Create an empty comment set
	 */
	public CommentSet() {
		entries = new TreeSet<CommentSetEntry>();
	}
	
	/**
	 * Add a comment to the comment set.
	 * @param comment The comment string
	 * @param flag The flag for the comment
	 */
	public void addComment(String comment, Flag flag) {
		
		boolean commentAdded = false;
		
		for (CommentSetEntry entry : entries) {
			if (entry.matches(comment)) {
				entry.increment(flag);
				commentAdded = true;
				break;
			}
		}
		
		if (!commentAdded) {
			entries.add(new CommentSetEntry(comment, flag));
		}
	}
	
	/**
	 * Provides an iterator for the entries in this comment set
	 * @return The iterator
	 */
	@Override
	public Iterator<CommentSetEntry> iterator() {
		return entries.iterator();
	}
}
