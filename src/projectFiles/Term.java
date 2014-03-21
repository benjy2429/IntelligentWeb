package projectFiles;

import java.util.LinkedList;

/**
 * Defines a frequently used term
 * @author Luke Heavens & Ben Carr
 */
public class Term {
	public int rank;
	public String term;
	public int totalCount;
	public LinkedList<Pair<Long,Integer>> userCounts = new LinkedList<Pair<Long,Integer>>();
	
	public Term(int rank, String term, int totalCount, LinkedList<Pair<Long, Integer>> userCounts) {
		this.rank = rank;
		this.term = term;
		this.totalCount = totalCount;
		this.userCounts = userCounts;
	}
}
