package compling.util;

import java.io.Serializable;

/**
 * A generic-typed pair of objects.
 * 
 * @author Dan Klein
 */
public class Pair<F, S> implements Serializable {
	private static final long serialVersionUID = -1731875395071923035L;
	
	F first;
	S second;

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	public void setFirst(F f) {
		first = f;
	}

	public void setSecond(S s) {
		second = s;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Pair))
			return false;

		final Pair<?, ?> pair = (Pair<?, ?>) o;

		if (first != null ? !first.equals(pair.first) : pair.first != null)
			return false;
		if (second != null ? !second.equals(pair.second) : pair.second != null)
			return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (first != null ? first.hashCode() : 0);
		result = 29 * result + (second != null ? second.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "(" + getFirst() + ", " + getSecond() + ")";
	}

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}
	
	public static <S, F> Pair<F, S> make(F first, S second) {
		return new Pair<F, S>(first, second);
	}
}
