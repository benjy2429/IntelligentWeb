package projectFiles;

/**
 * Define a tuple for storing two objects
 * @author Luke Heavens & Ben Carr
 *
 * @param <T> Left object
 * @param <U> Right object
 */
public class Pair<T,U> {
	public final T t;
	public final U u;

	public Pair(T t, U u) {         
		this.t= t;
		this.u= u;
	}
}
